package de.fhg.aisec.tpm2j.tpmu;

public abstract class TPMU_SCHEME implements TPMU_ASYM_SCHEME, TPMU_SIG_SCHEME {
	
	public abstract byte[] toBytes();

	public abstract void fromBytes(byte[] source, int offset) throws Exception;
	
	@Override
    public abstract String toString();	
}
