import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { Injectable } from '@angular/core';
import { environment } from '../environments/environment';
import { tap } from 'rxjs/operators';

export const HTTP_INJECTION_TOKEN = 'ApplicationHttpClient';
export const HTTP_PROVIDER = {
  provide: HTTP_INJECTION_TOKEN,
  useFactory: applicationHttpClientCreator,
  deps: [HttpClient]
};

export interface IRequestOptions {
  headers?: HttpHeaders | {
    [header: string]: string | Array<string>;
  };
  observe?: 'body';
  params?: HttpParams | {
    [param: string]: string | Array<string>;
  };
  reportProgress?: boolean;
  responseType?: 'json' | 'text';
  withCredentials?: boolean;
}

export interface IRequestOptionsJson extends IRequestOptions {
  responseType?: 'json';
}
export interface IRequestOptionsText extends IRequestOptions {
  responseType?: 'text';
}
export interface IRequestOptionsCached extends IRequestOptions {
  cacheTTL?: number;
}

export interface ApplicationHttpClient {
  /**
   * GET request
   * @param endPoint The endpoint, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  get<T>(endPoint: string, options?: IRequestOptionsCached): Observable<T>;

  /**
   * POST request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param body Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  post<T>(endPoint: string, body: any | null, options?: IRequestOptionsJson): Observable<T>;
  post(endPoint: string, body: any | null, options?: IRequestOptionsText): Observable<string>;

  /**
   * PUT request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param params Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  put<T>(endPoint: string, params: Object, options?: IRequestOptions): Observable<T>;

  /**
   * DELETE request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  delete<T>(endPoint: string, options?: IRequestOptions): Observable<T>;
}

export function applicationHttpClientCreator(client: HttpClient): ApplicationHttpClient {
  return new ApplicationHttpClientImpl(client);
}

@Injectable()
export class ApplicationHttpClientImpl implements ApplicationHttpClient {
  private cache = new Map<string, [number, any]>();

  constructor(private http: HttpClient) { }

  /**
   * GET request
   * @param endPoint The endpoint, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  get(endPoint: string, options?: any): any {
    if (options && options.cacheTTL) {
      const key = endPoint + JSON.stringify(options);
      const val = this.cache.get(key);
      if (val && val[0] > Date.now() - options.cacheTTL * 1e3) {
        return Observable.of(val[1]);
      } else {
        return this.http.get(environment.apiURL + endPoint, options)
          .pipe(
            tap(res => this.cache.set(key, [Date.now(), res]))
          );
      }
    }

    return this.http.get(environment.apiURL + endPoint, options);
  }

  /**
   * POST request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param body Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  post(endPoint: string, body: any | null, options?: any): any {
    return this.http.post(environment.apiURL + endPoint, body, options);
  }

  /**
   * PUT request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param params Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  put(endPoint: string, params: Object, options?: any): any {
    return this.http.put(environment.apiURL + endPoint, params, options);
  }

  /**
   * DELETE request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  delete<T>(endPoint: string, options?: any): any {
    return this.http.delete<T>(environment.apiURL + endPoint, options);
  }
}
