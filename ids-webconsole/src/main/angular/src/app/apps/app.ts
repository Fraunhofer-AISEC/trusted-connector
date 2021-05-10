import { AppStatus } from './app-status';

export interface App {
    id: string;
    image: string;
    created: Date;
    status: AppStatus;
    names: string;
    size: string;
    uptime: string;
    signature: string;
    owner: string;
    labels: Map<string, any>[];
    ports: string[];

    // Further portainer attributes:
    repository: any;
    type: string;
    name: string;
    hostname: string;
    title: string;
    description: string;
    note: string;
    categories: string[];
    platform: string;
    logo: string;
    registry: string;
    command: string;
    network: string;
    env: Map<string, any>[];
    privileged: boolean;
    interactive: boolean;
    restartPolicy: string;
    volumes: any[];
}

export class AppSearchTerm {
    public searchTerm: string;
}
