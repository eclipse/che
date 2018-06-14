/*
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

import {CheStack} from '../../../../components/api/che-stack.factory';
import {Observable} from '../../../../components/utils/observable';

/**
 * Service for stack selector.
 *
 * @author Oleksii Kurinnyi
 */
export class StackSelectorSvc extends Observable<any> {

  static $inject = ['$log', '$q', 'cheStack'];

  /**
   * Log service.
   */
  private $log: ng.ILogService;
  /**
   * Promises service.
   */
  private $q: ng.IQService;
  /**
   * Stack API interaction.
   */
  private cheStack: CheStack;
  /**
   * Selected stack ID.
   */
  private stack: string;

  /**
   * Default constructor that is using resource injection
   */
  constructor($log: ng.ILogService, $q: ng.IQService, cheStack: CheStack) {
    super();

    this.$log = $log;
    this.$q = $q;
    this.cheStack = cheStack;
  }

  /**
   * Fetch list of available stacks.
   *
   * @return {IPromise<Array<che.IStack>>}
   */
  getOrFetchStacks(): ng.IPromise<Array<che.IStack>> {
    const stacks = this.getStacks();
    if (stacks.length) {
      return this.$q.when(stacks);
    }

    return this.cheStack.fetchStacks().then(() => {
      return this.$q.when();
    }, (error: any) => {
      if (error && error.status !== 304) {
        this.$log.error(error);
      }
      return this.$q.when();
    }).then(() => {
      const stacks = this.getStacks();
      return this.$q.when(stacks);
    });
  }

  /**
   * Sets stack.
   *
   * @param {string} stack
   */
  setStackId(stack: string): void {
    this.onStackSelected(stack);
  }

  /**
   * Callback which is called when stack is selected.
   *
   * @param {string} stack a stack ID
   */
  onStackSelected(stack: string): void {
    this.stack = stack;

    this.publish(stack);
  }

  /**
   * Returns list of stacks.
   *
   * @return {Array<che.IStack>}
   */
  getStacks(): Array<che.IStack> {
    return this.cheStack.getStacks();
  }

  /**
   * Returns selected stack ID.
   *
   * @return {string}
   */
  getStackId(): string {
    return this.stack;
  }

  /**
   * Returns stack by its ID.
   *
   * @param {string} stack a stack ID
   * @return {che.IStack}
   */
  getStackById(stack: string): che.IStack {
    return this.getStacks().find((stack: che.IStack) => {
      return stack.id === stack;
    });
  }

}
