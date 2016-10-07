package de.fhg.aisec.dfpolicy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.management.InstrumentationProcessor;
import org.apache.camel.processor.LogProcessor;
import org.apache.camel.processor.SendProcessor;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyProcessor implements AsyncProcessor {
	
    private static final Logger LOG = LoggerFactory.getLogger(MyProcessor.class);  
    private Processor target;
    private static ArrayList<LabelingRule> labelRules = null;
    private static ArrayList<LabelingRule> removeLabelRules = null;
    private static ArrayList<AllowRule> allowRules = null;
    
    
    public MyProcessor(Processor target) {
    	this.target = target;
    	//If we didn't load the rules yet, do it now
    	if (labelRules == null || allowRules == null) {
    		labelRules = new ArrayList<LabelingRule>();
    		removeLabelRules = new ArrayList<LabelingRule>();
    		allowRules = new ArrayList<AllowRule>();
    		loadRules("deploy/rules");
    	}
    }
    
    
    private void loadRules(String rulefile) {
    	BufferedReader bufferedreader;
    	String line, attribute, label;
		try {
	    	bufferedreader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(rulefile))));
			while ((line = bufferedreader.readLine()
					// remove empty spaces
					.replaceAll(" ", "")
					// remove one-line-comments starting with //
					.replaceAll("//.*?\n","\n")
					// remove one-line-comments starting with #
					.replaceAll("#.*?\n","\n")
					) != null)   {
					
					//Check if it is a LABEL-rule that contains LABEL and AS, and both only once
					if (checkRuleSyntax(line, Constants.LABEL, Constants.AS)) {
									
						// source = the string between the first and the second keyword 
						attribute = line.substring(line.indexOf(Constants.LABEL) + Constants.LABEL.length(), line.indexOf(Constants.AS));
						
						// label = the string after the second keyword
						label = line.substring(line.indexOf(Constants.AS) + Constants.AS.length());
						
						labelRules.add(new LabelingRule(attribute, label));
					} 
					// Check for an ALLOW-rule
					else if (checkRuleSyntax(line, Constants.REMOVELABEL, Constants.FROM)) {
						
						// source = the string between the first and the second keyword 
						label = line.substring(line.indexOf(Constants.REMOVELABEL) + Constants.REMOVELABEL.length(), line.indexOf(Constants.FROM));
			
						// label = the string after the second keyword
						attribute = line.substring(line.indexOf(Constants.FROM) + Constants.FROM.length());
			
						removeLabelRules.add(new LabelingRule(attribute, label));
					}
					// Check for an ALLOW-rule
					else if (checkRuleSyntax(line, Constants.ALLOW, Constants.TO)) {
	
						// source = the string between the first and the second keyword 
						label = line.substring(line.indexOf(Constants.ALLOW) + Constants.ALLOW.length(), line.indexOf(Constants.TO));
						
						// label = the string after the second keyword
						attribute = line.substring(line.indexOf(Constants.TO) + Constants.TO.length());
						
						allowRules.add(new AllowRule(label, attribute));
					} 
					// skip if line is empty (or has just comments)
					else if (line.isEmpty()) {
						
					} 
					// otherwise log error
					else {
						LOG.error("Error: Could not parse line " +line + " from rules file");
					} 
				}
			} catch (IOException e) {
				LOG.error("Caught IOException: " + e.getMessage());
				e.printStackTrace();
			}
    		LOG.info("Loaded LABEL rules: " + labelRules.toString());
    		LOG.info("Loaded REMOVELABEL rules: " + removeLabelRules.toString());
    		LOG.info("Loaded ALLOW rules: " + allowRules.toString()); 		
    }
    
    
    // Checks for a line in the rule-file that each keyword exists only once, keyword1 before keyword 2, etc... 
    public boolean checkRuleSyntax(String line, String keyword1, String keyword2){
    	//keyword1 in the beginning?
    	if (line.startsWith(keyword1)
    			//keyword 2 exists?
				&& line.contains(keyword2)
				//no second keyword1?
				&& line.lastIndexOf(keyword1) == 0
				//no second keyword2?
				&& line.indexOf(keyword2) == line.lastIndexOf(keyword2)) {
    				return true;
				} else {
					return false;
				}
    }

	public void process(Exchange exchange) throws Exception {
		
		InstrumentationProcessor instrumentationprocessor;
		SendProcessor sendprocessor;
		String destination;
		String exchange_labels; 
		String body;
		//label the new message if needed
		exchange = LabelingProcess(exchange);
		// set labels from Property
		exchange_labels = (exchange.getProperty("labels") == null) ? "" : exchange.getProperty("labels").toString();

		
		//figuring out where the message should go to
		if (target instanceof InstrumentationProcessor) {
			instrumentationprocessor = (InstrumentationProcessor) target;
			if (instrumentationprocessor.getProcessor() instanceof SendProcessor) {
				sendprocessor = (SendProcessor) instrumentationprocessor.getProcessor();
				destination = sendprocessor.getEndpoint().getEndpointUri();
				
			//if it's also no LogProcessor, throw an Error
			} else if (instrumentationprocessor.getProcessor() instanceof LogProcessor) {
				//nothing to do yet, maybe some logging later 
				return;
			} else {
				LOG.error("target is neither an instance of Send- nor Log-Processor: " + target.toString());
				return;
			}
		} else {
			LOG.error("target is not an instance of InstrumentactionProcessor");
			return;
		}
		
		for (AllowRule rule : allowRules) {
			
			//match for destination
			Pattern pattern = Pattern.compile(rule.getDestination());
			Matcher matcher = pattern.matcher(destination);
			
			//the destination matches, now let's see if the label matches
			if (matcher.find()) {
				
				//Check if the message has the required label. If not, we stop 
				if (!checkIfLabelExists (rule.getLabel(), exchange_labels)) {
					System.out.println("Required label " + rule.getLabel() + " not found, message will be dropped...");
					return;
				}

			}
		}
		
		System.out.println("Message with labels  '" + exchange_labels +"' has all required labels for destination '" + destination + "', forwarding...");

		
		//store labels in message body
		body = exchange.getIn().getBody().toString();
		if (body.startsWith("Labels: ") && body.contains("\n\n")) {
			body = body.substring(body.indexOf("\n\n") + "\n\n".length(), body.length() - 1 );
		}
		exchange.getIn().setBody("Labels: " + exchange_labels + "\n\n" + body);
				
		target.process(exchange);
    }
	
	//check if a label exists in a list of labels
	public boolean checkIfLabelExists(String label, String labels){
		
		//There might be a , after and/or the label, so we add this to the regex
		Pattern pattern = Pattern.compile(",?" + label + ",?");
		Matcher matcher = pattern.matcher(labels);
		
		//if there are no requirements we have to fulfill, we return true
		if (labels == null) {
			return true;
		}
		
		//if label is null, but labels isn't, we return false
		if (label == null) {
			return false;
		}
		
		//check if the label is contained in the requirements. If not, return false;		
		if (matcher.find()) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public Exchange LabelingProcess(Exchange exchange) {
		String exchange_labels; 
		String body = exchange.getIn().getBody().toString();
		String labels;
		
		if (exchange.getProperty("labels") != null ) {
			exchange_labels = exchange.getProperty("labels").toString();
		} else {
			exchange_labels = "";
		}
		
		//Check if there are some labels in the body we have to use
		if (body.startsWith("Labels: ")) {
			labels = body.substring("Labels: ".length(), body.indexOf("\n"));
			System.out.println("Found labels in exchange body: " +labels);
			if (exchange_labels == "") {
				exchange_labels = labels;
			} else {
				exchange_labels = exchange_labels + "," + labels;
			}
				
		}
		
		//Check if we have to remove some labels based on the source
		exchange_labels = removeLabelBasedOnAttribute(exchange_labels, exchange.getFromEndpoint().getEndpointUri());
		
		//Check if we have to remove some labels based on the name
		exchange_labels = removeLabelBasedOnAttribute(exchange_labels, exchange.getIn().toString());
		
		//Check if we have a labeling rule for this source
		exchange_labels = addLabelBasedOnAttribute(exchange_labels, exchange.getFromEndpoint().getEndpointUri());
		
		//Check if we have a labeling rule for this name
		exchange_labels = addLabelBasedOnAttribute(exchange_labels, exchange.getIn().toString());
		
		exchange.setProperty("labels", exchange_labels);
		
		return exchange;
	}
	
	
	public String addLabelBasedOnAttribute(String exchange_labels, String attribute){
		
		for (LabelingRule rule : labelRules) {
			String rule_attribute = rule.getAttribute();
			String label = rule.getLabel();
			Pattern pattern = Pattern.compile(rule_attribute);
			Matcher matcher = pattern.matcher(attribute);
			
			if (matcher.find()) {
			
				if (exchange_labels == "") {
	
					exchange_labels = label;
					System.out.println("Got a message with attribute '" + attribute + "' matching pattern '" + rule_attribute + "', assigning label '" + label + "'. All labels are now: '" + exchange_labels + "'");
					
				} else {
				
					// Check if the label already exists
					System.out.println("Checking if the label already exists.... ");
					pattern = Pattern.compile(",?" + label + ",?");
					matcher = pattern.matcher(exchange_labels);
					if (!matcher.find()) {	
						exchange_labels = exchange_labels + "," + label;
						System.out.println("Got a message with attribute '" + attribute + "' matching pattern '" + rule_attribute + "', assigning label '" + label + "'. All labels are now: '" + exchange_labels + "'");
					} 
				}
			}
		}
		
		return exchange_labels;
	}
	
	public String removeLabelBasedOnAttribute(String exchange_labels, String attribute){
		
		//No labels to remove here
		if (exchange_labels == "") {
			return exchange_labels;
		}
		
		for (LabelingRule rule : removeLabelRules) {
			String rule_attribute = rule.getAttribute();
			String label = rule.getLabel();
			Pattern pattern = Pattern.compile(rule_attribute);
			Matcher matcher = pattern.matcher(attribute);
			
			if (matcher.find()) {
				
				// We do have a matching REMOVELABEL rule, let's remove the label
				if (exchange_labels.equals(label)) {
					exchange_labels = "";
				} else if (exchange_labels.contains("," + label + ",")) {
					exchange_labels = exchange_labels.replaceAll("," + label + ",", "");
				} else if (exchange_labels.contains("," + label)) {
					exchange_labels = exchange_labels.replaceAll("," + label, "");
				} else if (exchange_labels.contains(label + ",")) {
					exchange_labels = exchange_labels.replaceAll(label + ",", "");
				}
				System.out.println("Got a message with attribute '" + attribute + "' matching pattern '" + rule_attribute + "', removed label '" + label + "'. All labels are now: '" + exchange_labels + "'");
			
			}
		}
		
		return exchange_labels;
	}

    @Override
    public String toString() {
      return "MyProcessor[" + 
    		  "allow:" + MyProcessor.allowRules.toString() +
    		  "label:" + MyProcessor.labelRules.toString() +
    		  "remove:" + MyProcessor.removeLabelRules.toString() +
    		  "]";
    }

	@Override
	public boolean process(Exchange exchange, AsyncCallback ac) {
		try {
			process(exchange);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
 
}
