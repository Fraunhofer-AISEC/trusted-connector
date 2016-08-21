package de.fhg.aisec.ids.cm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamGobbler extends Thread {
    InputStream is;
    OutputStream out;
    
    // reads everything from is until empty. 
    public StreamGobbler(InputStream is, OutputStream out) {
        this.is = is;
        this.out = out;
    }

    public void run() {
        try {
            copy(is, out);
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
    
    private static void copy(InputStream in, OutputStream out) throws IOException {
        while (true) {
          int c = in.read();
          if (c == -1) break;
          out.write((char)c);
        }
      }
    
    public void close() {
    	try {
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
}