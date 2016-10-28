package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

import de.fhg.ids.comm.ws.protocol.rat.tpmobjects.TPM_ALG_ID.ALG_ID;

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
	
	public TPMT_PUBLIC() {
	}
	
	public TPMT_PUBLIC(byte[] buffer) {
		this.fromBytes(buffer, 0);
	}

	public TPMT_PUBLIC(
			TPMI_ALG_PUBLIC type, 
			TPMI_ALG_HASH nameAlg, 
			TPMA_OBJECT objectAttributes, 
			TPM2B_DIGEST authPolicy,
			TPMU_PUBLIC_PARMS parameters,
			TPMU_PUBLIC_ID unique) {
		this.type = type;
		this.nameAlg = nameAlg;
		this.objectAttributes = objectAttributes;
		this.authPolicy = authPolicy;
		this.parameters = parameters;
		this.unique = unique;
	}
	
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
	public byte[] getBuffer() {
		return this.toBytes();
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
        LOG.debug(this.type.toString());
        this.nameAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.nameAlg);
        LOG.debug(this.nameAlg.toString());
        this.objectAttributes = new TPMA_OBJECT();
        brw.readStruct(this.objectAttributes);
        LOG.debug(this.objectAttributes.toString());
        this.authPolicy = new TPM2B_DIGEST();
        brw.readStruct(this.authPolicy);
        LOG.debug(this.authPolicy.toString());
        ALG_ID algId = this.type.getAlgId().getAlgId();
        ALG_ID hashId = this.nameAlg.getHashId().getAlgId();
        switch(algId) {
        	case TPM_ALG_RSA:
        			this.parameters = new TPMS_RSA_PARMS();
        			brw.readStruct(this.parameters);
        			this.unique = new TPM2B_PUBLIC_KEY_RSA();
        			brw.readStruct(this.unique);
        		break;

        		// TODO: put other algorithm cases here

        	default:
        		break;
        }
	}
	
	@Override
    public String toString() {
        return "TPMT_PUBLIC:[type = " + this.type.toString() 
        	+ ", nameAlg = " + this.nameAlg.toString() 
        	+ ", objectAttributes = " + this.objectAttributes.toString() 
        	+ ", authPolicy = " + this.authPolicy.toString()
        	+ ", parameters = " + this.parameters.toString()
        	+ ", unique = " + this.unique.toString() + "]";
    }

}
