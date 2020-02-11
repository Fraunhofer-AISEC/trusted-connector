package de.fhg.aisec.ids.comm;

import de.fhg.aisec.ids.api.tokenm.DatException;

@FunctionalInterface
public interface DatVerifier {
  void verify(String dat) throws DatException;
}
