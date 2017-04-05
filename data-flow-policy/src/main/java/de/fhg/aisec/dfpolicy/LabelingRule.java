package de.fhg.aisec.dfpolicy;

import java.util.List;

public class LabelingRule extends Rule {
	
	private String attribute;
	
	public LabelingRule(String attribute, List<String> labels) {
		super.setLabel(labels);
		this.setAttribute(attribute);
	}
	
	public String getAttribute() {
		return attribute;
	}
	
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	// Method can be defined arbitrary, also the values 17 and 31 
	@Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + attribute.hashCode();
        result = 31 * result + getLabel().hashCode();
        return result;
    }
	
	@Override
    public boolean equals(Object o) {
        if (o == this) 
        	return true;
        
        if (!(o instanceof LabelingRule))
            return false;

        LabelingRule labelRule = (LabelingRule) o;

        return labelRule.attribute.equals(attribute) &&
        		this.getLabel().equals(labelRule.getLabel());
    }
}
