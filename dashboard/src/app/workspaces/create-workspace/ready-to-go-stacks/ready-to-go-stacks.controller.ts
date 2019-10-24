/*
 * Copyright (c) 2015-2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

import { CreateWorkspaceSvc } from '../create-workspace.service';
import { NamespaceSelectorSvc } from './namespace-selector/namespace-selector.service';
import { RandomSvc } from '../../../../components/utils/random.service';
import { IReadyToGoStacksScopeBindings } from './ready-to-go-stacks.directive';

const WORKSPACE_NAME_FORM = 'workspaceName';

/**
 * TODO
 */
export class ReadyToGoStacksController implements IReadyToGoStacksScopeBindings {

  static $inject = [
    '$timeout',
    'createWorkspaceSvc',
    'namespaceSelectorSvc',
    'randomSvc'
  ];

  /**
   * Directive scope bindings.
   */
  onChange: (eventData: { devfile: che.IWorkspaceDevfile, attrs: { [key: string]: string } }) => void;

  /**
   * Injected dependencies.
   */
  private $timeout: ng.ITimeoutService;
  private createWorkspaceSvc: CreateWorkspaceSvc;
  private namespaceSelectorSvc: NamespaceSelectorSvc;
  private randomSvc: RandomSvc;

  /**
   * The selected devfile.
   */
  private selectedDevfile: che.IWorkspaceDevfile;
  /**
   * The selected namespace ID.
   */
  private namespaceId: string;
  /**
   * The map of forms.
   */
  private forms: Map<string, ng.IFormController>;
  /**
   * The list of names of existing workspaces.
   */
  private usedNamesList: string[];
  /**
   * The name of workspace.
   */
  private workspaceName: string;
  /**
   * Hide progress loader if <code>true</code>.
   */
  private stackName: string;
  /**
   * Progress loader is hidden if it's `true`.
   */
  private hideLoader: boolean;

  /**
   * Default constructor that is using resource injection
   */
  constructor(
    $timeout: ng.ITimeoutService,
    createWorkspaceSvc: CreateWorkspaceSvc,
    namespaceSelectorSvc: NamespaceSelectorSvc,
    randomSvc: RandomSvc
  ) {
    this.$timeout = $timeout;
    this.createWorkspaceSvc = createWorkspaceSvc;
    this.namespaceSelectorSvc = namespaceSelectorSvc;
    this.randomSvc = randomSvc;

    this.usedNamesList = [];
    this.forms = new Map();

    this.hideLoader = false;
  }

  $onInit(): void {
    this.namespaceId = this.namespaceSelectorSvc.getNamespaceId();
    this.buildListOfUsedNames().then(() => {
      this.workspaceName = this.randomSvc.getRandString({ prefix: 'wksp-', list: this.usedNamesList });
      this.reValidateName();
    });
  }

  /**
   * Stores forms in list.
   *
   * @param name a name to register form controller.
   * @param form a form controller.
   */
  registerForm(name: string, form: ng.IFormController) {
    this.forms.set(name, form);
  }

  /**
   * Returns `false` if workspace name is not unique in the namespace.
   * Only member with 'manageWorkspaces' permission can definitely know whether
   * name is unique or not.
   *
   * @param name workspace name
   */
  isNameUnique(name: string): boolean {
    return this.usedNamesList.indexOf(name) === -1;
  }

  /**
   * Returns a warning message in case if namespace is missed.
   */
  getNamespaceEmptyMessage(): string {
    return this.namespaceSelectorSvc.getNamespaceEmptyMessage();
  }


  /**
   * Returns list of namespaces.
   */
  getNamespaces(): Array<che.INamespace> {
    return this.namespaceSelectorSvc.getNamespaces();
  }

  /**
   * Returns namespaces caption.
   */
  getNamespaceCaption(): string {
    return this.namespaceSelectorSvc.getNamespaceCaption();
  }

  /**
   * Callback which is called when stack is selected.
   */
  onDevfileSelected(devfile: che.IWorkspaceDevfile): void {
    // tiny timeout for templates selector to be rendered
    this.$timeout(() => {
      this.hideLoader = true;
    }, 10);
    this.selectedDevfile = devfile;
    this.onChange({
      devfile,
      attrs: { stackName: this.stackName }
    });
  }

  /**
   * Callback which is called when namespace is selected.
   */
  onNamespaceChanged(namespaceId: string) {
    this.namespaceId = namespaceId;

    this.buildListOfUsedNames().then(() => {
      this.reValidateName();
    });
  }

  /**
   * Triggers form validation on Settings tab.
   */
  private reValidateName(): void {
    const form: ng.IFormController = this.forms.get('name');

    if (!form) {
      return;
    }

    ['name', 'deskname'].forEach((inputName: string) => {
      const model = form[inputName] as ng.INgModelController;
      if (model) {
        model.$validate();
      }
    });
  }

  /**
   * Filters list of workspaces by current namespace and
   * builds list of names for current namespace.
   * TODO move this implementation into createWorkspaceSvc because it may be used by other nested directives
   */
  private buildListOfUsedNames(): ng.IPromise<void> {
    return this.createWorkspaceSvc.fetchWorkspacesByNamespace(this.namespaceId).then((workspaces: Array<che.IWorkspace>) => {
      this.usedNamesList = workspaces.filter((workspace: che.IWorkspace) => {
        return workspace.namespace === this.namespaceId;
      }).map((workspace: che.IWorkspace) => {
        return this.createWorkspaceSvc.getWorkspaceName(workspace);
      });
    });
  }

}
