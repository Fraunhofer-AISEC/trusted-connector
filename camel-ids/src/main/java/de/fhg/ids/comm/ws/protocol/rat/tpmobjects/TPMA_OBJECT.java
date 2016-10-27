package de.fhg.ids.comm.ws.protocol.rat.tpmobjects;

public class TPMA_OBJECT extends StandardTPMStruct {
	
	/*
	 * TPMA_OBJECT Bits
	 * typedef struct {
	 *     UINT32 reserved1            : 1;
	 *     UINT32 fixedTPM             : 1;
	 *     UINT32 stClear              : 1;
	 *     UINT32 reserved4            : 1;
	 *     UINT32 fixedParent          : 1;
	 *     UINT32 sensitiveDataOrigin  : 1;
	 *     UINT32 userWithAuth         : 1;
	 *     UINT32 adminWithPolicy      : 1;
	 *     UINT32 reserved8_9          : 2;
	 *     UINT32 noDA                 : 1;
	 *     UINT32 encryptedDuplication : 1;
	 *     UINT32 reserved12_15        : 4;
	 *     UINT32 restricted           : 1;
	 *     UINT32 decrypt              : 1;
	 *     UINT32 sign                 : 1;
	 *     UINT32 reserved19_31        : 13;
	 * } TPMA_OBJECT;
	 */
	
	private int reserved1;
	private int fixedTPM;
	private int stClear;
	private int reserved4;
	private int fixedParent;
	private int sensitiveDataOrigin;
	private int userWithAuth;
	private int adminWithPolicy;
	private int reserved8_9;
	private int noDA;
	private int encryptedDuplication;
	private int reserved12_15;
	private int restricted;
	private int decrypt;
	private int sign;
	private int reserved19_31;
	
	public TPMA_OBJECT() {
	}
	
	public TPMA_OBJECT(
			int reserved1,
			int fixedTPM,
			int stClear,
			int reserved4,
			int fixedParent,
			int sensitiveDataOrigin,
			int userWithAuth,
			int adminWithPolicy,
			int reserved8_9,
			int noDA,
			int encryptedDuplication,
			int reserved12_15,
			int restricted,
			int decrypt,
			int sign,
			int reserved19_31) {
		this.reserved1 = reserved1;
		this.fixedTPM = fixedTPM;
		this.stClear = stClear;
		this.fixedParent = fixedParent;
		this.sensitiveDataOrigin = sensitiveDataOrigin;
		this.userWithAuth = userWithAuth;
		this.adminWithPolicy = adminWithPolicy;
		this.reserved8_9 = reserved8_9;
		this.noDA = noDA;
		this.encryptedDuplication = encryptedDuplication;
		this.reserved12_15 = reserved12_15;
		this.restricted = restricted;
		this.decrypt = decrypt;
		this.sign = sign;
		this.reserved19_31 = reserved19_31;
	}

	public TPMA_OBJECT(byte[] buffer) {
		this.fromBytes(buffer, 0);
	}
	
	public int getReserved1() {
		return reserved1;
	}

	public void setReserved1(int reserved1) {
		this.reserved1 = reserved1;
	}

	public int getFixedTPM() {
		return fixedTPM;
	}

	public void setFixedTPM(int fixedTPM) {
		this.fixedTPM = fixedTPM;
	}

	public int getStClear() {
		return stClear;
	}

	public void setStClear(int stClear) {
		this.stClear = stClear;
	}

	public int getReserved4() {
		return reserved4;
	}

	public void setReserved4(int reserved4) {
		this.reserved4 = reserved4;
	}

	public int getFixedParent() {
		return fixedParent;
	}

	public void setFixedParent(int fixedParent) {
		this.fixedParent = fixedParent;
	}

	public int getSensitiveDataOrigin() {
		return sensitiveDataOrigin;
	}

	public void setSensitiveDataOrigin(int sensitiveDataOrigin) {
		this.sensitiveDataOrigin = sensitiveDataOrigin;
	}

	public int getUserWithAuth() {
		return userWithAuth;
	}

