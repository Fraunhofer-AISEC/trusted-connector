package de.fhg.aisec.ids.api.policy;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.InterceptStrategy;

public class PEP implements InterceptStrategy {
 

    public Processor wrapProcessorInInterceptors(final CamelContext context, final ProcessorDefinition<?> definition,
                                                 final Processor target, final Processor nextTarget) throws Exception {
 
        return new MyProcessor(target);

    }

}