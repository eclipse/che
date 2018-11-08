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
package org.eclipse.che.selenium.pageobject;

import static java.lang.String.format;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.ELEMENT_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.LOAD_PAGE_TIMEOUT_SEC;
import static org.eclipse.che.selenium.pageobject.Menu.Locators.DISABLED_ITEM;
import static org.eclipse.che.selenium.pageobject.Menu.Locators.ENABLED_ITEM;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;

/** @author Musienko Maxim */
@Singleton
public class Menu {
  private final Loader loader;
  private final SeleniumWebDriverHelper seleniumWebDriverHelper;

  @Inject
  public Menu(Loader loader, SeleniumWebDriverHelper seleniumWebDriverHelper) {
    this.loader = loader;
    this.seleniumWebDriverHelper = seleniumWebDriverHelper;
  }

  public interface Locators {
    String DISABLED_ITEM = "//tr[@id='%s' and @item-enabled='false']";
    String ENABLED_ITEM = "//tr[@id='%s' and @item-enabled='true']";
  }

  /**
   * Run command from toolbar
   *
   * @param idCommand
   */
  public void runCommand(String idCommand) {
    runCommand(idCommand, LOAD_PAGE_TIMEOUT_SEC);
  }

  /**
   * Run command from toolbar with user delay for active state of menu
   *
   * @param idCommand
   * @param userDelay delay for waiting active state menu defined by user
   */
  public void runCommand(String idCommand, int userDelay) {
    seleniumWebDriverHelper.waitNoExceptions(
        () -> seleniumWebDriverHelper.waitAndClick(By.id(idCommand), userDelay),
        userDelay,
        StaleElementReferenceException.class);
  }

  /**
   * Run command from sub menu.
   *
   * @param idTopMenuCommand
   * @param idCommandName
   */
  public void runCommand(final String idTopMenuCommand, final String idCommandName) {
    seleniumWebDriverHelper.waitSuccessCondition(
        driver -> {
          runCommand(idTopMenuCommand);

          // Time for submenu opening
          WaitUtils.sleepQuietly(1);

          if (isMenuItemVisible(idCommandName)) {
            runCommand(idCommandName);
            return true;
          }

          return false;
        },
        ELEMENT_TIMEOUT_SEC);
  }

  /**
   * Run command from sub menu.
   *
   * @param idTopMenuCommand
   * @param idCommandName
   * @param idSubCommandName
   */
  public void runCommand(String idTopMenuCommand, String idCommandName, String idSubCommandName) {
    seleniumWebDriverHelper.waitSuccessCondition(
        driver -> {
          runCommand(idTopMenuCommand, idCommandName);

          // Time for submenu opening
          WaitUtils.sleepQuietly(1);

          if (isMenuItemVisible(idSubCommandName)) {
            runCommand(idSubCommandName);
            return true;
          }

          return false;
        },
        ELEMENT_TIMEOUT_SEC);
  }

  /**
   * Run command from sub menu.
   *
   * @param idTopMenuCommand
   * @param idCommandName
   * @param xpathSubCommandName
   */
  public void runCommandByXpath(
      String idTopMenuCommand, String idCommandName, String xpathSubCommandName) {

    seleniumWebDriverHelper.waitSuccessCondition(
        driver -> {
          runCommand(idTopMenuCommand, idCommandName);

          // Time for submenu opening
          WaitUtils.sleepQuietly(1);

          if (seleniumWebDriverHelper.isVisible(By.xpath(xpathSubCommandName))) {
            seleniumWebDriverHelper.waitAndClick(By.xpath(xpathSubCommandName));
            return true;
          }

          return false;
        },
        ELEMENT_TIMEOUT_SEC);
  }

  /**
   * Run command from menu using Web elements with xpath
   *
   * @param command is name of command
   */
  public void runCommandByXpath(String command) {
    final String commandXpath = format(ENABLED_ITEM, command);

    seleniumWebDriverHelper.waitAndClick(By.xpath(commandXpath));

    loader.waitOnClosed();
  }

  /** wait a command is not present in the menu */
  public void waitCommandIsNotPresentInMenu(String menuCommand) {
    seleniumWebDriverHelper.waitInvisibility(By.id(menuCommand));
  }

  /**
   * wait a command is disabled in the menu
   *
   * @param idCommand is name of command in the menu
   */
  public void waitCommandIsDisabledInMenu(String idCommand) {
    final String disabledItemXpath = format(DISABLED_ITEM, idCommand);

    seleniumWebDriverHelper.waitVisibility(By.xpath(disabledItemXpath));
  }

  /**
   * wait a menu item is enabled
   *
   * @param idCommand is name of command in the menu
   */
  public void waitMenuItemIsEnabled(String idCommand) {
    seleniumWebDriverHelper.waitVisibility(By.id(idCommand));
  }

  private boolean isMenuItemVisible(String itemId) {
    return seleniumWebDriverHelper.isVisible(By.id(itemId));
  }
}