	public void setUserWithAuth(int userWithAuth) {
		this.userWithAuth = userWithAuth;
	}

	public int getAdminWithPolicy() {
		return adminWithPolicy;
	}

	public void setAdminWithPolicy(int adminWithPolicy) {
		this.adminWithPolicy = adminWithPolicy;
	}

	public int getReserved8_9() {
		return reserved8_9;
	}

	public void setReserved8_9(int reserved8_9) {
		this.reserved8_9 = reserved8_9;
	}

	public int getNoDA() {
		return noDA;
	}

	public void setNoDA(int noDA) {
		this.noDA = noDA;
	}

	public int getEncryptedDuplication() {
		return encryptedDuplication;
	}

	public void setEncryptedDuplication(int encryptedDuplication) {
		this.encryptedDuplication = encryptedDuplication;
	}

	public int getReserved12_15() {
		return reserved12_15;
	}

	public void setReserved12_15(int reserved12_15) {
		this.reserved12_15 = reserved12_15;
	}

	public int getRestricted() {
		return restricted;
	}

	public void setRestricted(int restricted) {
		this.restricted = restricted;
	}

	public int getDecrypt() {
		return decrypt;
	}

	public void setDecrypt(int decrypt) {
		this.decrypt = decrypt;
	}

	public int getSign() {
		return sign;
	}

	public void setSign(int sign) {
		this.sign = sign;
	}

	public int getReserved19_31() {
		return reserved19_31;
	}

	public void setReserved19_31(int reserved19_31) {
		this.reserved19_31 = reserved19_31;
	}

	public String toString() {
        return "TPMA_OBJECT: \n" 
        		+ "reserved1 = " + this.reserved1 + "\n"
        		+ "fixedTPM = " + this.fixedTPM + "\n"
        		+ "stClear = " + this.stClear + "\n"
        		+ "reserved4 = " + this.reserved4 + "\n"
        		+ "fixedParent = " + this.fixedParent + "\n"
        		+ "sensitiveDataOrigin = " + this.sensitiveDataOrigin + "\n"
        		+ "userWithAuth = " + this.userWithAuth + "\n"
        		+ "adminWithPolicy = " + this.adminWithPolicy + "\n"
        		+ "reserved8_9 = " + this.reserved8_9 + "\n"
        		+ "noDA = " + this.noDA + "\n"
        		+ "encryptedDuplication = " + this.encryptedDuplication + "\n"
        		+ "reserved12_15 = " + this.reserved12_15 + "\n"
        		+ "restricted = " + this.restricted + "\n"
        		+ "decrypt = " + this.decrypt + "\n"
        		+ "sign = " + this.sign + "\n"
        		+ "reserved19_31 = " + this.reserved19_31 + "\n";
    }
	
	@Override
	public byte[] toBytes() {
		return ByteArrayUtil.buildBuf(
				reserved1, 
				fixedTPM, 
				stClear, 
				reserved4, 
				fixedParent, 
				sensitiveDataOrigin,
				userWithAuth,
				adminWithPolicy,
				reserved8_9,
				noDA,
				encryptedDuplication,
				reserved12_15,
				restricted,
				decrypt,
				sign,
				reserved19_31
		);
	}

	@Override
	public void fromBytes(byte[] source, int offset) {
		ByteArrayReadWriter brw = new ByteArrayReadWriter( source, offset );
		this.reserved1 = brw.readInt32();
		this.fixedTPM = brw.readInt32();
		this.stClear = brw.readInt32();
		this.reserved4 = brw.readInt32();
		this.fixedParent = brw.readInt32();
		this.sensitiveDataOrigin = brw.readInt32();
		this.userWithAuth = brw.readInt32();
		this.adminWithPolicy = brw.readInt32();
		this.reserved8_9 = brw.readInt32();
		this.noDA = brw.readInt32();
		this.encryptedDuplication = brw.readInt32();
		this.reserved12_15 = brw.readInt32();
		this.restricted = brw.readInt32();
		this.decrypt = brw.readInt32();
		this.sign = brw.readInt32();
		this.reserved19_31 = brw.readInt32();
	}	
}
