package de.fhg.aisec.dfpolicy;

public class LabelRule {

	private String source;
	private String label;
	
	public LabelRule(String source, String label) {
		this.source = source;
		this.label = label;
	}

	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return "LabelRule [source=" + source + ", label=" + label + "]";
	}
}
