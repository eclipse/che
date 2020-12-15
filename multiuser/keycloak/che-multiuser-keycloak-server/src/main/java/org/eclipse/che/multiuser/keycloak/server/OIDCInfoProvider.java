/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.keycloak.server;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.eclipse.che.multiuser.keycloak.shared.KeycloakConstants.*;
import static org.eclipse.che.multiuser.keycloak.shared.KeycloakConstants.REALM_SETTING;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.proxy.ProxyAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OIDCInfoProvider retrieves OpenID Connect (OIDC) configuration for well-known endpoint. These
 * information is useful to provide access to the Keycloak api.
 */
@Singleton
public class OIDCInfoProvider implements Provider<OIDCInfo> {

  private static final Logger LOG = LoggerFactory.getLogger(OIDCInfoProvider.class);

  private final OIDCInfo oidcInfo;
  private final String realm;
  private final String serverURL;
  private final String serverInternalURL;
  private final String oidcProviderUrl;

  @Inject
  public OIDCInfoProvider(
      @Nullable @Named(AUTH_SERVER_URL_SETTING) String serverURL,
      @Nullable @Named(AUTH_SERVER_URL_INTERNAL_SETTING) String serverInternalURL,
      @Nullable @Named(OIDC_PROVIDER_SETTING) String oidcProviderUrl,
      @Nullable @Named(REALM_SETTING) String realm) {
    this.serverURL = serverURL;
    this.serverInternalURL = serverInternalURL;
    this.oidcProviderUrl = oidcProviderUrl;
    this.realm = realm;
    this.validate();

    String serverAuthUrl = (serverInternalURL != null) ? serverInternalURL : serverURL;
    String wellKnownEndpoint = this.getWellKnownEndpoint(serverAuthUrl);

    LOG.info("Retrieving OpenId configuration from endpoint: {}", wellKnownEndpoint);
    ProxyAuthenticator.initAuthenticator(wellKnownEndpoint);
    try (InputStream inputStream = new URL(wellKnownEndpoint).openStream()) {
      final JsonParser parser = new JsonFactory().createParser(inputStream);
      final TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};

      Map<String, Object> openIdConfiguration =
          new ObjectMapper().reader().readValue(parser, typeReference);

      LOG.info("openid configuration = {}", openIdConfiguration);

      String tokenEndPoint = (String) openIdConfiguration.get("token_endpoint");
      String userInfoEndpoint =
          this.setUserInfoEndpoint((String) openIdConfiguration.get("userinfo_endpoint"));
      String jwksUri = this.setJwksUri((String) openIdConfiguration.get("jwks_uri"));
      String endSessionEndpoint = (String) openIdConfiguration.get("end_session_endpoint");
      this.oidcInfo =
          new OIDCInfo(userInfoEndpoint, tokenEndPoint, jwksUri, endSessionEndpoint, serverAuthUrl);
    } catch (IOException e) {
      throw new RuntimeException(
          "Exception while retrieving OpenId configuration from endpoint: " + wellKnownEndpoint, e);
    } finally {
      ProxyAuthenticator.resetAuthenticator();
    }
  }

  private String getWellKnownEndpoint(String serverAuthUrl) {
    String wellKnownEndpoint = firstNonNull(oidcProviderUrl, serverAuthUrl + "/realms/" + realm);
    if (!wellKnownEndpoint.endsWith("/")) {
      wellKnownEndpoint = wellKnownEndpoint + "/";
    }
    wellKnownEndpoint += ".well-known/openid-configuration";
    return wellKnownEndpoint;
  }

  private void validate() {
    if (serverURL == null && serverInternalURL == null && oidcProviderUrl == null) {
      throw new RuntimeException(
          "Either the '"
              + AUTH_SERVER_URL_SETTING
              + "' or '"
              + AUTH_SERVER_URL_INTERNAL_SETTING
              + "' or '"
              + OIDC_PROVIDER_SETTING
              + "' property should be set");
    }

    if (oidcProviderUrl == null && realm == null) {
      throw new RuntimeException("The '" + REALM_SETTING + "' property should be set");
    }
  }

  private String setUserInfoEndpoint(String userInfoEndpoint) {
    if (serverURL != null && serverInternalURL != null) {
      return userInfoEndpoint.replace(serverURL, serverInternalURL);
    }
    return userInfoEndpoint;
  }

  private String setJwksUri(String jwksUri) {
    if (serverURL != null && serverInternalURL != null) {
      return jwksUri.replace(serverURL, serverInternalURL);
    }
    return jwksUri;
  }

  /** @return OIDCInfo with OIDC settings information. */
  @Override
  public OIDCInfo get() {
    return oidcInfo;
  }
}
