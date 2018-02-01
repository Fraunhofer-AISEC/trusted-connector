import { Route } from 'app/routes/route';

export class Result {
  successful: boolean;
  message: string;
}

export class RouteResult extends Result {
  route?: Route;
}
