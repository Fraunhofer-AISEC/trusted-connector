package de.fhg.aisec.ids.attestation;

public class Configuration {
	private long id;
	private String name;
	private String type;
	private Pcr[] values;
	
	public Configuration(long id, String name, String type, Pcr[] values) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.values = values;
	}
	
	public Configuration(long id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}	
	
	public Pcr[] getValues() {
		return values;
	}

	public void setValues(Pcr[] values) {
		this.values = values;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
