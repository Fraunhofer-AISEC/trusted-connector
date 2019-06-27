package de.fhg.aisec.tpm2j.tpmt;

import de.fhg.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fhg.aisec.tpm2j.tools.ByteArrayUtil;
import de.fhg.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fhg.aisec.tpm2j.tpmu.TPMU_HA;
import de.fhg.aisec.tpm2j.tpmu.TPMU_NAME;

public class TPMT_HA extends TPMU_NAME {

	/*
	 * TPMT_HA Structure
	 * typedef struct {
	 *     TPMI_ALG_HASH hashAlg;
	 *     TPMU_HA       digest;
	 * } TPMT_HA;
	 */
	
	TPMI_ALG_HASH hashAlg;
	TPMU_HA digest;
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(hashAlg, digest);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter(source, offset);
        this.hashAlg = new TPMI_ALG_HASH();
        brw.readStruct(this.hashAlg);
        this.digest = new TPMU_HA(this.hashAlg.getHashId().getAlgId());
        brw.readStruct(this.digest);
	}

	@Override
	public String toString() {
		return "TPMT_HA[hashAlg=" + this.hashAlg.toString() 
			+ ", digest=" + this.digest.toString() + "]";
	}	
}
