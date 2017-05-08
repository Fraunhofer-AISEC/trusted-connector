package de.fhg.aisec.ids.rm;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.InterceptStrategy;

/**
 * 
 * @author Mathias Morbitzer (mathias.morbitzer@aisec.fraunhofer.de)
 */

public class CamelInterceptor implements InterceptStrategy {
	private RouteManagerService rm;

	public CamelInterceptor(RouteManagerService rm) {
		this.rm = rm;
	}
	
	@Override
    public Processor wrapProcessorInInterceptors(final CamelContext context, final ProcessorDefinition<?> definition,
                                                 final Processor target, final Processor nextTarget) throws Exception {
 
        return new PolicyEnforcementPoint(target, this.rm);
    }

}