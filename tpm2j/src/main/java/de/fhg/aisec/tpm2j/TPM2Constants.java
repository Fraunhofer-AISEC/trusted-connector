package de.fhg.aisec.tpm2j;

import de.fhg.aisec.tpm2j.tpm.TPM_ALG_ID;

/**
 * All constants used inside tpm2j are part of this interface.
 * The values for those constants are taken from the header file Tpm20.h
 * 
 * @author georgraess
 * @version 1.0.3
 */
public interface TPM2Constants {
	
	/*
	 * Table 205 - Defines for SHA1 Hash Values
	 *     #define SHA1_DIGEST_SIZE 20
	 *     #define SHA1_BLOCK_SIZE  64
	*/
	public final int SHA1_DIGEST_SIZE = 20;
	public final int SHA1_BLOCK_SIZE = 64;
		
	/*
	 * Table 206 - Defines for SHA256 Hash Values
	 *     #define SHA256_DIGEST_SIZE 32
	 *     #define SHA256_BLOCK_SIZE  64
	*/
	public final int SHA256_DIGEST_SIZE = 32;
	public final int SHA256_BLOCK_SIZE = 64;
	
	/*
	 * Table 207 - Defines for SHA384 Hash Values
	 *     #define SHA384_DIGEST_SIZE 48
	 *     #define SHA384_BLOCK_SIZE  128
	 */
	public final int SHA384_DIGEST_SIZE = 48;
	public final int SHA384_BLOCK_SIZE = 128;
	
	/*
	 * Table 208 - Defines for SHA512 Hash Values
	 *     #define SHA512_DIGEST_SIZE 64
	 *     #define SHA512_BLOCK_SIZE  128
	 */
	public final int SHA512_DIGEST_SIZE = 64;
	public final int SHA512_BLOCK_SIZE = 128;
	
	/*
	 * Table 209 - Defines for SM3_256 Hash Values
	 *     #define SM3_256_DIGEST_SIZE 32
	 *     #define SM3_256_BLOCK_SIZE  64
	 */
	public final int SM3_256_DIGEST_SIZE = 32;
	public final int SM3_256_BLOCK_SIZE = 64;
	
	/*
	 * Table 210 - Defines for Architectural Limits Values
	 *     #define MAX_SESSION_NUMBER 3
	 */
	public final int MAX_SESSION_NUMBER = 3;
	
	/* Defines for Logic Values
	 *     #define YES   1
	 *     #define NO    0
	 *     #define SET   1
	 *     #define CLEAR 0
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
	
	
	/* Table 220 - Defines for Implementation Values
	#define BUFFER_ALIGNMENT              4
	#define IMPLEMENTATION_PCR            24
	#define PLATFORM_PCR                  24
	#define DRTM_PCR                      17
	#define NUM_LOCALITIES                5
	#define MAX_HANDLE_NUM                3
	#define MAX_ACTIVE_SESSIONS           64
	#define MAX_LOADED_SESSIONS           3
	#define MAX_SESSION_NUM               3
	#define MAX_LOADED_OBJECTS            3
	#define MIN_EVICT_OBJECTS             2	
	*/
	public final int BUFFER_ALIGNMENT = 4;
	public final int IMPLEMENTATION_PCR = 24;
	public final int PLATFORM_PCR = 24;
	public final int DRTM_PCR = 17;
	public final int NUM_LOCALITIES = 5;
	public final int MAX_HANDLE_NUM = 3;
	public final int MAX_ACTIVE_SESSIONS = 64;
	public final int MAX_LOADED_SESSIONS = 3;
	public final int MAX_SESSION_NUM = 3;
	public final int MAX_LOADED_OBJECTS = 3;
	public final int MIN_EVICT_OBJECTS = 2;

	/*
	#define PCR_SELECT_MIN                ((PLATFORM_PCR + 7) / 8)
	#define PCR_SELECT_MAX                ((IMPLEMENTATION_PCR + 7) / 8)
	*/
	public final int PCR_SELECT_MIN = ((PLATFORM_PCR + 7) / 8);
	public final int PCR_SELECT_MAX = ((IMPLEMENTATION_PCR + 7) / 8);
	
