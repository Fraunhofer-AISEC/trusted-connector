package de.fhg.aisec.ids.api.policy;

import java.io.IOException;
import java.io.InputStream;
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
    private Properties properties = null;
    private String dropword;
    
    
    public MyProcessor(Processor target) {
    	this.target = target;
    	
    	//for testing: if this string is in the filename, we won't forward the file
    	dropword = "aaa";
//    	if (properties == null) {
//    		properties = new Properties();
//    		InputStream input; 
//    	
//    		try {
//				input = getClass().getClassLoader().getResourceAsStream("rules.properties");
//				properties.load(input);
//				dropword = properties.getProperty("drop");
//				LOG.info("Loaded properties from rules.properties");
//    		} catch (IOException e) {
//				e.printStackTrace();
//			}
//    	}
    	
    	
    }

	public void process(Exchange exchange) throws Exception {
		
		String filename = exchange.getIn().getHeader("CamelFileName").toString();

//		System.out.println("Exchange properties: "+exchange.getProperties());
		
        if (filename.contains(dropword)) {
        	LOG.warn("Dropping message " + filename + " - dropword " + dropword + " detected...");
        } else {
        	LOG.debug("Forwarding message " + filename + " - no dropword '" + dropword + "' detected...");
        	target.process(exchange);
        }
        
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
