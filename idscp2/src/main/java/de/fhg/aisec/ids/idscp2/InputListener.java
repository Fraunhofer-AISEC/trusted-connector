package de.fhg.aisec.ids.idscp2;

/**
 * A event source interface for DataAvailableListener pattern
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface InputListener {

    void register(DataAvailableListener listener);

    void unregister(DataAvailableListener listener);

    void safeStop();
}
