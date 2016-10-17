package de.fhg.ids.comm.ws.protocol.rat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RemoteAttestationHandler {
	public byte[] setNonce() {
		byte[] ret = null;
		try {
			//Initialize SecureRandom
			SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
			//generate a random number
			String randomNum = new Integer(prng.nextInt()).toString();
			//get its digest
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			ret = sha.digest(randomNum.getBytes());
		}
		catch (NoSuchAlgorithmException ex) {
			System.err.println(ex);
		}
		return ret;
	}
}
