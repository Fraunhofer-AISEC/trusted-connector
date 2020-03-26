import { Route } from './routes/route';

export class Result {
  public successful: boolean;
  public message: string;
}

export class RouteResult extends Result {
  public route?: Route;
}
