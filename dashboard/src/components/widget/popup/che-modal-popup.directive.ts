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

/**
 * @ngdoc directive
 * @name components.directive:cheModalPopup
 * @restrict E
 * @function
 * @element
 *
 * @description
 * `<che-modal-popup>` defines modal popup component as wrapper for transclude content
 *
 * @param {string=} title the title of popup massage
 * @param {Function=} on-close close popup function
 *
 * @author Oleksii Orel
 */
export class CheModalPopup {
  restrict: string;
  templateUrl: string;
  transclude: boolean;
  scope: {
    [propName: string]: string
  };

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor() {
    this.restrict = 'E';
    this.transclude = true;
    this.templateUrl = 'components/widget/popup/che-modal-popup.html';

    // scope values
    this.scope = {
      title: '@',
      onClose: '&'
    };
  }

}
