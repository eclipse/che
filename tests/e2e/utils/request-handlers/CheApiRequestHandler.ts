/** *******************************************************************
 * copyright (c) 2019-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import axios, { AxiosResponse, InternalAxiosRequestConfig } from 'axios';
/* import * as util from 'util'; */ // allows to print circular reference JSONObjects
import { TYPES } from '../../configs/inversify.types';
import { inject, injectable } from 'inversify';
import { IAuthorizationHeaderHandler } from './headers/IAuthorizationHeaderHandler';
import { Logger } from '../Logger';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';

@injectable()
export class CheApiRequestHandler {
	constructor(
		@inject(TYPES.IAuthorizationHeaderHandler)
		private readonly headerHandler: IAuthorizationHeaderHandler
	) {}

	/**
	 * this method adds a request interceptor into axios request interceptors list and returns an ID of the interceptor
	 */
	static enableRequestInterceptor(): number {
		Logger.debug();
		return axios.interceptors.request.use((request: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
			try {
				/* logger.warn(`before parse:\n${util.inspect(request)}`); */ // allows to print circular reference JSONObjects
				const REQUEST_CENSORED: InternalAxiosRequestConfig = JSON.parse(JSON.stringify(request));
				if (REQUEST_CENSORED === undefined) {
					Logger.error('JSON.parse returned an undefined object, cannot process request');
					return request;
				}
				if (REQUEST_CENSORED.headers === undefined) {
					Logger.warn('request does not contain any headers object');
					return request;
				}
				REQUEST_CENSORED.headers.Cookie = 'CENSORED';
				Logger.info('request:\n' + JSON.stringify(REQUEST_CENSORED));
			} catch (err) {
				Logger.error('request: Failed to deep clone AxiosRequestConfig:' + err);
			}
			return request;
		});
	}

	/**
	 * this method adds a response interceptor into axios response interceptors list and returns an ID of the interceptor
	 */
	static enableResponseInterceptor(): number {
		Logger.debug();
		return axios.interceptors.response.use((response: AxiosResponse): AxiosResponse => {
			try {
				/* Logger.warn(`before parse:\n${util.inspect(response)}`); */ // allows to print circular reference JSONObjects
				const RESPONSE_CENSORED: AxiosResponse = JSON.parse(
					JSON.stringify(response, (key, value: string): string => {
						switch (key) {
							case 'request':
								return 'CENSORED';
							default:
								return value;
						}
					})
				);
				if (RESPONSE_CENSORED === undefined) {
					Logger.error('JSON.parse returned an undefined object, cannot process response');
					return response;
				}
				if (RESPONSE_CENSORED.config === undefined) {
					Logger.warn('response does not contain any config object');
					return response;
				}
				if (RESPONSE_CENSORED.config.headers === undefined) {
					Logger.warn('response does not contain any config.headers object');
					return response;
				}
				RESPONSE_CENSORED.config.headers.Cookie = 'CENSORED';
				Logger.info('response:\n' + JSON.stringify(RESPONSE_CENSORED));
			} catch (err) {
				Logger.error('response: Failed to deep clone AxiosResponse:' + err);
			}
			return response;
		});
	}

	async get(relativeUrl: string): Promise<AxiosResponse> {
		return await axios.get(this.assembleUrl(relativeUrl), await this.headerHandler.get());
	}

	async post(relativeUrl: string, data?: string): Promise<AxiosResponse> {
		return await axios.post(this.assembleUrl(relativeUrl), data, await this.headerHandler.get());
	}

	async delete(relativeUrl: string): Promise<AxiosResponse> {
		return await axios.delete(this.assembleUrl(relativeUrl), await this.headerHandler.get());
	}

	async patch(relativeUrl: string, patchParams: object): Promise<AxiosResponse> {
		return await axios.patch(this.assembleUrl(relativeUrl), patchParams, await this.headerHandler.get());
	}

	private assembleUrl(relativeUrl: string): string {
		return `${BASE_TEST_CONSTANTS.TS_SELENIUM_BASE_URL}/${relativeUrl}`;
	}
}
