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
    labels: Array<Array<string>>;
    ports: string;
}

export interface DockerHubApp {
    user: string;
    name: string;
    namespace: string;
    repository_type: string;
    status: number;
    description: string;
    is_private: boolean;
    is_automated: boolean;
    can_edit: boolean;
    star_count: string;
    pull_count: string;
    last_updated: string;
    tags?: Array<DockerHubTag>;
}

export interface DockerHubTag {
    name: string;
    full_size: number;
    id: number;
    repository: number;
    creator: number;
    last_updater: number;
    last_updated: string;
    image_id: any;
    images: any;
    v2: boolean;
}
