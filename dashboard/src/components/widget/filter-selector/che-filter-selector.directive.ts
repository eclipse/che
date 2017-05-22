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

/**
 * Defines the class for filter selector widget.
 * @author Ann Shumilova
 */
export class CheFilterSelector implements ng.IDirective {

  restrict: string = 'E';
  bindToController: boolean = true;
  templateUrl: string = 'components/widget/filter-selector/che-filter-selector.html';
  controller: string = 'CheFilterSelectorController';
  controllerAs: string = 'cheFilterSelectorController';
  require: Array<string> = ['ngModel'];

  scope: {
    [propName: string]: string
  };

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor() {
    this.scope = {
      valueModel: '=ngModel',
      values: '=cheValues',
      isDisabled: '=cheDisabled',
      onChange: '&cheOnChange',
      width: '@?cheWidth'
    };
  }
}
