import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';

import { environment } from '../environments/environment';

export interface RequestOptions {
  headers?: HttpHeaders | {
    [header: string]: string | string[];
  };
  observe?: 'body';
  params?: HttpParams | {
    [param: string]: string | string[];
  };
  reportProgress?: boolean;
  responseType?: 'json' | 'text';
  withCredentials?: boolean;
}

export interface RequestOptionsJson extends RequestOptions {
  responseType?: 'json';
}
export interface RequestOptionsText extends RequestOptions {
  responseType?: 'text';
}
export interface RequestOptionsCached extends RequestOptions {
  cacheTTL?: number;
}

export interface ApplicationHttpClient {
  /**
   * GET request
   * @param endPoint The endpoint, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  get<T>(endPoint: string, options?: RequestOptionsCached): Observable<T>;

  /**
   * POST request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param body Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  post<T>(endPoint: string, body: any | null, options?: RequestOptionsJson): Observable<T>;
  post(endPoint: string, body: any | null, options?: RequestOptionsText): Observable<string>;

  /**
   * PUT request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param params Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  put<T>(endPoint: string, params: object, options?: RequestOptions): Observable<T>;

  /**
   * DELETE request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  delete<T>(endPoint: string, options?: RequestOptions): Observable<T>;
}

@Injectable()
export class ApplicationHttpClientImpl implements ApplicationHttpClient {
  private readonly cache = new Map<string, [number, any]>();

  constructor(private readonly http: HttpClient) { }

  /**
   * GET request
   * @param endPoint The endpoint, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  public get(endPoint: string, options?: any): any {
    if (options && options.cacheTTL) {
      const key = endPoint + JSON.stringify(options);
      const val = this.cache.get(key);
      if (val && val[0] > Date.now() - options.cacheTTL * 1e3) {
        return of(val[1]);
      }

      return this.http.get(environment.apiURL + endPoint, options)
        .pipe(
          tap(res => this.cache.set(key, [Date.now(), res]))
        );
    }

    return this.http.get(environment.apiURL + endPoint, options);
  }

  /**
   * POST request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param body Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  public post(endPoint: string, body: any | null, options?: any): any {
    return this.http.post(environment.apiURL + endPoint, body, options);
  }

  /**
   * PUT request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param params Body of the request
   * @param options Options of the request like headers, body, etc.
   */
  public put(endPoint: string, params: object, options?: any): any {
    return this.http.put(environment.apiURL + endPoint, params, options);
  }

  /**
   * DELETE request
   * @param endPoint The endpoint at the API, starting with a slash
   * @param options Options of the request like headers, body, etc.
   */
  public delete<T>(endPoint: string, options?: any): any {
    return this.http.delete<T>(environment.apiURL + endPoint, options);
  }
}

export const applicationHttpClientCreator =
  (client: HttpClient): ApplicationHttpClient => new ApplicationHttpClientImpl(client);
export const HTTP_INJECTION_TOKEN = 'ApplicationHttpClient';
export const HTTP_PROVIDER = {
  provide: HTTP_INJECTION_TOKEN,
  useFactory: applicationHttpClientCreator,
  deps: [HttpClient]
};
