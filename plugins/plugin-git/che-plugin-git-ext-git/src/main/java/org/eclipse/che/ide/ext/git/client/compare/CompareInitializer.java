/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ext.git.client.compare;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.requirejs.RequireJsLoader;

/** @author Mykola Morhun */
@Singleton
public class CompareInitializer {

  public static final String GIT_COMPARE_MODULE = "Compare";

  private final RequireJsLoader requireJsLoader;

  @Inject
  CompareInitializer(final RequireJsLoader requireJsLoader) {
    this.requireJsLoader = requireJsLoader;
  }

  public Promise<Void> injectCompareWidget(final AsyncCallback<Void> callback) {
    return AsyncPromiseHelper.createFromAsyncRequest(
        call ->
            requireJsLoader.require(
                new Callback<JavaScriptObject[], Throwable>() {
                  @Override
                  public void onFailure(Throwable reason) {
                    callback.onFailure(reason);
                  }

                  @Override
                  public void onSuccess(JavaScriptObject[] result) {
                    callback.onSuccess(null);
                  }
                },
                new String[] {"built-compare/built-compare-amd.min"},
                new String[] {GIT_COMPARE_MODULE}));
  }
}
