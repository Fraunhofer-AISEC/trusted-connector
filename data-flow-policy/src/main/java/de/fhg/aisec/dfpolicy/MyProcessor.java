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
    //private ArrayList<LabelRule> label_rules;
    //private ArrayList<AllowRule> allow_rules;
    private HashMap<String, String> label_rules;
    private HashMap<String, String> allow_rules;
    
    
    public MyProcessor(Processor target) {
    	this.target = target;
    	//label_rules = new ArrayList<LabelRule>();
    	//allow_rules = new ArrayList<AllowRule>();
    	label_rules = new HashMap<String, String>();
    	allow_rules = new HashMap<String, String>();
    	loadRules("deploy/rules");
    }
    
    private void loadRules(String rulefile) {
    	FileInputStream fileinputstream;
    	// Get the object of DataInputStream
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
					
					//label_rules.add(new LabelRule(uri,label));
						
				// Check for an ALLOW-rule
				} else if (check_rule_syntax(line, ALLOW_KEYWORD1, ALLOW_KEYWORD2)) {

					// source = the string between the first and the second keyword 
					label = line.substring(line.indexOf(ALLOW_KEYWORD1) + ALLOW_KEYWORD1.length(), line.indexOf(ALLOW_KEYWORD2));
					
					// label = the string after the second keyword
					uri = line.substring(line.indexOf(ALLOW_KEYWORD2) + ALLOW_KEYWORD2.length());
					
					//allow_rules.add(new AllowRule(uri, label));
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
    
    // Checks for a line in the rulefile that each keyword exists only once, keyword1 before keyword 2, etc... 
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
		String rule_labels;
		String[] rules;
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
			
		rule_labels = allow_rules.get(destination);
		
		if (rule_labels == null) {
			System.out.println("No rules found for destination: " + destination + ", message will be dropped...");
			return;
		}
		rules = rule_labels.split(",");
		
		for (String rule : rules) {
			if (check_if_labels_exists (rule, exchange_labels)) {
				System.out.println("Found matching rule for destination " + destination +" with labels '" + exchange_labels + "', forwarding...");
				target.process(exchange);
				return;
			}
		}
		System.out.println("No matching rules found for labels: " + exchange_labels + ", message will be dropped...");
    }
	
	//check if all labels from labels1 exist in labels2, both might be a list of comma-separeted labels
	public boolean check_if_labels_exists(String labels1, String labels2){
		String[] labels = labels1.split(",");
		
		if (labels == null) {
			return false;
		}
		
		//if there are no requirements we have to fulfill, we return true
		if (labels2 == null) {
			return true;
		}
		
		//check for each label if it's contained in the requirements. If not, return false;
		for (String label : labels) {
			if (!labels2.equals(label)   
					&& !labels2.contains(label + ",") 
					&& !labels2.contains("," + label)) {
				return false;
			}
		}
		return true;
	}
	
	public Exchange LabelingProcess(Exchange exchange) {
		String from = exchange.getFromEndpoint().getEndpointUri();
		String labels_value; 
		String label;
		
		if (exchange.getProperty("labels") != null ) {
			labels_value = exchange.getProperty("labels").toString();
		} else {
			labels_value = "";
		}
		
		System.out.println("Received a message from " + from);
		
		//Check if we have a labeling rule for this uri
		label = label_rules.get(from);
		if (label != null) {
			
			//If all labels already exists, we don't have to do anything, else, we append it
			if (!check_if_labels_exists(label, labels_value)) {
				if (labels_value == "") {
					labels_value = label;
				} else {
					//TODO: what if some labels already exists, but some don't?
					labels_value = labels_value + "," + label;
				}
				System.out.println("Got a message from " + from + ", will label it with '" + label + "'");
				exchange.setProperty("labels", labels_value);
			}
		}
		
		return exchange;
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
