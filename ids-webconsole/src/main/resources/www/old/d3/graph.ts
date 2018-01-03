import { SimulationNodeDatum, SimulationLinkDatum } from "d3-ng2-service";

export class NodeData implements SimulationNodeDatum {
    name: string;
    action: string;
    type: string;

    index?: number;
    x?: number;
    y?: number;
    vx?: number;
    vy?: number;
    fx?: number;
    fy?: number;
}

export class Edge implements SimulationLinkDatum<SimulationNodeDatum> {
    source: string;
    target: string;
}

export class GraphData {
    nodes: NodeData[];
    links: Edge[];
}