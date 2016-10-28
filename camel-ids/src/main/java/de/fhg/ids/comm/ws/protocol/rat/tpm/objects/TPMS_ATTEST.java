package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

import de.fhg.ids.comm.ws.protocol.rat.ByteArrayReadWriter;
import de.fhg.ids.comm.ws.protocol.rat.ByteArrayUtil;

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
	public byte[] getBuffer() {
		return this.toBytes();
	} 

	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(magic, type, qualifiedSigner, extraData, clockInfo, firmwareVersion, attested);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
        ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
        this.magic = new TPM_GENERATED();
        brw.readStruct(this.magic);
        this.qualifiedSigner = new TPM2B_NAME();
        brw.readStruct(this.qualifiedSigner);
        this.extraData = new TPM2B_DATA();
        brw.readStruct(this.extraData);    
        this.clockInfo = new TPMS_CLOCK_INFO();
        brw.readStruct(this.clockInfo);
        this.firmwareVersion = brw.readLong();
        /*this.attested = 
        
        this.attested = new TPMU_ATTEST();
        brw.readStruct(this.attested);        */
	}

	@Override
    public String toString() {
        return "TPMS_ATTEST:[magic = " + this.magic 
        		+ ", type = " + this.type.toString() 
        		+ ", qualifiedSigner = " + this.qualifiedSigner.toString() 
        		+ ", extraData = " + this.extraData.toString()
        		+ ", clockInfo = " + this.clockInfo.toString()
        		+ ", firmwareVersion = " + this.firmwareVersion
        		+ ", attested: " + this.attested.toString() +"]";
    }
}
