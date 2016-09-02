package de.fhg.aisec.dfpolicy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.management.InstrumentationProcessor;
import org.apache.camel.processor.SendProcessor;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyProcessor implements AsyncProcessor {
	
    private static final Logger LOG = LoggerFactory.getLogger(MyProcessor.class);  
    private Processor target;
    private ArrayList<LabelRule> label_rules;
    private ArrayList<AllowRule> allow_rules;
    
    
    public MyProcessor(Processor target) {
    	this.target = target;
    	label_rules = new ArrayList<LabelRule>();
    	allow_rules = new ArrayList<AllowRule>();
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
					
					label_rules.add(new LabelRule(uri,label));
						
				// Check for an ALLOW-rule
				} else if (check_rule_syntax(line, ALLOW_KEYWORD1, ALLOW_KEYWORD2)) {

					// source = the string between the first and the second keyword 
					label = line.substring(line.indexOf(ALLOW_KEYWORD1) + ALLOW_KEYWORD1.length(), line.indexOf(ALLOW_KEYWORD2));
					
					// label = the string after the second keyword
					uri = line.substring(line.indexOf(ALLOW_KEYWORD2) + ALLOW_KEYWORD2.length());
					
					allow_rules.add(new AllowRule(uri, label));
					
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
		String destination = null;
		String labels_value; 
		String label;
		//label the new message if needed
		exchange = LabelingProcess(exchange);

		if (exchange.getProperty("labels") != null ) {
			labels_value = exchange.getProperty("labels").toString();
		} else {
			labels_value = "";
		}
		
		//figuring out where the message should go to
		if (target instanceof InstrumentationProcessor) {
			instrumentationprocessor = (InstrumentationProcessor) target;
			if (instrumentationprocessor.getProcessor() instanceof SendProcessor) {
				sendprocessor = (SendProcessor) instrumentationprocessor.getProcessor();
				destination = sendprocessor.getEndpoint().getEndpointUri();
			} else {
				LOG.error("target is not an instance of SendProcessor");
				return;
			}
		} else {
			LOG.error("target is not an instance of InstrumentactionProcessor");
			return;
		}
		
		for (AllowRule allow_rule : allow_rules) {
					
			if (allow_rule.getDestination().equals(destination)){
				
				label = allow_rule.getLabel();
				
				//is the label contained in the labels-property, either as 'label', 'label,' or ',label'?
				if (check_if_label_exists(label, labels_value)) {
					
					System.out.println("Found matching rule for destination " + destination +" with labels '" + labels_value + "': " + allow_rule.toString());
					target.process(exchange);
				}
			}  
		}
		
    }
	
	public boolean check_if_label_exists(String label, String labels_value){
		if (labels_value != "" 
				&& labels_value.equals(label) 
				|| labels_value.contains(label + ",") 
				|| labels_value.contains("," + label)) {
			return true;
		} else {
			return false;
		}
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
		
		//Check if there is a label rule for our source. label_rules.indexOf() will not return multiple occurrences, so we use a loop
		for (LabelRule rule : label_rules) {
			
			//If we find a matching rule, we check if the labels-property already exists. If it does, we append our label, if not, we create the labels-property
			label = rule.getLabel();
			
			if (rule.getSource().equals(from) && !(check_if_label_exists(label, labels_value))) {

				if (labels_value == "") {
					labels_value = label;
				} else {
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
