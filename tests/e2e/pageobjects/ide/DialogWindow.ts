/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { injectable, inject } from 'inversify';
import { CLASSES } from '../../inversify.types';
import { DriverHelper } from '../../utils/DriverHelper';
import { By } from 'selenium-webdriver';
import { Logger } from '../../utils/Logger';
import { TestConstants } from '../../TestConstants';

@injectable()
export class DialogWindow {
    private static readonly DIALOG_BODY_XPATH_LOCATOR: string = '//div[@id=\'theia-dialog-shell\']//div[@class=\'dialogBlock\']';
    private static readonly CLOSE_BUTTON_XPATH_LOCATOR: string = `${DialogWindow.DIALOG_BODY_XPATH_LOCATOR}//button[text()='close']`;

    constructor(@inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper) { }

    async dialogDisplayes(): Promise<boolean> {
        Logger.debug('WarningDialog.dialogDisplayes');

        return await this.driverHelper.isVisible(By.xpath(DialogWindow.DIALOG_BODY_XPATH_LOCATOR));
    }

    async waitAndCloseIfAppear() {
        Logger.debug('WarningDialog.waitAndCloseIfAppear');

        const dialogDisplayes: boolean = await this.driverHelper.waitVisibilityBoolean(By.xpath(DialogWindow.DIALOG_BODY_XPATH_LOCATOR));

        if (dialogDisplayes) {
            await this.closeDialog();
            await this.waitDialogDissappearance();
        }

    }

    async clickToButton(buttonText: string) {
        Logger.debug('WarningDialog.clickToButton');

        const buttonLocator: By = By.xpath(`${DialogWindow.DIALOG_BODY_XPATH_LOCATOR}//button[text()='${buttonText}']`);
        await this.driverHelper.waitAndClick(buttonLocator);
    }

    async closeDialog() {
        Logger.debug('WarningDialog.closeDialog');

        await this.clickToButton('close');
    }

    async clickToOpenLinkButton() {
        Logger.debug('WarningDialog.clickToOpenLinkButton');

        await this.clickToButton('Open Link');
    }

    async waitDialog(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('WarningDialog.waitDialog');

        await this.driverHelper.waitVisibility(By.xpath(DialogWindow.DIALOG_BODY_XPATH_LOCATOR), timeout);
    }

    async waitDialogAndOpenLink(timeout: number = TestConstants.TS_SELENIUM_DEFAULT_TIMEOUT) {
        Logger.debug('WarningDialog.waitDialogAndOpenLink');

        await this.waitDialog(timeout);
        await this.clickToOpenLinkButton();
        await this.waitDialogDissappearance();
    }

    async waitDialogDissappearance() {
        Logger.debug('WarningDialog.waitDialogDissappearance');

        await this.driverHelper.waitDisappearanceWithTimeout(By.xpath(DialogWindow.CLOSE_BUTTON_XPATH_LOCATOR));
    }

}
