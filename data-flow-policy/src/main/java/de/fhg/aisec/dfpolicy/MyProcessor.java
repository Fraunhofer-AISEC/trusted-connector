package de.fhg.aisec.dfpolicy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
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
						System.out.println("Error: Could not parse line " +line + " from rules file");
					} 
				}
			} catch (IOException e) {
				LOG.error("Caught IOException: " + e.getMessage());
				System.out.println("Caught IOException: " + e.getMessage());
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
		System.out.println("Start 'process' with endpoint ..." + exchange.getFromEndpoint().getEndpointUri());
		
		InstrumentationProcessor instrumentationprocessor;
		SendProcessor sendprocessor;
		String destination;
		String exchange_labels; 
		String body;
		//label the new message if needed
		exchange = LabelingProcess(exchange);
		// set labels from Property
		exchange_labels = (exchange.getProperty("labels") == null) ? "" : exchange.getProperty("labels").toString();

		System.out.println("process - after LabelingProcess");
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
		
		System.out.println("********************************************");
		boolean destinationAndRuleMatch = false;
		for (AllowRule rule : allowRules) {
			System.out.println("--------------------------------------------");
			System.out.println("Allow rule: " + rule.getLabel());
			//match for destination
			Pattern pattern = Pattern.compile(rule.getDestination());
			Matcher matcher = pattern.matcher(destination);
			
			System.out.println("pattern: " + rule.getDestination());
			System.out.println("matcher: " + destination);
			
			if (matcher.find()) {
				//the destination matches, now let's see if the label matches
				//Check if the message has the required label. If not, we stop 
				if (!checkIfLabelExists (rule.getLabel(), exchange_labels)) {
					System.out.println("Required label " + rule.getLabel() + " not found, message will be dropped...");
					return;
				} else {
					//TODO Aus if/for rausspringen, sobald einmal ein passendes Label gefunden wurde. 
					destinationAndRuleMatch = true;
					System.out.println("Destination '" + destination + "' and labels '" + exchange_labels + "' match.");	
				}
					
			} else {
				System.out.println("Destination does not match.");
				continue;
			}
			System.out.println("--------------------------------------------");
		}
		System.out.println("********************************************");

		if (destinationAndRuleMatch)
		{
			System.out.println("Message with labels  '" + exchange_labels +"' has all required labels for destination '" + destination + "', forwarding...");

			//store labels in message body
			body = exchange.getIn().getBody().toString();
			if (body.startsWith("Labels: ") && body.contains("\n\n")) {
				body = body.substring(body.indexOf("\n\n") + "\n\n".length(), body.length() - 1 );
			}
			exchange.getIn().setBody("Labels: " + exchange_labels + "\n\n" + body);
					
			target.process(exchange);	
		}
		
		System.out.println("Stop 'process' with endpoint ..." + exchange.getFromEndpoint().getEndpointUri());
    }
	
	//check if a label exists in a list of labels
	public boolean checkIfLabelExists(String label, String labels){
		System.out.println("Start 'checkIfLabelExists' ...");
		System.out.println("label: " + label);
		System.out.println("labels: " + labels);
		//There might be a , after and/or the label, so we add this to the regex
		Pattern pattern = Pattern.compile(",?" + label + ",?");
		Matcher matcher = pattern.matcher(labels);
		
		//if there are no requirements we have to fulfill, we return true
		if (labels == null) {
			System.out.println("labels = null");
			System.out.println("Stop 'checkIfLabelExists' ...");
				
			return true;
		}
		
		//if label is null, but labels isn't, we return false
		if (label == null) {
			System.out.println("label = null");
			System.out.println("Stop 'checkIfLabelExists' ...");
			return false;
		}
		
		//check if the label is contained in the requirements. If not, return false;
		if (matcher.find()) {
			System.out.println("matcher.find() = true");

			System.out.println("Stop 'checkIfLabelExists' ...");
			return true;
		} else {
			System.out.println("matcher.find() = false");
			System.out.println("Stop 'checkIfLabelExists' ...");
			return false;
		}
	}
	
	
	public Exchange LabelingProcess(Exchange exchange) {
		System.out.println("Start 'LabelingProcess' ...");
		
		Set<String> exchange_label_set = new HashSet<String>();
		String body = exchange.getIn().getBody().toString();
		String labels;
		
		if (exchange.getProperty("labels") != null ) {
			exchange_label_set.add(exchange.getProperty("labels").toString());
		}
		
		//Check if there are some labels in the body we have to use
		if (body.startsWith("Labels: ")) {
			labels = body.substring("Labels: ".length(), body.indexOf("\n"));
			System.out.println("Found labels in exchange body: " +labels);

			List<String> items = Arrays.asList(labels.split("\\s*,\\s*"));
			exchange_label_set.addAll(items);
		}
		
		//Check if we have to remove some labels based on the source
		exchange_label_set = removeLabelBasedOnAttributeToSet(exchange_label_set, exchange.getFromEndpoint().getEndpointUri());
		
		//Check if we have to remove some labels based on the name
		exchange_label_set = removeLabelBasedOnAttributeToSet(exchange_label_set, exchange.getIn().toString());
		
		//Check if we have a labeling rule for this source
		exchange_label_set = addLabelBasedOnAttributeToSet(exchange_label_set, exchange.getFromEndpoint().getEndpointUri());
		
		//Check if we have a labeling rule for this name
		exchange_label_set = addLabelBasedOnAttributeToSet(exchange_label_set, exchange.getIn().toString());
		
		exchange.setProperty("labels", joinStringSet(exchange_label_set, ","));
		System.out.println("Labels in message: " + joinStringSet(exchange_label_set, ","));

		System.out.println("Stop 'LabelingProcess' ...");
		
		return exchange;
	}
	
	public static String joinStringSet(Set<String> set, String seperator) {
		StringBuilder builder = new StringBuilder();
		for (String string : set) {
		  builder.append(string).append(seperator);
		}
		if (builder.length() >= seperator.length())
			builder.setLength(builder.length() - seperator.length());
		return builder.toString();
	}
	
	public Set<String> addLabelBasedOnAttributeToSet(Set<String> exchange_label_set, String attribute){
		System.out.println("Start 'addLabelBasedOnAttributeToSet' ...");
		
		for (LabelingRule rule : labelRules) {
			String rule_attribute = rule.getAttribute();
			String label = rule.getLabel();
			Pattern pattern = Pattern.compile(rule_attribute);
			Matcher matcher = pattern.matcher(attribute);
			
			if (matcher.find()) {
				exchange_label_set.add(label);
				System.out.println("Got a message with attribute '" + attribute + "' matching pattern '" + rule_attribute + "', assigning label '" + label + "'. All labels are now: '" + joinStringSet(exchange_label_set, ",") + "'");
				
			}
		}
		
		System.out.println("Stop 'addLabelBasedOnAttributeToSet' ...");
		
		return exchange_label_set;
	}

	public Set<String> removeLabelBasedOnAttributeToSet(Set<String> exchange_label_set, String attribute){
		System.out.println("Start 'removeLabelBasedOnAttributeToSet' ...");
		
		//No labels to remove here
		if (exchange_label_set.isEmpty()) {
			return exchange_label_set;
		}
		
		for (LabelingRule rule : removeLabelRules) {
			String rule_attribute = rule.getAttribute();
			String label = rule.getLabel();
			Pattern pattern = Pattern.compile(rule_attribute);
			Matcher matcher = pattern.matcher(attribute);
			
			if (matcher.find()) {
				exchange_label_set.remove(label);
				System.out.println("Got a message with attribute '" + attribute + "' matching pattern '" + rule_attribute + "', removed label '" + label + "'. All labels are now: '" + joinStringSet(exchange_label_set, ",") + "'");
			}
		}

		System.out.println("Stop 'removeLabelBasedOnAttributeToSet' ...");
		
		return exchange_label_set;
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
		System.out.println("Start ...");
		try {
			process(exchange);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Stop ...");
		return true;
	}
 
}
