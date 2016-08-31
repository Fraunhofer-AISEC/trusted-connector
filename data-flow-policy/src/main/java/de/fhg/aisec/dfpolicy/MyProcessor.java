package de.fhg.aisec.dfpolicy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyProcessor implements AsyncProcessor {
	
    private static final Logger LOG = LoggerFactory.getLogger(MyProcessor.class);  
    private Processor target;
    
    
    public MyProcessor(Processor target) {
    	this.target = target;
    	loadRules("rules");
    	
    	
    }
    
    private void loadRules(String rulefile) {
    	FileInputStream fileinputstream;
    	// Get the object of DataInputStream
    	DataInputStream datainputstream;
    	BufferedReader bufferedreader;
    	String line;
    	
    	try {
    		fileinputstream =  new FileInputStream(rulefile);
    		datainputstream = new DataInputStream(fileinputstream);
    		bufferedreader = new BufferedReader(new InputStreamReader(datainputstream));
    		
    		while ((line = bufferedreader.readLine()) != null)   {
    			System.out.println(line);
    		}
    		datainputstream.close();
    	}catch (Exception e){//Catch exception if any
    		  LOG.error("Error while loading rulefile: " + e.getMessage());
    	}
    }

	public void process(Exchange exchange) throws Exception {
		
		String from = exchange.getFromEndpoint().getEndpointUri();
		System.out.println("Received a message from " +from+ "...");
    }

    @Override
    public String toString() {
      return "MyProcessor[" + "]";
    }

	@Override
	public boolean process(Exchange exchange, AsyncCallback ac) {
		try {
			process(exchange);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
 
}
