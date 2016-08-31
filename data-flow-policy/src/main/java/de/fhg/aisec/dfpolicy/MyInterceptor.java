package de.fhg.aisec.dfpolicy;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.InterceptStrategy;

/**
 * TODO Implement me!
 * 
 * @author Mathias Morbitzer (mathias.morbitzer@aisec.fraunhofer.de)
 */
public class MyInterceptor implements InterceptStrategy {

	@Override
	public Processor wrapProcessorInInterceptors(CamelContext arg0, ProcessorDefinition<?> arg1, Processor arg2,
			Processor arg3) throws Exception {
		// TODO Implement me!
		return null;
	}

}
