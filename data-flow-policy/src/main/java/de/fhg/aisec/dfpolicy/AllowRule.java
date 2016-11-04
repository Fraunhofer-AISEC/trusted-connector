package de.fhg.aisec.dfpolicy;

import java.util.Set;

public class AllowRule extends Rule {

	private String destination;

	public AllowRule(Set<String> newLabels, String destination) {
		this.setLabel(newLabels);
		this.setDestination(destination);
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
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

        if (labelRule.destination.equals(destination) &&
        		this.getLabel().equals(labelRule.getLabel()))
            return true;
        else    
        	return false;
    }
}
