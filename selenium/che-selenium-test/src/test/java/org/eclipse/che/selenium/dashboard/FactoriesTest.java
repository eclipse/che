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
package org.eclipse.che.selenium.dashboard;

import static org.eclipse.che.selenium.core.constant.TestStacksConstants.JAVA;
import static org.eclipse.che.selenium.pageobject.dashboard.ProjectSourcePage.Template.WEB_JAVA_SPRING;
import static org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails.StateWorkspace.STOPPED;
import static org.testng.AssertJUnit.assertEquals;

import com.google.inject.Inject;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.client.TestFactoryServiceClient;
import org.eclipse.che.selenium.core.factory.FactoryTemplate;
import org.eclipse.che.selenium.core.factory.TestFactoryInitializer;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.DashboardFactory;
import org.eclipse.che.selenium.pageobject.dashboard.NewWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.ProjectSourcePage;
import org.eclipse.che.selenium.pageobject.dashboard.factories.FactoryDetails;
import org.eclipse.che.selenium.pageobject.dashboard.factories.NewFactory;
import org.eclipse.che.selenium.pageobject.dashboard.factories.NewFactory.TabNames;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.WorkspaceDetails;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.Workspaces;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FactoriesTest {

  private static final String MINIMAL_TEMPLATE_FACTORY_NAME = NameGenerator.generate("factory", 4);
  private static final String COMPLETE_TEMPLATE_FACTORY_NAME = NameGenerator.generate("factory", 4);
  private static final String CREATED_FROM_WORKSPACE_FACTORY_NAME =
      NameGenerator.generate("factory", 4);
  private static final String WORKSPACE_NAME = NameGenerator.generate("workspace", 4);
  private static final String MIN_FACTORY_NAME = NameGenerator.generate("factory", 4);
  private static final String MAX_FACTORY_NAME = NameGenerator.generate("factory", 4);

  private static final String NAME_IS_TOO_SHORT = "The name has to be more than 3 characters long.";
  private static final String NAME_IS_TOO_LONG = "The name has to be less than 20 characters long.";

  @Inject private TestFactoryServiceClient factoryServiceClient;
  @Inject private TestFactoryInitializer testFactoryInitializer;
  @Inject private DashboardFactory dashboardFactory;
  @Inject private ProjectSourcePage projectSourcePage;
  @Inject private FactoryDetails factoryDetails;
  @Inject private NewFactory newFactory;
  @Inject private Dashboard dashboard;

  @Inject private Loader loader;
  @Inject private NewWorkspace newWorkspace;
  @Inject private WorkspaceDetails workspaceDetails;
  @Inject private Workspaces workspaces;

  @BeforeClass
  public void setUp() throws Exception {
    createFactoryByApi("factory1");
    createFactoryByApi("factory2");
    createFactoryByApi("factory3");

    dashboard.open();
    createWorkspaceWithProject(WORKSPACE_NAME);
  }

  @AfterClass
  public void tearDown() throws Exception {
    factoryServiceClient.deleteFactory(MINIMAL_TEMPLATE_FACTORY_NAME);
  }

  @Test
  public void checkFactoryName() {
    dashboardFactory.selectFactoriesOnNavBar();
    dashboardFactory.waitAllFactoriesPage();
    dashboardFactory.clickOnAddFactoryBtn();

    newFactory.waitToolbarTitle();
    newFactory.typeFactoryName(MIN_FACTORY_NAME);
    newFactory.waitErrorMessageNotVisible();
    // TODO test the Create button is enabled

    newFactory.typeFactoryName(MAX_FACTORY_NAME);
    newFactory.waitErrorMessageNotVisible();

    newFactory.typeFactoryName(NameGenerator.generate("", 2));
    assertEquals(newFactory.getErrorMessage(), NAME_IS_TOO_SHORT);
    // TODO test the Create button is disabled

    newFactory.typeFactoryName(NameGenerator.generate("", 21));
    assertEquals(newFactory.getErrorMessage(), NAME_IS_TOO_LONG);
  }

  @Test
  public void createFactoryFromTemplates() {
    dashboardFactory.selectFactoriesOnNavBar();
    dashboardFactory.waitAllFactoriesPage();
    dashboardFactory.clickOnAddFactoryBtn();

    // create a factory from minimal template
    newFactory.waitToolbarTitle();
    newFactory.typeFactoryName(MINIMAL_TEMPLATE_FACTORY_NAME);
    newFactory.clickOnSourceTab(TabNames.TEMPLATE_TAB_ID);
    newFactory.clickOnMinimalTemplateButton();
    newFactory.clickOnCreateButton();
    factoryDetails.waitFactoryName(MINIMAL_TEMPLATE_FACTORY_NAME);
    factoryDetails.clickOnBackToFactoriesListButton();

    dashboardFactory.waitAllFactoriesPage();
    dashboardFactory.waitFactoryName(MINIMAL_TEMPLATE_FACTORY_NAME);
    assertEquals(dashboardFactory.getFactoryRamLimit(MINIMAL_TEMPLATE_FACTORY_NAME), "2048 MB");

    // create a factory from complete template
    dashboardFactory.waitAllFactoriesPage();
    dashboardFactory.clickOnAddFactoryBtn();
    newFactory.waitToolbarTitle();
    newFactory.typeFactoryName(COMPLETE_TEMPLATE_FACTORY_NAME);
    newFactory.clickOnSourceTab(TabNames.TEMPLATE_TAB_ID);
    newFactory.clickOnCompleteTemplateButton();
    newFactory.clickOnCreateButton();
    factoryDetails.waitFactoryName(COMPLETE_TEMPLATE_FACTORY_NAME);
    factoryDetails.clickOnBackToFactoriesListButton();

    dashboardFactory.waitAllFactoriesPage();
    dashboardFactory.waitFactoryName(COMPLETE_TEMPLATE_FACTORY_NAME);
    Assert.assertEquals(
        dashboardFactory.getFactoryRamLimit(COMPLETE_TEMPLATE_FACTORY_NAME), "2048 MB");
  }

  @Test
  public void createFactoryFromWorkspace() {
    dashboardFactory.selectFactoriesOnNavBar();
    dashboardFactory.waitAllFactoriesPage();
    dashboardFactory.clickOnAddFactoryBtn();

    newFactory.clickOnSourceTab(TabNames.WORKSPACE_TAB_ID);
    newFactory.typeFactoryName(CREATED_FROM_WORKSPACE_FACTORY_NAME);
    newFactory.clickOnWorkspaceFromList(WORKSPACE_NAME);
    newFactory.clickOnCreateButton();
    factoryDetails.waitFactoryName(CREATED_FROM_WORKSPACE_FACTORY_NAME);
    factoryDetails.clickOnBackToFactoriesListButton();

    dashboardFactory.waitAllFactoriesPage();
    dashboardFactory.waitFactoryName(CREATED_FROM_WORKSPACE_FACTORY_NAME);
    Assert.assertEquals(
        dashboardFactory.getFactoryRamLimit(CREATED_FROM_WORKSPACE_FACTORY_NAME), "3072 MB");
  }

  private void createFactoryByApi(String factoryName) throws Exception {
    TestFactoryInitializer.TestFactoryBuilder factoryBuilder =
        testFactoryInitializer.fromTemplate(FactoryTemplate.MINIMAL);
    factoryBuilder.setName(factoryName);
    factoryBuilder.build();
  }

  private void createWorkspaceWithProject(String workspaceName) {
    String machineName = "dev-machine";

    dashboard.waitDashboardToolbarTitle();
    dashboard.selectWorkspacesItemOnDashboard();
    dashboard.waitToolbarTitleName("Workspaces");
    workspaces.clickOnAddWorkspaceBtn();
    newWorkspace.waitToolbar();
    loader.waitOnClosed();
    newWorkspace.selectStack(JAVA.getId());
    newWorkspace.typeWorkspaceName(workspaceName);
    newWorkspace.setMachineRAM(machineName, 3.0);

    projectSourcePage.clickOnAddOrImportProjectButton();
    projectSourcePage.selectSample(WEB_JAVA_SPRING);
    projectSourcePage.clickOnAddProjectButton();
    projectSourcePage.waitCreatedProjectButton(WEB_JAVA_SPRING);

    newWorkspace.clickOnCreateButtonAndEditWorkspace();

    workspaceDetails.waitToolbarTitleName(workspaceName);
    workspaceDetails.checkStateOfWorkspace(STOPPED);
  }
}
