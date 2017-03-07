package de.fhg.ids.comm.ws.protocol.rat;

import java.util.Random;

public class NonceGenerator {
	// generate a new software nonce in sha-1
	public static String generate(int numchars) {
		Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while(sb.length() < numchars){
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars);
	} 

}
