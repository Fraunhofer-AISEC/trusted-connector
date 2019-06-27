package de.fhg.aisec.ids.api.internal;

/**
 * This exception is thrown when an internal component, 
 * i.e. an OSGi service or bundle is not available at 
 * runtime.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class ComponentNotAvailableException extends RuntimeException {
  private static final long serialVersionUID = -1195665246923819469L;
  }
