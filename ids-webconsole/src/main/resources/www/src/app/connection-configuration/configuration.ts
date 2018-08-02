import { Settings } from './settings.interface';

export class Configuration {
    connection: string;
    settings: Settings;
    dirty = false;

    constructor(connection: string, settings?: Settings) {
        this.connection = connection;
        if (settings) {
            this.settings = settings;
        } else {
            this.settings = {
                integrityProtectionAndVerification: '1',
                authentication: '1',
                serviceIsolation: '1',
                integrityProtectionVerificationScope: '1',
                appExecutionResources: '1',
                dataUsageControlSupport: '1',
                auditLogging: '1',
                localDataConfidentiality: '1'
            };
        }
    }
}
