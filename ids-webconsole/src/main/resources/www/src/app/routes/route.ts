import { GraphData } from "../d3/graph";

export class Route {
  id: string;
  description: string;
  dot: string;
  graph: GraphData;
  shortName: string;
  context: string;
  uptime: number;
  status: string;
}
