package de.fhg.aisec.dfpolicy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a rule file and returns allow, remove, and labeling rules.
 *
 */
public class RuleParser {
    private static final Logger LOG = LoggerFactory.getLogger(RuleParser.class);
	private List<LabelingRule> labelRules = new ArrayList<>();
	private List<LabelingRule> removeLabelRules = new ArrayList<>();
	private List<AllowRule> allowRules = new ArrayList<>();
	
	/**
	 * Returns rules for adding labels.
	 * 
	 * @return
	 */
	public List<LabelingRule> getLabelRules() {
		return labelRules;
	}

	/**
	 * Returns rules for removing labels.
	 * 
	 * @return
	 */
	public List<LabelingRule> getRemoveLabelRules() {
		return removeLabelRules;
	}

	/**
	 * Returns rules allowing forwarding of a message depending on its labels.
	 * @return
	 */
	public List<AllowRule> getAllowRules() {
		return allowRules;
	}

	/**
	 * Loads policies from an external file. Call this method before calling the getters.
	 * 
	 * @param rulefile
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
    public void loadRules(File rulefile) throws IOException {
    	LOG.debug("Start 'loadRules'...");
    	List<String> labelSet = new ArrayList<>();
    	String line;
    	
    	/* First look for a config file in karaf/etc */
 
		try (	FileInputStream fis = new FileInputStream(rulefile);
    			BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new DataInputStream(fis)))) {
    		
    		while ((line = bufferedreader.readLine())!= null)   {
				// remove empty spaces
				line = line.replaceAll(" ", "")
				// remove one-line-comments starting with //
				.replaceAll("//.*?\n","\n")
				// remove one-line-comments starting with #
				.replaceAll("#.*?\n","\n");
	
    			String attribute;
    			String label;
    			
				//Check if it is a LABEL-rule that contains LABEL and AS, and both only once
				if (checkRuleSyntax(line, Constants.LABEL, Constants.AS)) {
					labelSet = new ArrayList<>();
					// source = the string between the first and the second keyword 
					attribute = line.substring(line.indexOf(Constants.LABEL) + Constants.LABEL.length(), line.indexOf(Constants.AS));
	
					// label = the string after the second keyword
					label = line.substring(line.indexOf(Constants.AS) + Constants.AS.length());
					labelSet = Arrays.asList(label.split(","));
					labelRules.add(new LabelingRule(attribute, labelSet));
					
					LOG.info("labelRules - label_set: " + String.join("," , labelSet));
					LOG.info("labelRules - all: " + String.join(",", labelRules.stream().map(r -> r.toString()).collect(Collectors.toList())));
					LOG.info("labelRules.size() : " + labelRules.size());
				} 
				// Check for an REMOVELABEL-rule
				else if (checkRuleSyntax(line, Constants.REMOVELABEL, Constants.FROM)) {
					labelSet = new ArrayList<>();
					// label = the string between the first and the second keyword 
					label = line.substring(line.indexOf(Constants.REMOVELABEL) + Constants.REMOVELABEL.length(), line.indexOf(Constants.FROM));
		
					// source = the string after the second keyword
					attribute = line.substring(line.indexOf(Constants.FROM) + Constants.FROM.length());
					labelSet = Arrays.asList(label.split(","));
					removeLabelRules.add(new LabelingRule(attribute, labelSet));
					
					LOG.info("removeLabelRules - label_set: " + String.join(",", labelSet));
					LOG.info("removeLabelRules - all: " + String.join(",", removeLabelRules.stream().map(r -> r.toString()).collect(Collectors.toList())));
					LOG.info("removeLabelRules.size() : " + removeLabelRules.size());
				}
				// Check for an ALLOW-rule
				else if (checkRuleSyntax(line, Constants.ALLOW, Constants.TO)) {
					labelSet = new ArrayList<>();
					// label = the string between the first and the second keyword 
					label = line.substring(line.indexOf(Constants.ALLOW) + Constants.ALLOW.length(), line.indexOf(Constants.TO));
					
					// destination = the string after the second keyword
					attribute = line.substring(line.indexOf(Constants.TO) + Constants.TO.length());
					labelSet = Arrays.asList(label.split(","));
					allowRules.add(new AllowRule(labelSet, attribute));
					
					LOG.info("allowRules - label_set: " + String.join(",", labelSet));
					LOG.info("allowRules - all: " + String.join(",", allowRules.stream().map(r -> r.toString()).collect(Collectors.toList())));
					LOG.info("allowLabelRules.size() : " + allowRules.size());
				} 
				// skip if line is empty (or has just comments)
				else if (!line.isEmpty()) {
					// otherwise log error
					LOG.error("Error: Could not parse line " +line + " from rules file");
					LOG.info("Error: Could not parse line " +line + " from rules file");
				} 
				LOG.info("----------------------------------");
			}
    	}
    	
		LOG.info("Loaded LABEL rules: " + labelRules.toString());
		LOG.info("Loaded REMOVELABEL rules: " + removeLabelRules.toString());
		LOG.info("Loaded ALLOW rules: " + allowRules.toString()); 		
    	LOG.info("Stop'loadRules'...");
    }
    
    // Checks for a line in the rule-file that each keyword exists only once, keyword1 before keyword 2, etc... 
    private boolean checkRuleSyntax(String line, String keyword1, String keyword2) {
		//keyword1 in the beginning?
    	return line.startsWith(keyword1)
    			//keyword 2 exists?
				&& line.contains(keyword2)
				//no second keyword1?
				&& line.lastIndexOf(keyword1) == 0
				//no second keyword2?
				&& line.indexOf(keyword2) == line.lastIndexOf(keyword2);
    }

}