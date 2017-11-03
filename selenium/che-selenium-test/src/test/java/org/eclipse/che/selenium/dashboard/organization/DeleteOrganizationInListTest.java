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
package org.eclipse.che.selenium.dashboard.organization;

import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.selenium.pageobject.dashboard.NavigationBar.MenuItem.ORGANIZATIONS;
import static org.eclipse.che.selenium.pageobject.dashboard.organization.OrganizationListPage.OrganizationListHeader.NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import org.eclipse.che.multiuser.organization.shared.dto.OrganizationDto;
import org.eclipse.che.selenium.core.client.TestOrganizationServiceClient;
import org.eclipse.che.selenium.core.user.AdminTestUser;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.eclipse.che.selenium.pageobject.dashboard.ConfirmDialog;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.NavigationBar;
import org.eclipse.che.selenium.pageobject.dashboard.organization.OrganizationListPage;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test validates organization creation and actions on it in the list of organizations.
 *
 * @author Ann Shumilova
 */
public class DeleteOrganizationInListTest {
  private static final String ORGANIZATION_NAME = generate("organization", 7);

  private List<OrganizationDto> organizations;
  private OrganizationDto organization;

  @Inject private OrganizationListPage organizationListPage;
  @Inject private NavigationBar navigationBar;
  @Inject private ConfirmDialog confirmDialog;
  @Inject private AdminTestUser adminTestUser;
  @Inject private Dashboard dashboard;

  @Inject
  @Named("admin")
  private TestOrganizationServiceClient testOrganizationServiceClient;

  @BeforeClass
  public void setUp() throws Exception {
    organization = testOrganizationServiceClient.create(ORGANIZATION_NAME);
    testOrganizationServiceClient.create(generate("organization", 7));
    testOrganizationServiceClient.create(generate("organization", 7));
    testOrganizationServiceClient.create(generate("organization", 7));
    organizations = testOrganizationServiceClient.getAll();

    dashboard.open(adminTestUser.getName(), adminTestUser.getPassword());
  }

  @AfterClass
  public void tearDown() throws Exception {
    for (OrganizationDto organization : organizations)
      testOrganizationServiceClient.deleteById(organization.getId());
  }

  @Test
  public void testOrganizationDeletionFromList() {
    int organizationsCount = organizations.size();

    navigationBar.waitNavigationBar();
    navigationBar.clickOnMenu(ORGANIZATIONS);
    organizationListPage.waitForOrganizationsToolbar();
    assertEquals(navigationBar.getMenuCounterValue(ORGANIZATIONS), organizationsCount);
    navigationBar.clickOnMenu(ORGANIZATIONS);

    organizationListPage.waitForOrganizationsToolbar();
    organizationListPage.waitForOrganizationsList();
    assertEquals(organizationListPage.getOrganizationListItemCount(), organizationsCount);
    assertTrue(organizationListPage.getValues(NAME).contains(organization.getName()));

    // Tests the Delete organization dialog
    organizationListPage.clickOnDeleteButton(organization.getName());
    confirmDialog.waitOpened();
    assertEquals(confirmDialog.getTitle(), "Delete organization");
    assertEquals(
        confirmDialog.getMessage(),
        String.format("Would you like to delete organization '%s'?", organization.getName()));
    assertEquals(confirmDialog.getConfirmButtonTitle(), "Delete");
    assertEquals(confirmDialog.getCancelButtonTitle(), "Close");
    confirmDialog.closeDialog();
    confirmDialog.waitClosed();
    assertEquals(organizationListPage.getOrganizationListItemCount(), organizationsCount);

    organizationListPage.clickOnDeleteButton(organization.getName());
    confirmDialog.waitOpened();
    confirmDialog.clickCancel();
    confirmDialog.waitClosed();
    assertEquals(organizationListPage.getOrganizationListItemCount(), organizationsCount);

    // Delete organization
    organizationListPage.clickOnDeleteButton(organization.getName());
    confirmDialog.waitOpened();
    confirmDialog.clickConfirm();
    confirmDialog.waitClosed();
    organizationListPage.waitForOrganizationsList();
    WaitUtils.sleepQuietly(3);
    assertEquals(organizationListPage.getOrganizationListItemCount(), organizationsCount - 1);
    assertFalse(organizationListPage.getValues(NAME).contains(organization.getName()));
    assertEquals(navigationBar.getMenuCounterValue(ORGANIZATIONS), organizationsCount - 1);
  }
}
