export interface App {
    id: string;
    image: string;
    created: Date;
    status: string;
    names: string;
    size: string;
    uptime: string;
    signature: string;
    owner: string;
    labels: Array<Map<string, any>>;
    ports: Array<string>;

    // Further portainer attributes:
    repository: any;
    type: string;
    name: string;
    hostname: string;
    title: string;
    description: string;
    note: string;
    categories: Array<string>;
    platform: string;
    logo: string;
    registry: string;
    command: string;
    network: string;
    env: Array<Map<string, any>>;
    privileged: boolean;
    interactive: boolean;
    restartPolicy: string;
    volumes: Array<any>;
}

export class AppSearchTerm {
    public searchTerm: string;
}