	/*
	#define NUM_POLICY_PCR_GROUP          1
	#define NUM_AUTHVALUE_PCR_GROUP       1
	#define MAX_CONTEXT_SIZE              4000
	#define MAX_DIGEST_BUFFER             1024
	#define MAX_NV_INDEX_SIZE             1024
	#define MAX_CAP_BUFFER                1024
	#define NV_MEMORY_SIZE                16384
	#define NUM_STATIC_PCR                16
	#define MAX_ALG_LIST_SIZE             64
	#define TIMER_PRESCALE                100000
	#define PRIMARY_SEED_SIZE             32
	#define CONTEXT_ENCRYPT_ALG           TPM_ALG_AES
	#define CONTEXT_ENCRYPT_KEY_BITS      MAX_SYM_KEY_BITS
	#define CONTEXT_ENCRYPT_KEY_BYTES     ((CONTEXT_ENCRYPT_KEY_BITS + 7) / 8)
	#define CONTEXT_INTEGRITY_HASH_ALG    TPM_ALG_SHA256
	#define CONTEXT_INTEGRITY_HASH_SIZE   SHA256_DIGEST_SIZE
	#define PROOF_SIZE                    CONTEXT_INTEGRITY_HASH_SIZE
	#define NV_CLOCK_UPDATE_INTERVAL      12
	#define NUM_POLICY_PCR                1
	#define MAX_COMMAND_SIZE              4096
	#define MAX_RESPONSE_SIZE             4096
	#define ORDERLY_BITS                  8
	#define MAX_ORDERLY_COUNT             ((1 << ORDERLY_BITS) - 1)
	#define ALG_ID_FIRST                  TPM_ALG_FIRST
	#define ALG_ID_LAST                   TPM_ALG_LAST
	#define MAX_SYM_DATA                  128
	#define MAX_RNG_ENTROPY_SIZE          64
	#define RAM_INDEX_SPACE               512
	#define RSA_DEFAULT_PUBLIC_EXPONENT   0x00010001
	#define CRT_FORMAT_RSA                YES
	#define PRIVATE_VENDOR_SPECIFIC_BYTES ((MAX_RSA_KEY_BYTES / 2) * ( 3 + CRT_FORMAT_RSA * 2))
	*/
	public final int NUM_POLICY_PCR_GROUP = 1;
	public final int NUM_AUTHVALUE_PCR_GROUP = 1;
	public final int MAX_CONTEXT_SIZE = 4000;
	public final int MAX_DIGEST_BUFFER = 1024;
	public final int MAX_NV_INDEX_SIZE = 1024;
	public final int MAX_CAP_BUFFER = 1024;
	public final int NV_MEMORY_SIZE = 16384;
	public final int NUM_STATIC_PCR = 16;
	public final int MAX_ALG_LIST_SIZE = 64;
	public final int TIMER_PRESCALE = 100000;
	public final int PRIMARY_SEED_SIZE = 32;
	public final TPM_ALG_ID.ALG_ID CONTEXT_ENCRYPT_ALG = TPM_ALG_ID.ALG_ID.TPM_ALG_AES;
	public final int CONTEXT_ENCRYPT_KEY_BITS = MAX_SYM_KEY_BITS;
	public final int CONTEXT_ENCRYPT_KEY_BYTES = ((CONTEXT_ENCRYPT_KEY_BITS + 7) / 8);
	public final TPM_ALG_ID.ALG_ID CONTEXT_INTEGRITY_HASH_ALG = TPM_ALG_ID.ALG_ID.TPM_ALG_SHA256;
	public final int CONTEXT_INTEGRITY_HASH_SIZE = SHA256_DIGEST_SIZE;
	public final int PROOF_SIZE = CONTEXT_INTEGRITY_HASH_SIZE;
	public final int NV_CLOCK_UPDATE_INTERVAL = 12;
	public final int NUM_POLICY_PCR = 1;
	public final int MAX_COMMAND_SIZE = 4096;
	public final int MAX_RESPONSE_SIZE = 4096;
	public final int ORDERLY_BITS = 8;
	public final int MAX_ORDERLY_COUNT = ((1 << ORDERLY_BITS) - 1);
	public final TPM_ALG_ID.ALG_ID ALG_ID_FIRST = TPM_ALG_ID.ALG_ID.TPM_ALG_FIRST;
	public final TPM_ALG_ID.ALG_ID ALG_ID_LAST = TPM_ALG_ID.ALG_ID.TPM_ALG_LAST;
	public final int MAX_SYM_DATA = 128;
	public final int MAX_RNG_ENTROPY_SIZE = 64;
	public final int RAM_INDEX_SPACE = 512;
	public final int RSA_DEFAULT_PUBLIC_EXPONENT = 0x00010001;
	public final int CRT_FORMAT_RSA = YES;
	public final int PRIVATE_VENDOR_SPECIFIC_BYTES = ((MAX_RSA_KEY_BYTES / 2) * ( 3 + CRT_FORMAT_RSA * 2));
	
}
