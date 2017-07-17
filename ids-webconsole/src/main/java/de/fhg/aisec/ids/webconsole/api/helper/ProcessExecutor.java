/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
