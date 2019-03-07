export interface Route {
  id: string;
  description: string;
  dot: string;
  shortName: string;
  context: string;
  uptime: number;
  status: string;
}

export interface RouteComponent {
  bundle: string;
  description: string;
}
