"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
var OpenShiftLoginPage_1;
"use strict";
/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
require("reflect-metadata");
const inversify_1 = require("inversify");
const DriverHelper_1 = require("../../utils/DriverHelper");
const inversify_types_1 = require("../../inversify.types");
const TestConstants_1 = require("../../TestConstants");
const selenium_webdriver_1 = require("selenium-webdriver");
let OpenShiftLoginPage = OpenShiftLoginPage_1 = class OpenShiftLoginPage {
    constructor(driverHelper) {
        this.driverHelper = driverHelper;
    }
    openLoginPageOpenShift() {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.driverHelper.navigateToUrl(TestConstants_1.TestConstants.TS_SELENIUM_OPENSHIFT4_URL);
        });
    }
    waitOpenShiftLoginPage() {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.driverHelper.waitVisibility(selenium_webdriver_1.By.xpath(OpenShiftLoginPage_1.LOGIN_PAGE_OPENSHIFT));
        });
    }
    clickOnLoginWitnKubeAdmin() {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.driverHelper.waitAndClick(selenium_webdriver_1.By.xpath('//a[@title=\'Log in with kube:admin\']'));
        });
    }
    enterUserNameOpenShift(userName) {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.driverHelper.enterValue(selenium_webdriver_1.By.id('inputUsername'), userName);
        });
    }
    enterPasswordOpenShift(passw) {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.driverHelper.enterValue(selenium_webdriver_1.By.id('inputPassword'), passw);
        });
    }
    clickOnLoginButton() {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.driverHelper.waitAndClick(selenium_webdriver_1.By.xpath('//button[text()=\'Log In\']'));
        });
    }
    waitDisappearanceLoginPageOpenShift() {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.driverHelper.waitDisappearance(selenium_webdriver_1.By.xpath(OpenShiftLoginPage_1.LOGIN_PAGE_OPENSHIFT));
        });
    }
};
OpenShiftLoginPage.LOGIN_PAGE_OPENSHIFT = '//div[contains(@class, \'login\')]';
OpenShiftLoginPage = OpenShiftLoginPage_1 = __decorate([
    inversify_1.injectable(),
    __param(0, inversify_1.inject(inversify_types_1.CLASSES.DriverHelper)),
    __metadata("design:paramtypes", [DriverHelper_1.DriverHelper])
], OpenShiftLoginPage);
exports.OpenShiftLoginPage = OpenShiftLoginPage;
