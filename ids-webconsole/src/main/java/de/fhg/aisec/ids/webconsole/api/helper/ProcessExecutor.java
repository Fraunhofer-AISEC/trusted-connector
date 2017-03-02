package de.fhg.aisec.ids.webconsole.api.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ProcessExecutor {
	
	public int execute(String[] cmd, OutputStream stdout, OutputStream stderr) throws InterruptedException, IOException {
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(cmd);

		StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), stderr);
		StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), stdout);

		errorGobbler.start();
		outputGobbler.start();

		return proc.waitFor();
	}

}

class StreamGobbler extends Thread {
	InputStream is;
	OutputStream out;

	@SuppressWarnings("unused")
	private StreamGobbler() {
		// Do not call me
	}
	
	StreamGobbler(InputStream is, OutputStream out) {
		this.is = is;
		this.out = out;
	}

	@Override
	public void run() {
		try (	InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr); ) {			
			BufferedWriter bw = null; 
			if (out!=null) {
				bw = new BufferedWriter(new OutputStreamWriter(out));
			}
			String line;
			while ((line = br.readLine()) != null) {
				if (bw!=null) {
					bw.write(line);
					bw.newLine();
				}
			}
			if (bw!=null) {
				bw.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}