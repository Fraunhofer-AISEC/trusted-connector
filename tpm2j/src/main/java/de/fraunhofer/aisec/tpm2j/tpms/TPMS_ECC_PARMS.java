package de.fraunhofer.aisec.tpm2j.tpms;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_GENERATED;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ECC_CURVE.ECC_CURVE;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ECC_CURVE;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_ECC_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_KDF_SCHEME;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SYM_DEF_OBJECT;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_PUBLIC_PARMS;

public class TPMS_ECC_PARMS extends TPMU_PUBLIC_PARMS {

	/*
	 * TPMS_ECC_PARMS Structure
	 * typedef struct {
	 *     TPMT_SYM_DEF_OBJECT symmetric;
	 *     TPMT_ECC_SCHEME     scheme;
	 *     TPMI_ECC_CURVE      curveID;
	 *     TPMT_KDF_SCHEME     kdf;
	 * } TPMS_ECC_PARMS;
	 */
	
	TPMT_SYM_DEF_OBJECT symmetric;
	TPMT_ECC_SCHEME scheme;
	TPMI_ECC_CURVE curveID;
	TPMT_KDF_SCHEME kdf;

	@Override
	public byte[] getBuffer() {
		return this.toBytes();
	} 
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(symmetric, scheme, curveID, kdf);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.symmetric = new TPMT_SYM_DEF_OBJECT();
        brw.readStruct(this.symmetric);
        this.scheme = new TPMT_ECC_SCHEME();
        brw.readStruct(this.scheme);
        this.curveID = new TPMI_ECC_CURVE();
        brw.readStruct(this.curveID);
        this.kdf = new TPMT_KDF_SCHEME();
        brw.readStruct(this.kdf);
	}

	@Override
	public String toString() {
		return "TPMS_ECC_PARMS:[symmetric="+this.symmetric.toString()
			+ ", scheme=" + this.scheme.toString()
			+ ", curveID=" + this.curveID.toString()
			+ ", kdf=" + this.kdf.toString() + "]";
	}

}
