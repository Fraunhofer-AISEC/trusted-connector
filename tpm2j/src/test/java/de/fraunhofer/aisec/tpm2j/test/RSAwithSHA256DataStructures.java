package de.fraunhofer.aisec.tpm2j.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID.ALG_ID;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_HASH;
import de.fraunhofer.aisec.tpm2j.tpmi.TPMI_ALG_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;

public class RSAwithSHA256DataStructures {
	
	// byte arrays
	byte[] byteQuote = new byte[0];
	byte[] byteSignature = new byte[0];
	byte[] bytePubKey = new byte[0];
	
	// tpm2j structures
	TPM2B_PUBLIC pubkey;
	TPMT_SIGNATURE signature;
	TPMS_ATTEST quoted;
	
	@Before
	public void loadData() throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		this.byteQuote = RSAwithSHA256DataStructures.readFully(loader.getResourceAsStream("quoted.bin"));
		this.byteSignature = RSAwithSHA256DataStructures.readFully(loader.getResourceAsStream("sig.bin"));
		this.bytePubKey = RSAwithSHA256DataStructures.readFully(loader.getResourceAsStream("sigpub.bin"));	
	}

	@Test
	public void testPublicKeytoString() throws Exception {
		this.pubkey = new TPM2B_PUBLIC(this.bytePubKey);
		String defaultOutput = "TPM2B_PUBLIC:[size=278, publicArea=TPMT_PUBLIC:[type=TPMI_ALG_PUBLIC:[algId=(description=\"FIRST\", id=1)], nameAlg=TPMI_ALG_HASH:[hashId=(description=\"SHA256\", id=11)], objectAttributes=TPMA_OBJECT:[bitmask=0x00040472], authPolicy=TPM2B_DIGEST:[(0 byte): ], parameters=TPMS_RSA_PARMS:[symmetric=TPMT_SYM_DEF_OBJECT:[algorithm=TPMI_ALG_PUBLIC:[algId=(description=\"NULL\", id=16)]], scheme=TPMT_RSA_SCHEME:[scheme=TPMI_ALG_RSA_SCHEME:[algId=(description=\"NULL\", id=16)]], keyBits=TPM_KEY_BITS:(2048 bit), exponent=0], unique=TPM2B_PUBLIC_KEY_RSA:[(256 byte): aa cd ed 2b 00 01 2f a6 dd 0c 8d d7 66 03 fa a7 87 f5 19 c7d0 7a 5a 67 57 3d 69 ed b7 85 dd 7c 02 d7 c4 a2 93 66 d6 9d25 3d 33 0f 51 13 33 23 d6 b5 00 37 5d d7 01 3e 4d 05 e5 c916 34 fd 92 7a d4 62 c7 0a 4f db 69 91 0e 96 65 fa 6b c1 0e9e 85 fc 5d 28 98 fb 44 5a 29 e3 5b 01 30 50 8b 24 88 13 b0c1 f7 38 cd e4 7b 3d 8b 33 96 85 13 d0 ff a6 07 4c 15 7d 4eb1 11 fc 75 ae ea 09 4a ae ba a2 98 18 72 39 dd 1c 1f 15 548f a9 18 30 aa e8 f7 cb 92 1c 5f 51 cf d7 dc 92 c1 bb bc 3aa1 72 59 31 3d 01 ac a8 a9 7d f2 7c 83 84 d6 86 f4 b7 60 5259 d9 52 2e 3b 1a 05 ed d3 50 f1 5d 86 d5 ce 7a 11 cb 23 8b90 44 4b 07 82 e4 28 b8 ee 61 61 48 4e 6d a8 f9 b8 bd 6f df7c a8 90 23 b7 3d f2 b5 77 4b 0f bb 72 a9 37 40 eb aa bf 102c 34 4f 50 c2 6b ce 62 4a 03 5e 4e ab 72 c7 8d]]]";
		assertTrue(this.pubkey.toString().equals(defaultOutput)); 
		
	}
	
	@Test
	public void testPublicKeySize() throws Exception {
		this.pubkey = new TPM2B_PUBLIC(this.bytePubKey);
		assertTrue(this.pubkey.getSize() == 278); 
	}
	
	@Test
	public void testPublicArea() throws Exception {
		this.pubkey = new TPM2B_PUBLIC(this.bytePubKey);
		assertTrue(this.pubkey.getPublicArea().getClass().equals(TPMT_PUBLIC.class)); 
	}
	
	@Test
	public void testPublicKey() throws Exception {
		this.pubkey = new TPM2B_PUBLIC(this.bytePubKey);
		assertTrue(this.pubkey.getPublicArea().getType().getClass().equals(TPMI_ALG_PUBLIC.class));
		assertTrue(this.pubkey.getPublicArea().getType().getAlgId().getAlgId().equals(ALG_ID.TPM_ALG_FIRST));
		assertTrue(this.pubkey.getPublicArea().getNameAlg().getHashId().getAlgId().equals(ALG_ID.TPM_ALG_SHA256));
		assertTrue(ByteArrayUtil.toPrintableHexString(this.pubkey.getPublicArea().getObjectAttributes().getBuffer()).equals("00 04 04 72"));
	}
	
	@Test
	public void testPublicAreaNameAlg() throws Exception {
		this.pubkey = new TPM2B_PUBLIC(this.bytePubKey);
		assertTrue(this.pubkey.getPublicArea().getNameAlg().getClass().equals(TPMI_ALG_HASH.class)); 
	}

	@Test
	public void testSignaturetoString() throws Exception {
		this.signature = new TPMT_SIGNATURE(this.byteSignature);
		String defaultOutput = "TPMT_SIGNATURE:[sigAlg=TPMI_ALG_SIG_SCHEME:[algId=(description=\"RSASSA\", id=20)], signature=TPMS_SIGNATURE_RSA:[hashAlg=TPMI_ALG_HASH:[hashId=(description=\"SHA256\", id=11)], sig=TPM2B_PUBLIC_KEY_RSA:[(256 byte): 44 7a 3d ef 49 27 0c 1c a0 af 27 b0 35 02 02 95 a8 36 2b b35e 86 e2 14 a2 b7 7e bb e5 34 be 11 62 c7 14 47 5f 29 4a 87c4 e6 4c 19 d8 73 8c 59 41 9f 20 cd e3 2c 38 e8 1c 75 22 e9c9 23 64 1a ab ec 62 a8 02 00 af 65 4b 2a 66 75 fc d0 19 57e6 fb bf e1 39 d0 67 fe b6 0e 72 79 60 08 96 22 b2 a7 33 a1de 64 e2 89 c9 31 fe 7e 01 58 94 fa 84 cd 24 eb 85 a8 0d 83eb 19 00 56 f1 40 92 94 54 ab 9e dc df 24 71 cb e6 17 67 b70d c7 78 f5 6c c0 ee 13 08 8b 8f 6a 43 dd 8b b6 87 fd 05 310a c7 d0 d9 1d 91 9d ce 9a 6c 72 e9 a8 50 4c 19 54 81 57 f3a1 80 2c 15 fd 91 ab b4 00 73 86 c0 4b fa 60 8f 33 44 73 a0ca af 35 3e 83 ef dd ba ce ae 82 9f 29 2b e4 22 6e db 5b f2c3 be 58 7e e7 83 66 72 23 b2 eb d2 2a 7a 49 85 b7 88 a1 43f7 f3 71 06 89 8d b7 2d 8c d8 a1 99 90 71 fc c2]]]";
		assertTrue(this.signature.toString().equals(defaultOutput)); 
		
	}

	@Test
	public void testQuotedtoString() throws Exception {
		this.quoted = new TPMS_ATTEST(this.byteQuote);
		String defaultOutput = "TPMS_ATTEST:[magic=(TPM_GENERATED=0xff544347), type=TPMI_ST_ATTEST:[stId=(description=\"ATTEST_QUOTE\", id=0x8018)], qualifiedSigner=TPM2B_NAME:[(34 byte): 00 0b 38 7c fd f4 57 91 01 b4 79 c6 ed 2a d4 c4 73 e7 a5 65cd b0 5c c6 2d 62 21 db d7 6f 02 ab 2b dd], extraData=TPM2B_DATA:[(3 byte): 61 61 61], clockInfo=TPMS_CLOCK_INFO:[clock=165, resetCount=0, restartCount=0, safe=TPMI_YES_NO:[01]], firmwareVersion=20 16 05 11 00 16 28 00, attested=TPMS_QUOTE_INFO:[pcrSelect=TPM2B_DIGEST:[(11 byte): 03 03 00 00 00 20 f5 a5 fd 42 d1], pcrDigest=TPML_PCR_SELECTION:[count=1, pcrSelections=[0:TPMS_PCR_SELECTION:[hash=TPMI_ALG_HASH:[hashId=(description=\"SHA256\", id=11)], sizeofSelect=3, pcrSelect=03 00 00]]]]]";
		assertTrue(this.quoted.toString().equals(defaultOutput)); 
		
	}
	
	public static byte[] readFully(InputStream input) throws IOException {
	    byte[] buffer = new byte[8192];
	    int bytesRead;
	    ByteArrayOutputStream output = new ByteArrayOutputStream();
	    while ((bytesRead = input.read(buffer)) != -1) {
	        output.write(buffer, 0, bytesRead);
	    }
	    return output.toByteArray();
	}
}
