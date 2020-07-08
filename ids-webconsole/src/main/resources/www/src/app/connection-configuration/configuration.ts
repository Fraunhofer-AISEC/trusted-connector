import { Settings } from './settings.interface';

export class Configuration {
    public connection: string;
    public settings: Settings;
    public dirty = false;

    constructor(connection: string, settings?: Settings) {
        this.connection = connection;
        if (settings) {
            this.settings = settings;
        } else {
            this.settings = {
                securityProfile: 'idsc:BASE_SECURITY_PROFILE'
            };
        }
    }
}
