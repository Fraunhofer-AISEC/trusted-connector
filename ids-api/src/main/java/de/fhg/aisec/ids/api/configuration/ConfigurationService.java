package de.fhg.aisec.ids.api.configuration;

import java.util.Map;

/**
 * The configuration service keeps track of settings of the core platform.
 * 
 * There must not be more than one ConfigurationService in the Core Platform.
 * The first service available will be used, all others will be ignored. This
 * interface does not make any assumptions on how values are persisted.
 * 
 * @author Julian Schütt (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface ConfigurationService {

	/**
	 * Retrieves the value of a given key.
	 * 
	 * May be null if no entry for key exists.
	 * 
	 * @param key
	 * @return
	 */
	public Object get(String key);

	/**
	 * Retrieves the value of a given key or a default value if key does not
	 * exist.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Object get(String key, Object defaultValue);

	/**
	 * Sets a value for a given key.
	 * 
	 * If the key already exists, its value will be overridden by
	 * <code>value</code>. If it does not exist, it will be created.
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, Object value);

	/**
	 * Returns all settings as a key-value map.
	 * @return
	 */
	public Map<String, Object> list();
}
