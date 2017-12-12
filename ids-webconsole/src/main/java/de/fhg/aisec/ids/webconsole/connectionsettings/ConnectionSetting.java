/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.webconsole.connectionsettings;

public class ConnectionSetting {
	
	String connection;
	Settings settings;

	public ConnectionSetting(String connection, Settings settings) {
		super();
		this.connection = connection;
		this.settings = settings;
	}
	
	public ConnectionSetting(String connection) {
		super();
		this.connection = connection;
		this.settings = new Settings();
	}
	public String getConnection() {
		return connection;
	}
	public void setConnection(String connection) {
		this.connection = connection;
	}
	public Settings getSettings() {
		return settings;
	}
	public void setSettings(Settings settings) {
		this.settings = settings;
	}
	
}
