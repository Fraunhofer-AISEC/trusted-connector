package de.fhg.aisec.dfpolicy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Policy decision point for deciding data flow control policies.
 *
 */



public class PDP {
	
    private static final Logger LOG = LoggerFactory.getLogger(PDP.class);
	private static PDP instance;
	private static final String RULE_FILE_NAME = "de.fhg.dfcontrol.rules.cfg";
	private List<LabelingRule> labelRules = new ArrayList<>();
	private List<AllowRule> allowRules = new ArrayList<>();
	private List<LabelingRule> removeLabelRules = new ArrayList<>();
	private RuleParser rp;
	String rulefile;

	/* Private C'tor, do not call */
	private PDP() {    	
		rp = new RuleParser();
		
		// Look for the rulefile in different directories
		try {
			rulefile = System.getProperty("karaf.base") + "/deploy/" + RULE_FILE_NAME;
			LOG.info("Loading rules from " + rulefile + "...");
			rp.loadRules(new File(rulefile));
		} catch (IOException e) {
			try {
				rulefile = System.getProperty("karaf.etc") + "/" +  RULE_FILE_NAME;
				LOG.info("Couldn't load rules, trying to load from " + rulefile);
				rp.loadRules(new File(rulefile));
			} catch (IOException ex) {
				LOG.error("Unable to load rulefile", ex);
			}
		}
		this.labelRules = rp.getLabelRules();
		this.allowRules = rp.getAllowRules();
		this.removeLabelRules = rp.getRemoveLabelRules();			
	}
	
	/**
	 * Returns a singleton instance of this policy decision point.
	 * 
	 * @return
	 */
	public static PDP getInstance() {
		if (instance == null) {
			instance = new PDP();
		}
		return instance;
	}
	
	/**
	 * Mainly needed for testing. Rules are otherwise loaded at first call to getInstance().
	 * 
	 * @param ruleFile
	 * @throws IOException 
	 */
	public void loadRules(File ruleFile) throws IOException {
		rp.loadRules(ruleFile);
	}
	
	public boolean decide(String source, String destination, Set<String> exchangeLabels) {
		
		// Transform labels according to rules
		labelingProcess(source, exchangeLabels);
		
		for (AllowRule rule : allowRules) {
			LOG.info("--------------------------------------------");
			LOG.info("Allow rule: " + rule.getLabel());

			//match for destination
			Matcher matcher = Pattern.compile(rule.getDestination()).matcher(destination);
			
			LOG.info("pattern: " + rule.getDestination());
			LOG.info("matcher: " + destination);
			
			if (matcher.find()) {
				//the destination matches, now let's see if the label matches
				//Check if the message has the required labels. If not, we stop 
				boolean labelsExist = checkIfLabelsMatch(rule.getLabel(), exchangeLabels);
				
				if (labelsExist) {
					LOG.info("Destination '" + destination + "' and label(s) '" + String.join(",", exchangeLabels) + "' match.");
					return true;
				}
					
			}
		}
		return false;
	}
	
	public Set<String> labelingProcess(String source, Set<String> labels) {
		LOG.info("Start 'LabelingProcess' ...");
		
		//Check if we have to remove some labels based on the source
		removeLabelBasedOnAttributeToSet(labels, source);
				
		//Check if we have a labeling rule for this source
		addLabelBasedOnAttributeToSet(labels, source);
				
		LOG.info("Labels in message: " + String.join(",", labels));

		LOG.info("Stop 'LabelingProcess' ...");
		
		return labels;
	}

	public Set<String> addLabelBasedOnAttributeToSet(Set<String> labelSet, String attribute){
		LOG.info("Start 'addLabelBasedOnAttributeToSet' ...");
		LOG.info("labelRules: " + joinStringSetLabelRule(labelRules, ","));
		
		for (LabelingRule rule : labelRules) {
			String ruleAttribute = rule.getAttribute();
			Set<String> label = rule.getLabel();
			Pattern pattern = Pattern.compile(ruleAttribute);
			Matcher matcher = pattern.matcher(attribute);
			
			if (matcher.find()) {
				labelSet.addAll(label);
				LOG.info("Got a message with attribute '" + attribute + "' matching pattern '" + ruleAttribute + "', assigning label '" + label + "'. All labels are now: '" + String.join(",", labelSet) + "'");				
			}
		}
		
		LOG.info("Stop 'addLabelBasedOnAttributeToSet' ...");
		
		return labelSet;
	}
	
	// Join LabelingRule-Strings from a Set with a separator
    private String joinStringSetLabelRule(List<LabelingRule> labelRules2, String separator) {
    	StringBuilder builder = new StringBuilder();

		for (LabelingRule string : labelRules2) {
			  builder.append("(").append(string.getLabel().toString()).append(",").append(string.getAttribute()).append(")").append(separator);
			}

		if (builder.length() >= separator.length())
			builder.setLength(builder.length() - separator.length());
		
		return builder.toString();
    }

	private Set<String> removeLabelBasedOnAttributeToSet(Set<String> labelSet, String attribute){
		LOG.info("Start 'removeLabelBasedOnAttributeToSet' ...");
		
		//No labels to remove here
		if (labelSet.isEmpty()) {
			return labelSet;
		}

		LOG.info("removeLabelRules: " + joinStringSetLabelRule(removeLabelRules, ","));
		
		for (LabelingRule rule : removeLabelRules) {
			String ruleAttribute = rule.getAttribute();
			Set<String> label = rule.getLabel();
			Pattern pattern = Pattern.compile(ruleAttribute);
			Matcher matcher = pattern.matcher(attribute);
			
			if (matcher.find()) {
				labelSet.removeAll(label);
				LOG.info("Got a message with attribute '" + attribute + "' matching pattern '" + ruleAttribute + "', removed label '" + label + "'. All labels are now: '" + String.join(",", labelSet) + "'");
			}
		}

		LOG.info("Stop 'removeLabelBasedOnAttributeToSet' ...");
		
		return labelSet;
	}
	
	// Check if a set of labels exist in another set of labels
	private boolean checkIfLabelsMatch(Set<String> labelsToFind, Set<String> exchangeLabels) {		
		LOG.info("Start 'checkIfLabelsExists' ...");
		
		if (exchangeLabels==null || exchangeLabels.isEmpty()) {
			return false;
		}
		
		LOG.info("label_set: " + labelsToFind);
		LOG.info("check_labels: " + String.join(",",exchangeLabels));
		
		return exchangeLabels.parallelStream().anyMatch(exLabel -> labelsToFind.contains(exLabel));
		
	}

}
