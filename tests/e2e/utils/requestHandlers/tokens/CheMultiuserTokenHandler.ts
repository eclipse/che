/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import axios from 'axios';
import querystring from 'querystring';
import { injectable } from 'inversify';
import { TestConstants } from '../../../TestConstants';
import { ITokenHandler } from './ITokenHandler';
import https from 'https';

@injectable()
export class CheMultiuserTokenHandler implements ITokenHandler {
    async get(): Promise<string> {
        let params = {};

        let keycloakUrl = TestConstants.TS_SELENIUM_BASE_URL;
        const keycloakAuthSuffix = '/auth/realms/che/protocol/openid-connect/token';
        keycloakUrl = keycloakUrl.replace('che', 'keycloak') + keycloakAuthSuffix;
        params = {
            client_id: 'che-public',
            username: TestConstants.TS_SELENIUM_USERNAME,
            password: TestConstants.TS_SELENIUM_PASSWORD,
            grant_type: 'password'
        };

        try {
            const responseToObtainBearerToken = await axios.post(keycloakUrl, querystring.stringify(params), {httpsAgent: new https.Agent({ rejectUnauthorized: false })});
            return responseToObtainBearerToken.data.access_token;
        } catch (err) {
            console.log(`Can not get bearer token. URL used: ${keycloakUrl}`);
            throw err;
        }

    }
}


