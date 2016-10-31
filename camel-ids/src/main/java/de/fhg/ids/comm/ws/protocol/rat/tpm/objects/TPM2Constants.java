package de.fhg.ids.comm.ws.protocol.rat.tpm.objects;

public interface TPM2Constants {

	/*
	 * Defines for Logic Values
	 * #define YES   1
	 * #define NO    0
	 * #define SET   1
	 * #define CLEAR 0
	 */
	public final int YES = 1;
	public final int NO = 0;
	public final int SET = 1;
	public final int CLEAR = 0;
	
	/*
	 * Defines for RSA Algorithm Constants
	 * #define MAX_RSA_KEY_BITS  2048
	 * #define MAX_RSA_KEY_BYTES ((MAX_RSA_KEY_BITS + 7) / 8)
	 */
	public final int MAX_RSA_KEY_BITS = 2048;
	public final int MAX_RSA_KEY_BYTES = ((MAX_RSA_KEY_BITS + 7 ) / 8);
	
	/*
	 * Defines for ECC Algorithm Constants
	 * #define MAX_ECC_KEY_BITS  256
	 * #define MAX_ECC_KEY_BYTES ((MAX_ECC_KEY_BITS + 7) / 8)
	 */
	public final int MAX_ECC_KEY_BITS = 256;
	public final int MAX_ECC_KEY_BYTES = ((MAX_ECC_KEY_BITS + 7) / 8);

	/*
	 * Defines for AES Algorithm Constants
	 * #define MAX_AES_KEY_BITS         128
	 * #define MAX_AES_BLOCK_SIZE_BYTES 16
	 * #define MAX_AES_KEY_BYTES        ((MAX_AES_KEY_BITS + 7) / 8)
	 */
	public final int MAX_AES_KEY_BITS = 128;
	public final int MAX_AES_BLOCK_SIZE_BYTES = 16;
	public final int MAX_AES_KEY_BYTES = ((MAX_AES_KEY_BITS + 7) / 8);

	/*
	 * Defines for SM4 Algorithm Constants
	 * #define MAX_SM4_KEY_BITS         128
	 * #define MAX_SM4_BLOCK_SIZE_BYTES 16
	 * #define MAX_SM4_KEY_BYTES        ((MAX_SM4_KEY_BITS + 7) / 8)
	 */
	public final int MAX_SM4_KEY_BITS = 128;
	public final int MAX_SM4_BLOCK_SIZE_BYTES = 16;
	public final int MAX_SM4_KEY_BYTES = ((MAX_SM4_KEY_BITS + 7) / 8);
	
	/*
	 * Defines for Symmetric Algorithm Constants
	 * #define MAX_SYM_KEY_BITS   MAX_AES_KEY_BITS
	 * #define MAX_SYM_KEY_BYTES  MAX_AES_KEY_BYTES
	 * #define MAX_SYM_BLOCK_SIZE MAX_AES_BLOCK_SIZE_BYTES
	 */
	public final int MAX_SYM_KEY_BITS = MAX_AES_KEY_BITS;
	public final int MAX_SYM_KEY_BYTES = MAX_AES_KEY_BYTES;
	public final int MAX_SYM_BLOCK_SIZE = MAX_AES_BLOCK_SIZE_BYTES;
}
