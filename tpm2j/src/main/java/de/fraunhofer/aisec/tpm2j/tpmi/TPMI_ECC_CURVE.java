package de.fraunhofer.aisec.tpm2j.tpmi;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ECC_CURVE;

public class TPMI_ECC_CURVE extends StandardTPMStruct {

	/*
	 * TPMI_ECC_CURVE Type
	 * typedef TPM_ECC_CURVE TPMI_ECC_CURVE;
	 */
	
	private TPM_ECC_CURVE eccCurve;

	public TPM_ECC_CURVE getEccCurve() {
		return eccCurve;
	}

	public void setEccCurve(TPM_ECC_CURVE eccCurve) {
		this.eccCurve = eccCurve;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(eccCurve);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.eccCurve = new TPM_ECC_CURVE();
        brw.readStruct(this.eccCurve);
	}

	@Override
	public String toString() {
		return "TPMI_ECC_CURVE:[eccCurve="+this.eccCurve.toString()+"]";
	}
}
