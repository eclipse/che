/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.factory.ide.configure;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.factory.FactoryServiceClient;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.plugin.factory.ide.FactoryLocalizationConstant;

import java.util.Collections;
import java.util.List;

/**
 * @author Anton Korneta
 */
@Singleton
public class CreateFactoryPresenter implements CreateFactoryView.ActionDelegate {
    public static final String CONFIGURE_LINK = "/dashboard/#/factory/";

    private final CreateFactoryView           view;
    private final AppContext                  appContext;
    private final FactoryServiceClient        factoryService;
    private final FactoryLocalizationConstant locale;

    @Inject
    public CreateFactoryPresenter(CreateFactoryView view,
                                  AppContext appContext,
                                  FactoryServiceClient factoryService,
                                  FactoryLocalizationConstant locale) {
        this.view = view;
        this.appContext = appContext;
        this.factoryService = factoryService;
        this.locale = locale;
        view.setDelegate(this);
    }

    public void showDialog() {
        view.showDialog();
    }

    @Override
    public void onCreateClicked() {
        final String factoryName = view.getFactoryName();
        factoryService.getFactoryJson(appContext.getWorkspace().getId(), null)
                      .then(new Operation<FactoryDto>() {
                          @Override
                          public void apply(final FactoryDto factory) throws OperationException {
                              factoryService.findFactory(null, null, Collections.singletonList(Pair.of("name", factoryName)))
                                            .then(saveFactory(factory, factoryName))
                                            .catchError(logError());
                          }
                      })
                      .catchError(logError());
    }

    @Override
    public void onFactoryNameChanged(String factoryName) {
        view.enableCreateFactoryButton(isValidFactoryName(factoryName));
    }

    @Override
    public void onCancelClicked() {
        view.close();
    }

    private Operation<List<FactoryDto>> saveFactory(final FactoryDto factory, final String factoryName) {
        return new Operation<List<FactoryDto>>() {
            @Override
            public void apply(List<FactoryDto> factories) throws OperationException {
                if (!factories.isEmpty()) {
                    view.showFactoryNameError(locale.createFactoryAlreadyExist(), null);
                } else {
                    factoryService.saveFactory(factory.withName(factoryName))
                                  .then(new Operation<FactoryDto>() {
                                      @Override
                                      public void apply(FactoryDto factory) throws OperationException {
                                          final Link link = factory.getLink("accept-named");
                                          if (link != null) {
                                              view.setAcceptFactoryLink(link.getHref());
                                          }
                                          view.setConfigureFactoryLink(CONFIGURE_LINK + factory.getId() + "/configure");
                                      }
                                  })
                                  .catchError(logError());
                }
            }
        };
    }

    private Operation<PromiseError> logError() {
        return err -> view.showFactoryNameError(locale.createFactoryFromCurrentWorkspaceFailed(), err.getMessage());
    }

    private boolean isValidFactoryName(String name) {
        if (name.length() == 0 || name.length() >= 125) {
            return false;
        }
        view.hideFactoryNameError();
        return true;
    }
}
