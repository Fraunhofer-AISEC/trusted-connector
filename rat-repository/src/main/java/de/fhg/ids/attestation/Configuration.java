package de.fhg.ids.attestation;

import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;

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
	
	@Override
	public String toString() {
		String pcr = "";
		for(int i = 0; i < this.values.length; i++) {
			pcr += "{"+this.values[i].getNumber()+":"+this.values[i].getValue()+"},";
		}
		return "["+this.name+"(id:"+this.id+", type:"+this.type+")=["+pcr.substring(0, pcr.length() - 1)+"]]";
	}
}
