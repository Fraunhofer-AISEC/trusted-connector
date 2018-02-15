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
    user : string;
    name : string;
    namespace : string;
    repository_type : string;
    status : number;
    description : string;
    is_private : boolean;
    is_automated : boolean;
    can_edit : boolean;
    star_count : string;
    pull_count : string;
    last_updated : string;
}