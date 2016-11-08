package de.fhg.ids.comm.ws.protocol.rat.tpm20.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class NonceGenerator {
	private static MessageDigest digest;
	private static String nonce;
	
	// generate and return a new nonce
	public static String generate() {
		nonce = generateNew();
		return nonce;
	}
	
	// generate a new software nonce in sha-1
	private static String generateNew() {
		String ret = null;
		try {
			SecureRandom random = SecureRandom.getInstanceStrong();
			byte[] values = new byte[20];
			random.nextBytes(values);
			digest = MessageDigest.getInstance("SHA-1");
			digest.update(NonceGenerator.generateSalt());
			ret = javax.xml.bind.DatatypeConverter.printHexBinary(digest.digest(values));
		}
		catch (NoSuchAlgorithmException ex) {
			System.err.println(ex);
		}
		//System.out.println("generated nonce: " + ret);
		return ret;
	} 
	
    //generate some salt
    private static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }
}
