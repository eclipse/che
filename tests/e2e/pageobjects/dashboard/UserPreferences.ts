/** *******************************************************************
 * copyright (c) 2019-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { inject, injectable } from 'inversify';
import 'reflect-metadata';
import { CLASSES } from '../../configs/inversify.types';
import { By } from 'selenium-webdriver';
import { DriverHelper } from '../../utils/DriverHelper';
import { Logger } from '../../utils/Logger';
import { GitProviderType } from '../../constants/FACTORY_TEST_CONSTANTS';
import { TIMEOUT_CONSTANTS } from '../../constants/TIMEOUT_CONSTANTS';

@injectable()
export class UserPreferences {
	private static readonly USER_SETTINGS_DROPDOWN: By = By.xpath('//header//button/span[text()!=""]//parent::button');
	private static readonly USER_PREFERENCES_BUTTON: By = By.xpath('//button[text()="User Preferences"]');
	private static readonly USER_PREFERENCES_PAGE: By = By.xpath('//h1[text()="User Preferences"]');

	private static readonly CONTAINER_REGISTRIES_TAB: By = By.xpath('//button[text()="Container Registries"]');

	private static readonly GIT_SERVICES_TAB: By = By.xpath('//button[text()="Git Services"]');
	private static readonly GIT_SERVICES_REVOKE_BUTTON: By = By.xpath('//button[text()="Revoke"]');

	private static readonly PAT_TAB: By = By.xpath('//button[text()="Personal Access Tokens"]');
	private static readonly ADD_NEW_PAT_BUTTON: By = By.xpath('//button[text()="Add Personal Access Token"]');

	private static readonly GIT_CONFIG_PAGE: By = By.xpath('//button[text()="Gitconfig"]');

	private static readonly SSH_KEY_TAB: By = By.xpath('//button[text()="SSH Keys"]');
	private static readonly ADD_NEW_SSH_KEY_BUTTON: By = By.xpath('//button[text()="Add SSH Key"]');

	private static readonly CONFIRMATION_WINDOW: By = By.xpath('//span[text()="Revoke Git Services"]');
	private static readonly DELETE_CONFIRMATION_CHECKBOX: By = By.xpath('//input[@data-testid="warning-info-checkbox"]');
	private static readonly DELETE_ITEM_BUTTON_ENABLED: By = By.xpath('//button[@data-testid="revoke-button" and not(@disabled)]');

	constructor(
		@inject(CLASSES.DriverHelper)
		readonly driverHelper: DriverHelper
	) {}

	async openUserPreferencesPage(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(UserPreferences.USER_SETTINGS_DROPDOWN);
		await this.driverHelper.waitAndClick(UserPreferences.USER_PREFERENCES_BUTTON);

		await this.driverHelper.waitVisibility(UserPreferences.USER_PREFERENCES_PAGE);
	}

	async checkTabsAvailability(): Promise<void> {
		Logger.debug();

		await this.openContainerRegistriesTab();
		await this.openGitServicesTab();
		await this.openPatTab();
		await this.openGitConfigPage();
		await this.openSshKeyTab();
	}

	async openContainerRegistriesTab(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(UserPreferences.CONTAINER_REGISTRIES_TAB);
	}

	async openGitServicesTab(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(UserPreferences.GIT_SERVICES_TAB);
	}

	async revokeGitService(servicesName: string): Promise<void> {
		Logger.debug();

		await this.selectListItem(servicesName);
		await this.driverHelper.waitAndClick(UserPreferences.GIT_SERVICES_REVOKE_BUTTON);

		await this.driverHelper.waitVisibility(UserPreferences.CONFIRMATION_WINDOW);
		await this.driverHelper.waitAndClick(UserPreferences.DELETE_CONFIRMATION_CHECKBOX);
		await this.driverHelper.waitAndClick(UserPreferences.DELETE_ITEM_BUTTON_ENABLED);

		await this.driverHelper.waitAttributeValue(
			this.getServicesListItemLocator(servicesName),
			'disabled',
			'true',
			TIMEOUT_CONSTANTS.TS_COMMON_DASHBOARD_WAIT_TIMEOUT
		);
	}

	async selectListItem(servicesName: string): Promise<void> {
		Logger.debug(`of the '${servicesName}' list item`);

		await this.driverHelper.waitAndClick(this.getServicesListItemLocator(servicesName));
	}

	async openPatTab(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(UserPreferences.PAT_TAB);
		await this.driverHelper.waitVisibility(UserPreferences.ADD_NEW_PAT_BUTTON);
	}

	async openGitConfigPage(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(UserPreferences.GIT_CONFIG_PAGE);
	}

	async openSshKeyTab(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(UserPreferences.SSH_KEY_TAB);
		await this.driverHelper.waitVisibility(UserPreferences.ADD_NEW_SSH_KEY_BUTTON);
	}

	getServiceConfig(service: string): string {
		const gitService: { [key: string]: string } = {
			[GitProviderType.GITHUB]: 'GitHub',
			[GitProviderType.GITLAB]: 'GitLab',
			[GitProviderType.AZURE_DEVOPS]: 'Microsoft Azure DevOps',
			[GitProviderType.BITBUCKET_CLOUD_OAUTH2]: 'Bitbucket Cloud',
			[GitProviderType.BITBUCKET_SERVER_OAUTH1]: 'Bitbucket Server',
			[GitProviderType.BITBUCKET_SERVER_OAUTH2]: 'Bitbucket Server'
		};

		return gitService[service];
	}

	private getServicesListItemLocator(servicesName: string): By {
		return By.xpath(`//tr[td[text()='${servicesName}']]//input`);
	}
}
