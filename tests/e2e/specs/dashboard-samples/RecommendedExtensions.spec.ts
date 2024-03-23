/** *******************************************************************
 * copyright (c) 2019-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import {
	ActivityBar,
	ContextMenu,
	ContextMenuItem,
	EditorView,
	ExtensionsViewItem,
	ExtensionsViewSection,
	Locators,
	SideBarView,
	TextEditor,
	ViewSection
} from 'monaco-page-objects';
import { registerRunningWorkspace } from '../MochaHooks';
import { LoginTests } from '../../tests-library/LoginTests';
import { e2eContainer } from '../../configs/inversify.config';
import { CLASSES, TYPES } from '../../configs/inversify.types';
import { WorkspaceHandlingTests } from '../../tests-library/WorkspaceHandlingTests';
import { ProjectAndFileTests } from '../../tests-library/ProjectAndFileTests';
import { Logger } from '../../utils/Logger';
import { DriverHelper } from '../../utils/DriverHelper';
import { CheCodeLocatorLoader } from '../../pageobjects/ide/CheCodeLocatorLoader';
import { expect } from 'chai';
import { TIMEOUT_CONSTANTS } from '../../constants/TIMEOUT_CONSTANTS';
import { BrowserTabsUtil } from '../../utils/BrowserTabsUtil';
import { PLUGIN_TEST_CONSTANTS } from '../../constants/PLUGIN_TEST_CONSTANTS';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';
import { ITestWorkspaceUtil } from '../../utils/workspace/ITestWorkspaceUtil';
import { Dashboard } from '../../pageobjects/dashboard/Dashboard';

const samples: string[] = PLUGIN_TEST_CONSTANTS.TS_SAMPLE_LIST.split(',');

for (const sample of samples) {
	suite(`Check if recommended extensions installed for ${sample} ${BASE_TEST_CONSTANTS.TEST_ENVIRONMENT}`, function (): void {
		this.bail(false);

		const projectAndFileTests: ProjectAndFileTests = e2eContainer.get(CLASSES.ProjectAndFileTests);
		const workspaceHandlingTests: WorkspaceHandlingTests = e2eContainer.get(CLASSES.WorkspaceHandlingTests);
		const loginTests: LoginTests = e2eContainer.get(CLASSES.LoginTests);
		const driverHelper: DriverHelper = e2eContainer.get(CLASSES.DriverHelper);
		const browserTabsUtil: BrowserTabsUtil = e2eContainer.get(CLASSES.BrowserTabsUtil);
		const cheCodeLocatorLoader: CheCodeLocatorLoader = e2eContainer.get(CLASSES.CheCodeLocatorLoader);
		const webCheCodeLocators: Locators = cheCodeLocatorLoader.webCheCodeLocators;
		const testWorkspaceUtil: ITestWorkspaceUtil = e2eContainer.get(TYPES.WorkspaceUtil);
		const dashboard: Dashboard = e2eContainer.get(CLASSES.Dashboard);

		let projectSection: ViewSection;
		let extensionSection: ExtensionsViewSection;
		let extensionsView: SideBarView | undefined;

		const [pathToExtensionsListFileName, extensionsListFileName]: string[] = ['.vscode', 'extensions.json'];
		let recommendedExtensions: any = {
			recommendations: []
		};

		suiteSetup('Login', async function (): Promise<void> {
			await loginTests.loginIntoChe();
		});

		test(`Create and open new workspace, stack:${sample}`, async function (): Promise<void> {
			await workspaceHandlingTests.createAndOpenWorkspace(sample);
		});
		test('Obtain workspace name from workspace loader page', async function (): Promise<void> {
			await workspaceHandlingTests.obtainWorkspaceNameFromStartingPage();
			expect(WorkspaceHandlingTests.getWorkspaceName(), 'Workspace name was not fetched from the loading page').not.undefined;
		});

		test('Registering the running workspace', function (): void {
			registerRunningWorkspace(WorkspaceHandlingTests.getWorkspaceName());
		});

		test('Wait workspace readiness', async function (): Promise<void> {
			await projectAndFileTests.waitWorkspaceReadinessForCheCodeEditor();
		});

		test('Check the project files were imported', async function (): Promise<void> {
			projectSection = await projectAndFileTests.getProjectViewSession();
			expect(await projectAndFileTests.getProjectTreeItem(projectSection, pathToExtensionsListFileName), 'Files not imported').not
				.undefined;
		});

		test('Accept the project as a trusted one', async function (): Promise<void> {
			await projectAndFileTests.performTrustAuthorDialog();
		});

		test(`Get recommended extensions list from ${extensionsListFileName}`, async function (): Promise<void> {
			await (await projectAndFileTests.getProjectTreeItem(projectSection, pathToExtensionsListFileName))?.select();
			await (await projectAndFileTests.getProjectTreeItem(projectSection, extensionsListFileName, 3))?.select();
			Logger.debug(`EditorView().openEditor(${extensionsListFileName})`);
			const editor: TextEditor = (await new EditorView().openEditor(extensionsListFileName)) as TextEditor;
			await driverHelper.waitVisibility(webCheCodeLocators.Editor.inputArea);
			Logger.debug('editor.getText(): get recommended extensions as text from editor, delete comments and parse to object.');
			recommendedExtensions = JSON.parse((await editor.getText()).replace(/\/\*[\s\S]*?\*\/|(?<=[^:])\/\/.*|^\/\/.*/g, '').trim());
			Logger.debug('recommendedExtensions.recommendations: Get recommendations clear names using map().');
			recommendedExtensions.recommendations = recommendedExtensions.recommendations.map(
				(r: { split: (arg: string) => [any, any] }): { name: any; publisher: any } => {
					const [publisher, name] = r.split('.');
					return { publisher, name };
				}
			);
			Logger.debug(`Recommended extension for this workspace:\n${JSON.stringify(recommendedExtensions.recommendations)}.`);
			expect(recommendedExtensions.recommendations, 'Recommendations not found').not.empty;
		});

		test('Open "Extensions" view section', async function (): Promise<void> {
			Logger.debug('ActivityBar().getViewControl("Extensions"))?.openView(): open Extensions view.');
			extensionsView = await (await new ActivityBar().getViewControl('Extensions'))?.openView();
			expect(extensionsView, 'Can`t find Extension section').not.undefined;
		});

		test('Let extensions complete installation', async function (): Promise<void> {
			this.test?.retries(0);
			Logger.debug(
				`Time for extensions installation TimeoutConstants.TS_COMMON_PLUGIN_TEST_TIMEOUT=${TIMEOUT_CONSTANTS.TS_COMMON_PLUGIN_TEST_TIMEOUT}`
			);
			await driverHelper.wait(TIMEOUT_CONSTANTS.TS_COMMON_PLUGIN_TEST_TIMEOUT);
		});

		test('Check if extensions are installed and enabled', async function (): Promise<void> {
			// timeout 15 seconds per extensions
			this.timeout(TIMEOUT_CONSTANTS.TS_FIND_EXTENSION_TEST_TIMEOUT * recommendedExtensions.recommendations.length);
			Logger.debug('ActivityBar().getViewControl("Extensions"))?.openView(): open Extensions view.');
			extensionsView = await (await new ActivityBar().getViewControl('Extensions'))?.openView();

			Logger.debug('extensionsView?.getContent().getSections(): get current section.');
			[extensionSection] = (await extensionsView?.getContent().getSections()) as ExtensionsViewSection[];
			expect(extensionSection, 'Can`t find Extension section').not.undefined;
			await driverHelper.waitVisibility(
				webCheCodeLocators.ExtensionsViewSection.itemTitle,
				TIMEOUT_CONSTANTS.TS_EDITOR_TAB_INTERACTION_TIMEOUT
			);

			for (const extension of recommendedExtensions.recommendations) {
				Logger.debug(`extensionSection.findItem(${extension.name}).`);
				await extensionSection.findItem(extension.name);

				const isReloadRequired: boolean = await driverHelper.isVisible(
					(webCheCodeLocators.ExtensionsViewSection as any).requireReloadButton
				);
				Logger.debug(`Is extensions require reload the editor: ${isReloadRequired}`);

				if (isReloadRequired) {
					Logger.debug('Refreshing the page..');
					await browserTabsUtil.refreshPage();
					await projectAndFileTests.waitWorkspaceReadinessForCheCodeEditor();
					await driverHelper.waitVisibility(
						webCheCodeLocators.ActivityBar.viewContainer,
						TIMEOUT_CONSTANTS.TS_EDITOR_TAB_INTERACTION_TIMEOUT
					);
					Logger.debug('ActivityBar().getViewControl("Extensions"))?.openView(): reopen Extensions view.');
					extensionsView = await (await new ActivityBar().getViewControl('Extensions'))?.openView();
					await driverHelper.waitVisibility(
						webCheCodeLocators.ExtensionsViewSection.itemTitle,
						TIMEOUT_CONSTANTS.TS_EDITOR_TAB_INTERACTION_TIMEOUT
					);
					expect(extensionsView, 'Can`t find Extension View section').not.undefined;
					[extensionSection] = (await extensionsView?.getContent().getSections()) as ExtensionsViewSection[];
					expect(extensionSection, 'Can`t find Extension section').not.undefined;
					Logger.debug(`extensionSection.findItem(${extension.name}).`);
					await extensionSection.findItem(extension.name);
				}

				Logger.debug('extensionsView.getContent().getSections(): switch to marketplace section.');
				const [marketplaceSection]: ExtensionsViewSection[] = (await extensionsView
					?.getContent()
					.getSections()) as ExtensionsViewSection[];

				Logger.debug('marketplaceSection.getVisibleItems()');
				const allFinedItems: ExtensionsViewItem[] = await marketplaceSection.getVisibleItems();
				expect(allFinedItems, 'Extensions not found').not.empty;
				let itemWithRightNameAndPublisher: ExtensionsViewItem | undefined = undefined;
				for (const item of allFinedItems) {
					Logger.debug(`Try to find extension published by ${extension.publisher}.`);
					if ((await item.getAuthor()) === extension.publisher) {
						itemWithRightNameAndPublisher = item;
						Logger.debug(`Extension was found: ${await itemWithRightNameAndPublisher?.getTitle()}`);
						break;
					}
					expect(itemWithRightNameAndPublisher, `Extension ${extension.name} not found`).not.undefined;
				}

				Logger.debug('itemWithRightNameAndPublisher?.isInstalled()');
				const isInstalled: boolean = (await itemWithRightNameAndPublisher?.isInstalled()) as boolean;

				Logger.debug(`itemWithRightNameAndPublisher?.isInstalled(): ${isInstalled}.`);
				expect(isInstalled, `Extension ${extension.name} not installed`).to.be.true;

				Logger.debug('itemWithRightNameAndPublisher.manage(): get context menu.');
				const extensionManageMenu: ContextMenu = await (itemWithRightNameAndPublisher as ExtensionsViewItem).manage();

				Logger.debug('extensionManageMenu.getItems(): get menu items.');
				const extensionMenuItems: ContextMenuItem[] = await extensionManageMenu.getItems();
				let extensionMenuItemLabels: string = '';
				Logger.trace('extensionMenuItems -> item.getLabel(): get menu items names.');
				for (const item of extensionMenuItems) {
					extensionMenuItemLabels += (await item.getLabel()) + ' ';
				}

				Logger.debug(`extensionMenuItemLabels: ${extensionMenuItemLabels}.`);
				expect(extensionMenuItemLabels, `Extension ${extension.name} not enabled`).contains('Disable').and.not.contains('Enable');
			}
		});

		suiteTeardown('Open dashboard and close all other tabs', async function (): Promise<void> {
			await dashboard.openDashboard();
			await browserTabsUtil.closeAllTabsExceptCurrent();
		});

		suiteTeardown('Stop and delete the workspace by API', async function (): Promise<void> {
			await testWorkspaceUtil.stopAndDeleteWorkspaceByName(WorkspaceHandlingTests.getWorkspaceName());
		});

		suiteTeardown('Unregister running workspace', function (): void {
			registerRunningWorkspace('');
		});
	});
}
