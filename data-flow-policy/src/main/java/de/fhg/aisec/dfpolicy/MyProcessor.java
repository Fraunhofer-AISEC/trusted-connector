package de.fhg.aisec.dfpolicy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
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
    private static HashMap<String, String> label_rules = null;
    private static HashMap<String, String> allow_rules = null;
    
    
    public MyProcessor(Processor target) {
    	this.target = target;
    	//If we didn't load the rules yet, do it now
    	if (label_rules == null || allow_rules == null) {
    		label_rules = new HashMap<String, String>();
    		allow_rules = new HashMap<String, String>();
    		loadRules("deploy/rules");
    	}
    }
    
    private void loadRules(String rulefile) {
    	FileInputStream fileinputstream;
    	DataInputStream datainputstream;
    	BufferedReader bufferedreader;
    	final String LABEL_KEYWORD1 = "LABEL";
    	final String LABEL_KEYWORD2 = "AS";
    	final String ALLOW_KEYWORD1 = "ALLOW";
    	final String ALLOW_KEYWORD2 = "TO";
    	String line;
    	String uri;
    	String label;
    	String existing_label;
    	
		try {
			fileinputstream =  new FileInputStream(rulefile);
			datainputstream = new DataInputStream(fileinputstream);
	    	bufferedreader = new BufferedReader(new InputStreamReader(datainputstream));
			
    	
			while ((line = bufferedreader.readLine()) != null)   {
				
				//Remove unneeded spaces
				line = line.replaceAll(" ", "");
				
				//Check if it is a LABEL-rule that contains LABEL and AS, and both only once
				if (check_rule_syntax(line, LABEL_KEYWORD1, LABEL_KEYWORD2)) {
								
					// source = the string between the first and the second keyword 
					uri = line.substring(line.indexOf(LABEL_KEYWORD1) + LABEL_KEYWORD1.length(), line.indexOf(LABEL_KEYWORD2));
					
					// label = the string after the second keyword
					label = line.substring(line.indexOf(LABEL_KEYWORD2) + LABEL_KEYWORD2.length());
					
					existing_label = label_rules.get(uri);
					if (existing_label == null) {
						label_rules.put(uri, label);
					} else {
						label_rules.put(uri, existing_label + "," + label);
					}
						
				// Check for an ALLOW-rule
				} else if (check_rule_syntax(line, ALLOW_KEYWORD1, ALLOW_KEYWORD2)) {

					// source = the string between the first and the second keyword 
					label = line.substring(line.indexOf(ALLOW_KEYWORD1) + ALLOW_KEYWORD1.length(), line.indexOf(ALLOW_KEYWORD2));
					
					// label = the string after the second keyword
					uri = line.substring(line.indexOf(ALLOW_KEYWORD2) + ALLOW_KEYWORD2.length());
					
					existing_label = allow_rules.get(uri);
					if (existing_label == null) {
						allow_rules.put(uri, label);
					} else {
						allow_rules.put(uri, existing_label + "," + label);
					}
					
				// If it's also no comment, throw an error	
				} else if (!line.startsWith("#")) {
					LOG.error("Error: Could not parse line " +line + " from rules file");
				} 
			}
			datainputstream.close();
			} catch (IOException e) {
				LOG.error("Caught IOException: " + e.getMessage());
				e.printStackTrace();
			}
    		
    		LOG.info("Loaded LABEL rules: " + label_rules.toString());
    		LOG.info("Loaded ALLOW rules: " + allow_rules.toString());
    		
    }
    
    // Checks for a line in the rule-file that each keyword exists only once, keyword1 before keyword 2, etc... 
    public boolean check_rule_syntax(String line, String keyword1, String keyword2){
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
		String[] rule_labels;
		//label the new message if needed
		exchange = LabelingProcess(exchange);

		if (exchange.getProperty("labels") != null ) {
			exchange_labels = exchange.getProperty("labels").toString();
		} else {
			exchange_labels = "";
		}
		
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
			
		rule_labels = allow_rules.get(destination).split(",");
		
		if (rule_labels == null) {
			System.out.println("No rules found for destination: " + destination + ", message will be dropped...");
			return;
		}
		
		//Check if the message has _ALL_ the required labels. If we miss one, stop 
		for (String rule : rule_labels) {
			if (!check_if_label_exists (rule, exchange_labels)) {
				System.out.println("Required label " + rule + " not found, message will be dropped...");
				return;
			}
		}
		System.out.println("Message with labels  '" + exchange_labels +"' has all required labels ('" + allow_rules.get(destination) + "') for destination '" + destination + "', forwarding...");
		target.process(exchange);
    }
	
	//check if a label exists in a list of labels
	public boolean check_if_label_exists(String label, String labels){
		
		//if there are no requirements we have to fulfill, we return true
		if (labels == null) {
			return true;
		}
		
		//if label is null, but labels isn't, we return false
		if (label == null) {
			return false;
		}
		
		//check for each label if it's contained in the requirements. If not, return false;
		if (!labels.equals(label)   
				&& !labels.contains(label + ",") 
				&& !labels.contains("," + label)) {
			return false;
		}
		return true;
	}
	
	public Exchange LabelingProcess(Exchange exchange) {
		String exchange_labels; 
		
		if (exchange.getProperty("labels") != null ) {
			exchange_labels = exchange.getProperty("labels").toString();
		} else {
			exchange_labels = "";
		}
		
		//Check if we have a labeling rule for this source
		exchange_labels = get_label_based_on_attribute(exchange_labels, exchange.getFromEndpoint().getEndpointUri());
		
		//Check if we have a labeling rule for this filename
		exchange_labels = get_label_based_on_attribute(exchange_labels, exchange.getIn().toString());
		
		exchange.setProperty("labels", exchange_labels);
		
		return exchange;
	}
	
	public String get_label_based_on_attribute(String exchange_labels, String attribute){
		
		String[] rule_labels;
		
		//if there are no rules for the attribute, we can stop here
		if (label_rules.get(attribute) == null) {
			return exchange_labels;
		}
		
		rule_labels = label_rules.get(attribute).split(",");
		
		if (rule_labels != null) {
			
			for (String label : rule_labels) {
				//If the label already exists, we don't have to do anything, else, we append it
				if (!check_if_label_exists(label, exchange_labels)) {
					System.out.println("Got a message with attribute '" + attribute + "', will label it with '" + label + "'");
					if (exchange_labels == "") {
						exchange_labels = label;
					} else {
						exchange_labels = exchange_labels + "," + label;
					}
				}
			}
		}
		
		return exchange_labels;
	}

    @Override
    public String toString() {
      return "MyProcessor[" + "]";
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
