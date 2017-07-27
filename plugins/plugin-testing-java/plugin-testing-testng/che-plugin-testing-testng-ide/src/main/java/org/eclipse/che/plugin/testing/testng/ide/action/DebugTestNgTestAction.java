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
package org.eclipse.che.plugin.testing.testng.ide.action;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.jsonrpc.commons.JsonRpcError;
import org.eclipse.che.api.core.jsonrpc.commons.JsonRpcPromise;
import org.eclipse.che.api.testing.shared.TestExecutionContext;
import org.eclipse.che.api.testing.shared.TestLaunchResult;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.debug.DebugConfiguration;
import org.eclipse.che.ide.api.debug.DebugConfigurationsManager;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.plugin.testing.ide.TestServiceClient;
import org.eclipse.che.plugin.testing.ide.handler.TestingHandler;
import org.eclipse.che.plugin.testing.ide.model.GeneralTestingEventsProcessor;
import org.eclipse.che.plugin.testing.ide.view2.TestResultPresenter;
import org.eclipse.che.plugin.testing.testng.ide.TestNgLocalizationConstant;
import org.eclipse.che.plugin.testing.testng.ide.TestNgResources;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for debugging TestNg test.
 */
public class DebugTestNgTestAction extends TestNGAbstractAction {
    private       TestServiceClient          client;
    private       TestingHandler             testingHandler;
    private final NotificationManager        notificationManager;
    private       DebugConfigurationsManager debugConfigurationsManager;
    private final TestResultPresenter        testResultPresenter;

    @Inject
    public DebugTestNgTestAction(TestNgResources resources,
                                 EventBus eventBus,
                                 TestServiceClient client,
                                 TestingHandler testingHandler,
                                 DtoFactory dtoFactory,
                                 NotificationManager notificationManager,
                                 DebugConfigurationsManager debugConfigurationsManager,
                                 AppContext appContext,
                                 TestResultPresenter testResultPresenter,
                                 TestNgLocalizationConstant localization) {
        super(eventBus,
              client,
              dtoFactory,
              appContext,
              notificationManager,
              singletonList(PROJECT_PERSPECTIVE_ID),
              localization.actionDebugDescription(),
              localization.actionDebugTestTitle(),
              resources.testIcon());
        this.client = client;
        this.testingHandler = testingHandler;
        this.notificationManager = notificationManager;
        this.debugConfigurationsManager = debugConfigurationsManager;
        this.testResultPresenter = testResultPresenter;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final StatusNotification notification = new StatusNotification("Debugging Tests...", PROGRESS, FLOAT_MODE);
        notificationManager.notify(notification);

        Pair<String, String> frameworkAndTestName = Pair.of(TESTNG_TEST_FRAMEWORK, null);
        TestExecutionContext context = createTestExecutionContext(frameworkAndTestName, contextType, selectedNodePath);
        context.withDebugModeEnable(TRUE);

        GeneralTestingEventsProcessor eventsProcessor = new GeneralTestingEventsProcessor(TESTNG_TEST_FRAMEWORK,
                                                                                          testResultPresenter.getRootState());
        testingHandler.setProcessor(eventsProcessor);
        eventsProcessor.addListener(testResultPresenter.getEventListener());

        JsonRpcPromise<TestLaunchResult> testResultPromise = client.runTests(context);
        testResultPromise.onSuccess(result -> onTestRanSuccessfully(result, eventsProcessor, notification))
                         .onFailure(exception -> onTestRanFailed(exception, notification));
    }

    private void onTestRanSuccessfully(TestLaunchResult result,
                                       GeneralTestingEventsProcessor eventsProcessor,
                                       StatusNotification notification) {
        notification.setStatus(SUCCESS);
        if (result.isSuccess()) {
            notification.setTitle("Test runner executed successfully.");
            testResultPresenter.handleResponse();
            runRemoteJavaDebugger(eventsProcessor, result.getDebugPort());
        } else {
            notification.setTitle("Test runner failed to execute.");
        }
    }

    private void runRemoteJavaDebugger(GeneralTestingEventsProcessor eventsProcessor, int port) {
        DebugConfiguration debugger = debugConfigurationsManager.createConfiguration("jdb",
                                                                                     TESTNG_TEST_FRAMEWORK,
                                                                                     "localhost",
                                                                                     port,
                                                                                     emptyMap());
        eventsProcessor.setDebuggerConfiguration(debugger, debugConfigurationsManager);

        debugConfigurationsManager.apply(debugger);
    }

    private void onTestRanFailed(JsonRpcError exception, StatusNotification notification) {
        final String errorMessage = (exception.getMessage() != null) ? exception.getMessage()
                                                                     : "Failed to run test cases";
        notification.setContent(errorMessage);
        notification.setStatus(FAIL);
    }

}
