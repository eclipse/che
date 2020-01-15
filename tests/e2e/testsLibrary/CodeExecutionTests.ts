/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import { CLASSES, Terminal, TopMenu, Ide, DialogWindow } from '..';
import { e2eContainer } from '../inversify.config';

const terminal: Terminal = e2eContainer.get(CLASSES.Terminal);
const topMenu: TopMenu = e2eContainer.get(CLASSES.TopMenu);
const ide: Ide = e2eContainer.get(CLASSES.Ide);
const dialogWindow: DialogWindow = e2eContainer.get(CLASSES.DialogWindow);

const dialogWindowCloseButtonText: string = 'close';

export function runTask(taskName: string, timeout: number) {
    test(`Run command '${taskName}'`, async () => {
        await topMenu.runTask(taskName);
        await ide.waitNotification('has exited with code 0.', timeout);
    });
}

export function runTaskWithDialogShellAndOpenLink(taskName: string, expectedDialogText: string, timeout: number) {
    test(`Run command '${taskName}' expecting dialog shell`, async () => {
        await topMenu.runTask(taskName);
        await dialogWindow.waitDialogAndOpenLink(timeout, expectedDialogText);
    });
}

export function runTaskWithDialogShellAndClose(taskName: string, expectedDialogText: string, timeout: number) {
    test(`Run command '${taskName}' expecting dialog shell`, async () => {
        await topMenu.runTask(taskName);
        await dialogWindow.waitDialog(timeout, expectedDialogText);
        await dialogWindow.clickToButton(dialogWindowCloseButtonText);
        await dialogWindow.waitDialogDissappearance();
    });
}

export function closeTerminal(taskName: string) {
    test('Close the terminal tasks', async () => {
        await ide.closeAllNotifications();
        await terminal.closeTerminalTab(taskName);
    });
}
