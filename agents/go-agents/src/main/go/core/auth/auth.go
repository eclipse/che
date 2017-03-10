//
// Copyright (c) 2012-2017 Codenvy, S.A.
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// which accompanies this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html
//
// Contributors:
//   Codenvy, S.A. - initial API and implementation
//

// Package auth provides simple way of authentication of http requests on workspace master
package auth

import (
	"errors"
	"fmt"
	"net/http"

	"github.com/eclipse/che/agents/go-agents/src/main/go/core/rest"
)

// TokenCache represents authentication tokens cache
type TokenCache interface {
	// Put adds token into the cache.
	Put(token string)
	// Expire removes provided token from the cache.
	Expire(token string)
	// Contains returns true if token is present in the cache and false otherwise.
	Contains(token string) bool
}

// UnauthorizedHandler handles request when authentication failed
type UnauthorizedHandler func(w http.ResponseWriter, r *http.Request, err error)

type handler struct {
	delegate            http.Handler
	apiEndpoint         string
	unauthorizedHandler UnauthorizedHandler
}

type cachingHandler struct {
	delegate            http.Handler
	apiEndpoint         string
	cache               TokenCache
	unauthorizedHandler UnauthorizedHandler
}

// NewHandler creates HTTP handler that authenticates all the http calls on workspace master.
// Checks on workspace master if provided by request token is valid and calls ServerHTTP on delegate.
// Otherwise if UnauthorizedHandler is configured calls ServerHTTP on it.
// If it is not configured returns 401 with appropriate error message.
func NewHandler(delegate http.Handler, apiEndpoint string, unauthorizedHandler UnauthorizedHandler) http.Handler {
	if unauthorizedHandler == nil {
		unauthorizedHandler = defaultUnauthorizedHandler
	}
	return &handler{
		delegate:            delegate,
		apiEndpoint:         apiEndpoint,
		unauthorizedHandler: unauthorizedHandler,
	}
}

// NewCachingHandler creates HTTP handler that authenticates all the http calls on workspace master.
// Checks on workspace master if provided by request token is valid and calls ServerHTTP on delegate.
// Otherwise if UnauthorizedHandler is configured calls ServerHTTP on it.
// If it is not configured returns 401 with appropriate error message.
// This implementation caches the results of authentication to speedup request handling.
func NewCachingHandler(delegate http.Handler, apiEndpoint string, unauthorizedHandler UnauthorizedHandler, cache TokenCache) http.Handler {
	if cache == nil {
		panic("TokenCache argument of NewCachingHandler required")
	}
	if unauthorizedHandler == nil {
		unauthorizedHandler = defaultUnauthorizedHandler
	}
	return &cachingHandler{
		delegate:            delegate,
		apiEndpoint:         apiEndpoint,
		cache:               cache,
		unauthorizedHandler: unauthorizedHandler,
	}
}

func (handler handler) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	token := req.URL.Query().Get("token")
	if err := authenticateOnMaster(handler.apiEndpoint, token); err == nil {
		handler.delegate.ServeHTTP(w, req)
	} else {
		handler.unauthorizedHandler(w, req, err)
	}
}

func (handler cachingHandler) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	token := req.URL.Query().Get("token")
	if handler.cache.Contains(token) {
		handler.delegate.ServeHTTP(w, req)
	} else if err := authenticateOnMaster(handler.apiEndpoint, token); err == nil {
		handler.cache.Put(token)
		handler.delegate.ServeHTTP(w, req)
	} else {
		handler.unauthorizedHandler(w, req, err)
	}
}

func authenticateOnMaster(apiEndpoint string, tokenParam string) error {
	if tokenParam == "" {
		return rest.Unauthorized(errors.New("Authentication failed: missing 'token' query parameter"))
	}
	req, err := http.NewRequest("GET", apiEndpoint+"/machine/token/user/"+tokenParam, nil)
	if err != nil {
		return rest.Unauthorized(err)
	}
	req.Header.Add("Authorization", tokenParam)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return rest.Unauthorized(err)
	}
	if resp.StatusCode != 200 {
		return rest.Unauthorized(fmt.Errorf("Authentication failed, token: %s is invalid", tokenParam))
	}
	return nil
}

func defaultUnauthorizedHandler(w http.ResponseWriter, r *http.Request, err error) {
	http.Error(w, err.Error(), http.StatusUnauthorized)
}
