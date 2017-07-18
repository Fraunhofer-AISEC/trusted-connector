package de.fraunhofer.aisec.tpm2j.tpms;

import java.nio.ByteBuffer;

import de.fraunhofer.aisec.tpm2j.struct.StandardTPMStruct;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayReadWriter;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_GENERATED;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_DATA;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_NAME;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ST_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmu.TPMU_ATTEST;

public class TPMS_ATTEST extends StandardTPMStruct {

	/*
	 * TPMS_ATTEST Structure
	 * typedef struct {
	 *     TPM_GENERATED   magic;
	 *     TPMI_ST_ATTEST  type;
	 *     TPM2B_NAME      qualifiedSigner;
	 *     TPM2B_DATA      extraData;
	 *     TPMS_CLOCK_INFO clockInfo;
	 *     UINT64          firmwareVersion;
	 *     TPMU_ATTEST     attested;
	 * } TPMS_ATTEST;
	 */

	private TPM_GENERATED magic; // TPM_GENERATED equals 4 Byte or 4*8 = 32 Bit => int
	private TPMI_ST_ATTEST type;
	private TPM2B_NAME qualifiedSigner;
	private TPM2B_DATA extraData;
	private TPMS_CLOCK_INFO clockInfo;
	private long firmwareVersion;
	private TPMU_ATTEST attested;

	public TPMS_ATTEST(byte[] yourQuoted) throws Exception {
		this.fromBytes(yourQuoted, 0);
	}
	
	public TPM_GENERATED getMagic() {
		return magic;
	}

	public void setMagic(TPM_GENERATED magic) {
		this.magic = magic;
	}

	public TPMI_ST_ATTEST getType() {
		return type;
	}

	public void setType(TPMI_ST_ATTEST type) {
		this.type = type;
	}

	public TPM2B_NAME getQualifiedSigner() {
		return qualifiedSigner;
	}

	public void setQualifiedSigner(TPM2B_NAME qualifiedSigner) {
		this.qualifiedSigner = qualifiedSigner;
	}

	public TPM2B_DATA getExtraData() {
		return extraData;
	}

	public void setExtraData(TPM2B_DATA extraData) {
		this.extraData = extraData;
	}

	public TPMS_CLOCK_INFO getClockInfo() {
		return clockInfo;
	}

	public void setClockInfo(TPMS_CLOCK_INFO clockInfo) {
		this.clockInfo = clockInfo;
	}

	public long getFirmwareVersion() {
		return firmwareVersion;
	}

	public void setFirmwareVersion(long firmwareVersion) {
		this.firmwareVersion = firmwareVersion;
	}

	public TPMU_ATTEST getAttested() {
		return attested;
	}

	public void setAttested(TPMU_ATTEST attested) {
		this.attested = attested;
	}

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(magic, type, qualifiedSigner, extraData, clockInfo, firmwareVersion, attested);
	}

	@Override
	public void fromBytes(byte[] source, int offset) throws Exception {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.magic = new TPM_GENERATED();
        brw.readStruct(this.magic);
        this.type = new TPMI_ST_ATTEST();
        brw.readStruct(this.type);
        this.qualifiedSigner = new TPM2B_NAME();
        brw.readStruct(this.qualifiedSigner);
        this.extraData = new TPM2B_DATA();
        brw.readStruct(this.extraData);    
        this.clockInfo = new TPMS_CLOCK_INFO();
        brw.readStruct(this.clockInfo);
        this.firmwareVersion = brw.readLong();
        switch(this.type.getStId().getStId()) {
	    	case TPM_ST_ATTEST_CERTIFY:
	    		this.attested = new TPMS_CERTIFY_INFO();
	    		brw.readStruct(this.attested);
	    		break;
	        case TPM_ST_ATTEST_QUOTE:
	        	this.attested = new TPMS_QUOTE_INFO();
	        	brw.readStruct(this.attested);
	        	break;
	    	case TPM_ST_ATTEST_SESSION_AUDIT:
	    		this.attested = new TPMS_SESSION_AUDIT_INFO();
	    		brw.readStruct(this.attested);
	    		break;
        	case TPM_ST_ATTEST_COMMAND_AUDIT:
        		this.attested = new TPMS_COMMAND_AUDIT_INFO();
        		brw.readStruct(this.attested);
        		break;
        	case TPM_ST_ATTEST_TIME:
        		this.attested = new TPMS_TIME_ATTEST_INFO();
        		brw.readStruct(this.attested);
        		break;
        	case TPM_ST_ATTEST_CREATION:
        		this.attested = new TPMS_CREATION_INFO();
        		brw.readStruct(this.attested);
        		break;
        	case TPM_ST_ATTEST_NV:
        		this.attested = new TPMS_NV_CERTIFY_INFO();
        		brw.readStruct(this.attested);
        		break;
        	default:
        		throw new Exception("TPMS_ATTEST wrong TPMI_ST_ATTEST type set");
        }
	}

	@Override
    public String toString() {
        return "TPMS_ATTEST:[magic=" + this.magic 
        		+ ", type=" + this.type.toString() 
        		+ ", qualifiedSigner=" + this.qualifiedSigner.toString() 
        		+ ", extraData=" + this.extraData.toString()
        		+ ", clockInfo=" + this.clockInfo.toString()
        		+ ", firmwareVersion=" + ByteArrayUtil.toPrintableHexString(ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(firmwareVersion).array())
        		+ ", attested=" + this.attested.toString() +"]";
    }
}
