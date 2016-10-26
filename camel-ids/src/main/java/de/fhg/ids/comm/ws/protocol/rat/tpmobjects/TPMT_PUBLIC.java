package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMT_PUBLIC extends StandardTPMStruct {

	/*
	 * TPMT_PUBLIC Structure
	 * typedef struct {
	 *     TPMI_ALG_PUBLIC   type;
	 *     TPMI_ALG_HASH     nameAlg;
	 *     TPMA_OBJECT       objectAttributes;
	 *     TPM2B_DIGEST      authPolicy;
	 *     TPMU_PUBLIC_PARMS parameters;
	 *     TPMU_PUBLIC_ID    unique;
	 * } TPMT_PUBLIC;
	 */
	
	private TPMI_ALG_PUBLIC type;
	private TPMI_ALG_HASH nameAlg;
	private TPMA_OBJECT objectAttributes;
	private TPM2B_DIGEST authPolicy;
	private TPMU_PUBLIC_PARMS parameters;
	private TPMU_PUBLIC_ID unique;
	
	public TPMI_ALG_PUBLIC getType() {
		return type;
	}

	public void setType(TPMI_ALG_PUBLIC type) {
		this.type = type;
	}

	public TPMI_ALG_HASH getNameAlg() {
		return nameAlg;
	}

	public void setNameAlg(TPMI_ALG_HASH nameAlg) {
		this.nameAlg = nameAlg;
	}

	public TPMA_OBJECT getObjectAttributes() {
		return objectAttributes;
	}

	public void setObjectAttributes(TPMA_OBJECT objectAttributes) {
		this.objectAttributes = objectAttributes;
	}

	public TPM2B_DIGEST getAuthPolicy() {
		return authPolicy;
	}

	public void setAuthPolicy(TPM2B_DIGEST authPolicy) {
		this.authPolicy = authPolicy;
	}

	public TPMU_PUBLIC_PARMS getParameters() {
		return parameters;
	}

	public void setParameters(TPMU_PUBLIC_PARMS parameters) {
		this.parameters = parameters;
	}

	public TPMU_PUBLIC_ID getUnique() {
		return unique;
	}

	public void setUnique(TPMU_PUBLIC_ID unique) {
		this.unique = unique;
	}
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(type, nameAlg, objectAttributes, authPolicy, parameters, unique);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.type = new TPMI_ALG_PUBLIC();
        brw.readStruct(this.type);
        this.nameAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.nameAlg);
        this.objectAttributes = new TPMA_OBJECT();
        brw.readStruct(this.objectAttributes);
        this.authPolicy = new TPM2B_DIGEST();
        brw.readStruct(this.authPolicy);
        this.parameters = new TPMU_PUBLIC_PARMS();
        brw.readStruct(this.parameters);
        this.unique = new TPMU_PUBLIC_ID();
        brw.readStruct(this.unique);
	}
	
    public String toString() {
        return "TPMT_PUBLIC: \n" 
        		+ "type = " + this.type.toString() + "\n"
        		+ "nameAlg = " + this.nameAlg.toString() + "\n"
        		+ "objectAttributes = " + this.objectAttributes.toString() + "\n"
        		+ "authPolicy = " + this.authPolicy.toString() + "\n"
        		+ "parameters = " + this.parameters.toString() + "\n"
        		+ "unique = " + this.unique.toString();
    }

}
