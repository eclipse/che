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
package org.eclipse.che.plugin.pullrequest.client;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.api.constraints.Constraints.FIRST;
import static org.eclipse.che.ide.api.parts.PartStackType.TOOLING;
import static org.eclipse.che.plugin.pullrequest.shared.ContributionProjectTypeConstants.CONTRIBUTE_TO_BRANCH_VARIABLE_NAME;
import static org.eclipse.che.plugin.pullrequest.shared.ContributionProjectTypeConstants.CONTRIBUTION_PROJECT_TYPE_ID;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.factory.FactoryAcceptedEvent;
import org.eclipse.che.ide.api.factory.FactoryAcceptedHandler;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.selection.SelectionChangedEvent;
import org.eclipse.che.ide.api.selection.SelectionChangedHandler;
import org.eclipse.che.ide.api.workspace.WorkspaceReadyEvent;
import org.eclipse.che.plugin.pullrequest.client.parts.contribute.ContributePartPresenter;
import org.eclipse.che.plugin.pullrequest.client.vcs.VcsService;
import org.eclipse.che.plugin.pullrequest.client.vcs.VcsServiceProvider;
import org.eclipse.che.plugin.pullrequest.client.vcs.hosting.VcsHostingService;
import org.eclipse.che.plugin.pullrequest.client.vcs.hosting.VcsHostingServiceProvider;
import org.eclipse.che.plugin.pullrequest.client.workflow.Context;
import org.eclipse.che.plugin.pullrequest.client.workflow.WorkflowExecutor;

/**
 * Responsible for setting up contribution mixin for the currently selected project in application
 * context.
 *
 * @author Vlad Zhukovskyi
 * @since 5.0.0
 */
@Singleton
public class ContributionMixinProvider {

  private final EventBus eventBus;
  private final AppContext appContext;
  private final WorkspaceAgent workspaceAgent;
  private final ContributePartPresenter contributePart;
  private final WorkflowExecutor workflowExecutor;
  private final VcsServiceProvider vcsServiceProvider;
  private final VcsHostingServiceProvider vcsHostingServiceProvider;
  private final PromiseProvider promiseProvider;
  private final ContributeMessages contributeMessages;

  private HandlerRegistration handlerRegistration;

  private Project lastSelected;

  @Inject
  public ContributionMixinProvider(
          EventBus eventBus,
          AppContext appContext,
          WorkspaceAgent workspaceAgent,
          ContributePartPresenter contributePart,
          WorkflowExecutor workflowExecutor,
          VcsServiceProvider vcsServiceProvider,
          VcsHostingServiceProvider vcsHostingServiceProvider,
          PromiseProvider promiseProvider,
          ContributeMessages contributeMessages) {
    this.eventBus = eventBus;
    this.appContext = appContext;
    this.workspaceAgent = workspaceAgent;
    this.contributePart = contributePart;
    this.workflowExecutor = workflowExecutor;
    this.vcsServiceProvider = vcsServiceProvider;
    this.vcsHostingServiceProvider = vcsHostingServiceProvider;
    this.promiseProvider = promiseProvider;
    this.contributeMessages = contributeMessages;

    if (appContext.getFactory() != null) {
      handlerRegistration =
          eventBus.addHandler(
              FactoryAcceptedEvent.TYPE,
              new FactoryAcceptedHandler() {
                @Override
                public void onFactoryAccepted(FactoryAcceptedEvent event) {
                  handlerRegistration.removeHandler();

                  subscribeToSelectionChangedEvent();
                }
              });
    } else {
      handlerRegistration =
          eventBus.addHandler(
              WorkspaceReadyEvent.getType(),
              new WorkspaceReadyEvent.WorkspaceReadyHandler() {
                @Override
                public void onWorkspaceReady(WorkspaceReadyEvent event) {
                  handlerRegistration.removeHandler();

                  subscribeToSelectionChangedEvent();
                }
              });
    }
  }

