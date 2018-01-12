/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.workspaces;

import static org.eclipse.che.selenium.core.constant.TestStacksConstants.NODE;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.APPLICATION_START_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.LOAD_PAGE_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.PREPARING_WS_TIMEOUT_SEC;
import static org.eclipse.che.selenium.pageobject.ProjectExplorer.CommandsGoal.BUILD;
import static org.eclipse.che.selenium.pageobject.ProjectExplorer.CommandsGoal.RUN;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;
import static org.testng.Assert.fail;

import com.google.inject.Inject;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.client.TestWorkspaceServiceClient;
import org.eclipse.che.selenium.core.user.TestUser;
import org.eclipse.che.selenium.pageobject.AskDialog;
import org.eclipse.che.selenium.pageobject.Consoles;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.dashboard.CreateWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.NavigationBar;
import org.eclipse.che.selenium.pageobject.dashboard.ProjectSourcePage;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.Workspaces;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/** @author Aleksandr Shmaraev */
public class WorkingWithNodeWsTest {
  private static final String WORKSPACE = NameGenerator.generate("WorkingWithNode", 4);
  private static final String PROJECT_NAME = "web-nodejs-simple";
  private static final String RUN_PROCESS = PROJECT_NAME + ":run";
  private static final String INSTALL_DEPENDENCIES_PROCESS = PROJECT_NAME + ":install dependencies";
  private static final String ASK_DIALOG_MSG_ANGULAR_APP =
      "The process web-nodejs-simple:run will be terminated after closing console. Do you want to continue?";

  @Inject private TestUser defaultTestUser;
  @Inject private Ide ide;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private Consoles consoles;
  @Inject private NavigationBar navigationBar;
  @Inject private CreateWorkspace createWorkspace;
  @Inject private ProjectSourcePage projectSourcePage;
  @Inject private Dashboard dashboard;
  @Inject private WorkspaceDetails workspaceDetails;
  @Inject private AskDialog askDialog;
  @Inject private SeleniumWebDriver seleniumWebDriver;
  @Inject private TestWorkspaceServiceClient workspaceServiceClient;
  @Inject private Workspaces workspaces;

  @AfterClass
  public void tearDown() throws Exception {
    workspaceServiceClient.delete(WORKSPACE, defaultTestUser.getName());
  }

  @Test
  public void checkNodeJsWsAndRunApp() {
    String currentWindow;

    // Create a workspace from the Node stack with the web-nodejs-simple project
    dashboard.open();
    dashboard.waitDashboardToolbarTitle();
    dashboard.selectWorkspacesItemOnDashboard();
    dashboard.waitToolbarTitleName("Workspaces");
    workspaces.clickOnNewWorkspaceBtn();
    createWorkspace.waitToolbar();
    createWorkspace.selectStack(NODE.getId());
    createWorkspace.typeWorkspaceName(WORKSPACE);
    projectSourcePage.clickOnAddOrImportProjectButton();
    projectSourcePage.selectSample(PROJECT_NAME);
    projectSourcePage.clickOnAddProjectButton();
    createWorkspace.clickOnCreateWorkspaceButton();

    seleniumWebDriver.switchFromDashboardIframeToIde();
    currentWindow = seleniumWebDriver.getWindowHandle();
    projectExplorer.waitProjectExplorer();
    projectExplorer.waitItem(PROJECT_NAME);
    projectExplorer.selectItem(PROJECT_NAME);

    // Perform run web nodeJs application
    consoles.startCommandFromProcessesArea("dev-machine", BUILD, INSTALL_DEPENDENCIES_PROCESS);
    consoles.waitTabNameProcessIsPresent(INSTALL_DEPENDENCIES_PROCESS);
    consoles.waitExpectedTextIntoConsole("bower_components/angular", APPLICATION_START_TIMEOUT_SEC);

    consoles.startCommandFromProcessesArea("dev-machine", RUN, RUN_PROCESS);
    consoles.waitTabNameProcessIsPresent(RUN_PROCESS);
    consoles.waitExpectedTextIntoConsole("Started connect web server", PREPARING_WS_TIMEOUT_SEC);

    // Check the preview url is present after refreshing
    consoles.waitPreviewUrlIsPresent();
    seleniumWebDriver.navigate().refresh();
    seleniumWebDriver.switchFromDashboardIframeToIde();
    try {
      consoles.waitPreviewUrlIsPresent();
    } catch (TimeoutException ex) {
      // Remove try-catch block after issue has been resolved
      fail("Known issue https://github.com/eclipse/che/issues/7072");
    }

    // Run the application
    projectExplorer.selectItem(PROJECT_NAME);
    consoles.selectProcessInProcessConsoleTreeByName(RUN_PROCESS);
    consoles.clickOnPreviewUrl();
    seleniumWebDriver.switchToNoneCurrentWindow(currentWindow);
    checkAngularYeomanAppl();
    seleniumWebDriver.close();
    seleniumWebDriver.switchTo().window(currentWindow);
    seleniumWebDriver.switchFromDashboardIframeToIde();

    // Close terminal tab for 'run' process
    consoles.closeProcessInProcessConsoleTreeByName(RUN_PROCESS);
    askDialog.acceptDialogWithText(ASK_DIALOG_MSG_ANGULAR_APP);
    consoles.waitProcessIsNotPresentInProcessConsoleTree(RUN_PROCESS);
  }

  /** Check main elements of the AngularJS-Yeoman */
  public void checkAngularYeomanAppl() {
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(By.xpath("//h1[text()=\"'Allo, 'Allo!\"]")));
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(By.xpath("//img[@src='images/yeoman.png']")));
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(By.linkText("Splendid!")))
        .click();
  }
}
