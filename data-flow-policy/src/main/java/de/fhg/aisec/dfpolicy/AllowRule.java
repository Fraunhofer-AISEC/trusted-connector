package de.fhg.aisec.dfpolicy;

import java.util.List;

public class AllowRule extends Rule {
	private String destination;

	public AllowRule(List<String> labelSet, String destination) {
		this.setLabel(labelSet);
		this.setDestination(destination);
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
	
	// Method can be defined arbitrary, also the values 17 and 31 
	@Override
    public int hashCode() {
		int result = 17;
        result = 31 * result + destination.hashCode();
        result = 31 * result + getLabel().hashCode();
        return result;
    }
	
	@Override
    public boolean equals(Object o) {
        if (o == this) 
        	return true;
        
        if (!(o instanceof AllowRule))
            return false;

        AllowRule labelRule = (AllowRule) o;

        return labelRule.destination.equals(destination) &&	this.getLabel().equals(labelRule.getLabel());
    }
}
