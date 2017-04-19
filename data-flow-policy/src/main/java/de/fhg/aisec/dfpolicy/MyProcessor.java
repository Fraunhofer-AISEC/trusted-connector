package de.fhg.aisec.dfpolicy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.management.InstrumentationProcessor;
import org.apache.camel.processor.SendProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyProcessor implements AsyncProcessor {
	
    private static final Logger LOG = LoggerFactory.getLogger(MyProcessor.class);  
    private Processor target;
    private final PDP pdp;
    
    
    public MyProcessor(Processor target) {
    	this.target = target;
    	this.pdp = PDP.getInstance();
    }
        
    public void process(Exchange exchange) throws Exception {
    	// Check if environment is usable as expected
    	if (target==null || exchange==null || !(target instanceof InstrumentationProcessor)) {
			LOG.warn("Cannot check data flow policy. Null or no InstrumentationProcessor");
			return;
		}
		
    	// We expect a SendProcessor to retrieve the endpoint URL from
		if ( !(((InstrumentationProcessor) target).getProcessor() instanceof SendProcessor) ) {
			LOG.warn("Not a SendProcessor. Skipping");
			return;
		}
    	
    	LOG.info("Start 'process' with endpoint ..." + exchange.getFromEndpoint().getEndpointUri());		
		
		// get labels from Exchange property
		String exchangeLabelsRaw = (exchange.getProperty("labels") == null) ? "" : exchange.getProperty("labels").toString();
		String[] labelArray = exchangeLabelsRaw.split(",");
		Set<String> exchangeLabels = new HashSet<String>(Arrays.asList(labelArray));
		
		//figure out where the message comes from and where it should go to
		SendProcessor sendprocessor = (SendProcessor) ((InstrumentationProcessor) target).getProcessor();
		String destination = sendprocessor.getEndpoint().getEndpointUri();
		String source = exchange.getFromEndpoint().getEndpointUri();
		
		LOG.info("********************************************");
		LOG.info("START: Check if label(s) exist in allow rules for destination " + destination);
		
		// Call PDP to transform labels and decide whether we may forward the Exchange
		boolean isAllowed = pdp.decide(source, destination, exchangeLabels);

		LOG.info("STOP: Check if label(s) exist in allow rules.");
		LOG.info("********************************************");

		if (isAllowed) {
			LOG.info("Message with labels  '" + exchangeLabelsRaw +"' has all required labels for destination '" + destination + "', forwarding...");

			// store labels in message body
			/* TODO COMMENTED OUT BY JULIAN. The rules should determine whether labels should be persisted in body
			String body = exchange.getIn().getBody().toString();
			if (body.startsWith("Labels: ") && body.contains("\n\n")) {
				body = body.substring(body.indexOf("\n\n") + "\n\n".length(), body.length() - 1 );
			}
			exchange.getIn().setBody("Labels: " + exchangeLabelsRaw + "\n\n" + body); */
					
			// forward the Exchange
			target.process(exchange);
		}
		
		LOG.info("Stop 'process' with endpoint ..." + exchange.getFromEndpoint().getEndpointUri());
    }
	
	@Override
	public boolean process(Exchange exchange, AsyncCallback ac) {
		try {
			process(exchange);
		}catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return true;
	}
}
