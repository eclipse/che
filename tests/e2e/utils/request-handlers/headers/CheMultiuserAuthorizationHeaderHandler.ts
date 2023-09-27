/** *******************************************************************
 * copyright (c) 2019-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { AxiosRequestConfig } from 'axios';
import { IAuthorizationHeaderHandler } from './IAuthorizationHeaderHandler';
import { inject, injectable } from 'inversify';
import { DriverHelper } from '../../DriverHelper';
import { CLASSES } from '../../../configs/inversify.types';
import { Logger } from '../../Logger';
import { IWebDriverCookie } from 'selenium-webdriver';
import { BASE_TEST_CONSTANTS, Platform } from '../../../constants/BASE_TEST_CONSTANTS';

@injectable()
export class CheMultiuserAuthorizationHeaderHandler implements IAuthorizationHeaderHandler {
	private authorizationToken: string = '';
	private readonly cookiesType: string = BASE_TEST_CONSTANTS.TS_PLATFORM === Platform.OPENSHIFT ? '_oauth_proxy' : '_oauth2_proxy';

	constructor(
		@inject(CLASSES.DriverHelper)
		private readonly driverHelper: DriverHelper
	) {}

	async get(): Promise<AxiosRequestConfig> {
		Logger.debug();
		try {
			const token: IWebDriverCookie = await this.driverHelper.getDriver().manage().getCookie(this.cookiesType);
			if (this.authorizationToken !== token.value) {
				this.authorizationToken = token.value;
			}
		} catch (err) {
			if (this.authorizationToken.length > 0) {
				Logger.warn(
					'Could not obtain _oauth_proxy cookie from chromedriver, browser session may have been killed. Using stored value.'
				);
			} else {
				throw new Error(
					'Could not obtain _oauth_proxy cookie from chromedriver, browser session may have been killed. No stored token present!'
				);
			}
		}

		return {
			headers: {
				Cookie: `${this.cookiesType}=${this.authorizationToken}`
			}
		};
	}
}
