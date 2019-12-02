package de.fhg.aisec.ids.idscp2;

/**
 * A Interface for DataAvailableListeners
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public interface DataAvailableListener {

    void onMessage(int len, byte[] bytes);

}
