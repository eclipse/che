/*
 * Copyright (c) 2015-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';
import {CheNotification} from '../../../../components/notification/che-notification.factory';
import {CheWorkspace} from '../../../../components/api/che-workspace.factory';

/**
 * @ngdoc controller
 * @name workspaces.list.controller:WorkspaceItemStatusController
 * @description This class is handling the controller for workspace status widget
 * @author Oleksii Orel
 */
export class WorkspaceStatusController {
  cheNotification: CheNotification;
  cheWorkspace: CheWorkspace;

  isLoading: boolean;
  workspace: che.IWorkspace;

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor(cheNotification: CheNotification, cheWorkspace: CheWorkspace) {
    this.cheNotification = cheNotification;
    this.cheWorkspace = cheWorkspace;

    this.isLoading = false;
  }

  startWorkspace(): void {
    if (this.isLoading || !this.workspace || !this.workspace.config || this.workspace.status !== 'STOPPED') {
      return;
    }

    this.isLoading = true;
    let promise = this.cheWorkspace.startWorkspace(this.workspace.id, this.workspace.config.defaultEnv);

    promise.then(() => {
      this.isLoading = false;
    }, (error: any) => {
      this.isLoading = false;
      this.cheNotification.showError(error.data.message ? error.data.message : 'Run workspace error.');
    });
  }

  stopWorkspace(): void {
    if (this.isLoading || !this.workspace || this.workspace.status !== 'RUNNING') {
      return;
    }

    this.isLoading = true;
    let promise = this.cheWorkspace.stopWorkspace(this.workspace.id);

    promise.then(() => {
      this.isLoading = false;
    }, (error: any) => {
      this.isLoading = false;
      this.cheNotification.showError(error.data.message ? error.data.message : 'Stop workspace error.');
    });
  }

}
