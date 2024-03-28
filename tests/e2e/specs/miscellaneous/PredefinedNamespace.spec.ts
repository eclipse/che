/** *******************************************************************
 * copyright (c) 2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { e2eContainer } from '../../configs/inversify.config';
import { expect } from 'chai';
import { CLASSES } from '../../configs/inversify.types';
import { WorkspaceHandlingTests } from '../../tests-library/WorkspaceHandlingTests';
import { Logger } from '../../utils/Logger';
import { KubernetesCommandLineToolsExecutor } from '../../utils/KubernetesCommandLineToolsExecutor';
import { ShellExecutor } from '../../utils/ShellExecutor';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';
import { DevWorkspaceConfigurationHelper } from '../../utils/DevWorkspaceConfigurationHelper';
import { DevfileContext } from '@eclipse-che/che-devworkspace-generator/lib/api/devfile-context';

suite(`Create predefined workspace and check it ${BASE_TEST_CONSTANTS.TEST_ENVIRONMENT}`, function (): void {
	const predefinedNamespaceName: string = 'predefined-ns';
	const workspaceName: string = 'empty-ws';
	const shellExecutor: ShellExecutor = e2eContainer.get(CLASSES.ShellExecutor);
	const userName: string = 'user';
	let devWorkspaceConfigurationHelper: DevWorkspaceConfigurationHelper;
	let devfileContext: DevfileContext;
	const kubernetesCommandLineToolsExecutor: KubernetesCommandLineToolsExecutor = e2eContainer.get(
		CLASSES.KubernetesCommandLineToolsExecutor
	);

	suiteSetup(function (): void {
		// create a predefined namespace for user using shell script and login into user dashboard
		Logger.debug('Test prerequisites:');
		Logger.debug(
			' (1) there is OCP user with username and user password that have been set in the TS_SELENIUM_OCP_USERNAME and TS_SELENIUM_OCP_PASSWORD variables'
		);
		Logger.debug(' (2) "oc" client installed and logged into test OCP cluster with admin rights.');
		kubernetesCommandLineToolsExecutor.loginToOcp('admin');
		const predefinedProjectConfiguration: string =
			'kind: Namespace\n' +
			'apiVersion: v1\n' +
			'metadata:\n' +
			`  name: ${predefinedNamespaceName}\n` +
			'  labels:\n' +
			'    app.kubernetes.io/part-of: che.eclipse.org\n' +
			'    app.kubernetes.io/component: workspaces-namespace\n' +
			'  annotations:\n' +
			'    che.eclipse.org/username: user';
		kubernetesCommandLineToolsExecutor.applyWithoutNamespace(predefinedProjectConfiguration);
		const setEditRightsForUser: string = `oc adm policy add-role-to-user edit user -n ${predefinedNamespaceName}`;
		shellExecutor.executeCommand(setEditRightsForUser);
	});
	// generate empty workspace DevFile and create it through oc client under a regular user
	suiteSetup('Login', async function (): Promise<void> {
		const devfileContent: string = 'schemaVersion: 2.2.0\n' + 'metadata:\n' + `  name: ${workspaceName}\n`;
		kubernetesCommandLineToolsExecutor.loginToOcp(userName);
		devWorkspaceConfigurationHelper = new DevWorkspaceConfigurationHelper({
			devfileContent
		});
		devfileContext = await devWorkspaceConfigurationHelper.generateDevfileContext();
		const devWorkspaceConfigurationYamlString: string =
			devWorkspaceConfigurationHelper.getDevWorkspaceConfigurationYamlAsString(devfileContext);
		kubernetesCommandLineToolsExecutor.applyWithoutNamespace(devWorkspaceConfigurationYamlString);
	});

	// verify that just created workspace is available for the dedicated user
	test('Validate that predefined namespace has been created', function (): void {
		const expectedProject: string = shellExecutor.executeArbitraryShellScript('oc get projects');
		expect(expectedProject).contains(predefinedNamespaceName);
	});

	// ensure the generated DevSpace is created within the predefined namespace
	test('Create test DevWorkspace and verify its creation within the predefined namespace', function (): void {
		kubernetesCommandLineToolsExecutor.namespace = predefinedNamespaceName;
		kubernetesCommandLineToolsExecutor.workspaceName = workspaceName;
		// relogin under the admin user (because regular user does not have permissions for getting pod states)
		kubernetesCommandLineToolsExecutor.loginToOcp('admin');
		expect(kubernetesCommandLineToolsExecutor.waitDevWorkspace().stdout).contains('condition met');
	});

	// verify that the newly created workspace, identified by a unique name, exists within the predefined namespace. Given that multiple users may use the same cluster and create multiple DevSpaces within the same OpenShift project, it's essential to confirm that our test project, distinguished by a unique name, has been successfully created.
	test('Validate the creation of the correct DevWorkspace with a unique name', function (): void {
		const ocDevWorkspaceOutput: string = kubernetesCommandLineToolsExecutor.getDevWorkspaceYamlConfiguration();
		expect(ocDevWorkspaceOutput).includes(workspaceName);
	});

	suiteTeardown(function (): void {
		const workspaceName: string = WorkspaceHandlingTests.getWorkspaceName();
		try {
			// the test can failed under the regular user. Need login as admin for removing test namespace and DevSpaces.
			kubernetesCommandLineToolsExecutor.loginToOcp('admin');
			kubernetesCommandLineToolsExecutor.deleteDevWorkspace();
			kubernetesCommandLineToolsExecutor.deleteProject(predefinedNamespaceName);
		} catch (e) {
			Logger.error(`Cannot remove the predefined project: ${workspaceName}, please fix it manually: ${e}`);
		}
	});
});