  private void subscribeToSelectionChangedEvent() {
    eventBus.addHandler(
        SelectionChangedEvent.TYPE,
        new SelectionChangedHandler() {
          @Override
          public void onSelectionChanged(SelectionChangedEvent event) {
            processCurrentProject();
          }
        });
  }

  private void processCurrentProject() {
    final Project rootProject = appContext.getRootProject();

    if (lastSelected != null && lastSelected.equals(rootProject)) {
      return;
    }

    final PartStack toolingPartStack = workspaceAgent.getPartStack(TOOLING);

    if (rootProject == null) {
      if (toolingPartStack.containsPart(contributePart)) {
        invalidateContext(lastSelected);
        contributePart.showStub(contributeMessages.stubTextProjectNotProvideVSC()); // todo
      }
    } else if (hasVcsService(rootProject)) {
      // todo maybe not here...
      contributePart.hideStub();
      if (hasContributionMixin(rootProject)) {

        vcsHostingServiceProvider
            .getVcsHostingService(rootProject)
            .then(
                new Operation<VcsHostingService>() {
                  @Override
                  public void apply(VcsHostingService vcsHostingService) throws OperationException {
                    workflowExecutor.init(vcsHostingService, rootProject);
                    addPart(toolingPartStack);
                  }
                });
      } else {
        vcsHostingServiceProvider
            .getVcsHostingService(rootProject)
            .then(
                new Operation<VcsHostingService>() {
                  @Override
                  public void apply(final VcsHostingService vcsHostingService)
                      throws OperationException {
                    addMixin(rootProject)
                        .then(
                            new Operation<Project>() {
                              @Override
                              public void apply(Project project) throws OperationException {
                                workflowExecutor.init(vcsHostingService, project);
                                addPart(toolingPartStack);

                                lastSelected = project;
                              }
                            })
                        .catchError(
                            new Operation<PromiseError>() {
                              @Override
                              public void apply(final PromiseError error)
                                  throws OperationException {
                                invalidateContext(rootProject);
                                contributePart.showStub("Failed to display " + error.getMessage()); //todo
                              }
                            });
                  }
                })
            .catchError(
                new Operation<PromiseError>() {
                  @Override
                  public void apply(final PromiseError error) throws OperationException {
                    invalidateContext(rootProject);
                    contributePart.showStub("Some error" + error); // todo
                  }
                });
      }

    } else {
      invalidateContext(rootProject);
      contributePart.showStub(contributeMessages.stubTextProjectNotProvideVSC()); // todo
    }

    lastSelected = rootProject;
  }

  private void invalidateContext(Project project) {
    final Optional<Context> context = workflowExecutor.getContext(project.getName());
    if (context.isPresent()) {
      workflowExecutor.invalidateContext(context.get().getProject());
    }
  }

  private void addPart(PartStack partStack) {
    if (!partStack.containsPart(contributePart)) {
      partStack.addPart(contributePart, FIRST);
      if (partStack.getActivePart() == null) {
        partStack.setActivePart(contributePart);
      }
    }
  }

  private boolean hasVcsService(Project project) {
    return vcsServiceProvider.getVcsService(project) != null;
  }

  private boolean hasContributionMixin(Project project) {
    return project.getMixins().contains(CONTRIBUTION_PROJECT_TYPE_ID);
  }

  private Promise<Project> addMixin(final Project project) {
    final VcsService vcsService = vcsServiceProvider.getVcsService(project);

    if (vcsService == null || project.getMixins().contains(CONTRIBUTION_PROJECT_TYPE_ID)) {
      return promiseProvider.resolve(project);
    }

    return vcsService
        .getBranchName(project)
        .thenPromise(
            new Function<String, Promise<Project>>() {
              @Override
              public Promise<Project> apply(String branchName) throws FunctionException {
                MutableProjectConfig mutableConfig = new MutableProjectConfig(project);
                mutableConfig.getMixins().add(CONTRIBUTION_PROJECT_TYPE_ID);
                mutableConfig
                    .getAttributes()
                    .put(CONTRIBUTE_TO_BRANCH_VARIABLE_NAME, singletonList(branchName));

                return project.update().withBody(mutableConfig).send();
              }
            });
  }
}
