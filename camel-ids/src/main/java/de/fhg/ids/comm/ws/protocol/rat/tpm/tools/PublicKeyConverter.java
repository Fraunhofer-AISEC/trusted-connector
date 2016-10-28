package de.fhg.ids.comm.ws.protocol.rat.tpm.tools;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import de.fhg.ids.comm.ws.protocol.rat.tpm.objects.TPM2B_PUBLIC;

public class PublicKeyConverter {
	
	private final String openSSLfixedHeader = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA";
	private byte[] midHeader = new byte[2];
	private byte[] exponent = new byte[3];
	private PublicKey key = null;
	private KeyFactory kf;
	private X509EncodedKeySpec spec;
	
	public PublicKeyConverter(TPM2B_PUBLIC publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] keyBuffer  = this.setExponent(this.setMidHeader(this.setModulus(this.setFixedHeader(), publicKey)));
		spec = new X509EncodedKeySpec(keyBuffer);
		kf = KeyFactory.getInstance("RSA");
		this.key = kf.generatePublic(spec);
	}
	
	private byte[] setModulus(byte[] key, TPM2B_PUBLIC publicKey) {
		return PublicKeyConverter.combineByteArray(key, publicKey.getPublicArea().getUnique().getBuffer());
	}

	public PublicKey getPublicKey() {
		return this.key;
	}
	
	private byte[] setFixedHeader() {
		return Base64.getDecoder().decode(openSSLfixedHeader);
	}
	
	private byte[] setMidHeader(byte[] key) {
		// Mid-header is always 0x02 0x03 i.e. the exponent is a 3 bytes (0x03) integer (0x02)
		midHeader[0] = 0x02;
		midHeader[1] = 0x03;
		return PublicKeyConverter.combineByteArray(key, midHeader);
	}
	
	private byte[] setExponent(byte[] key) {
		// Exponent is always 65537 (2^16+1) 
		exponent[0] = 0x01;
		exponent[1] = 0x00;
		exponent[2] = 0x01;
		return PublicKeyConverter.combineByteArray(key, exponent);
	}
	
	private static byte[] combineByteArray(byte[] one, byte[] two) {
		byte[] three = new byte[one.length + two.length];
		System.arraycopy(one, 0, three, 0, one.length);
		System.arraycopy(two, 0, three, one.length, two.length);
		return three;
	}
}
