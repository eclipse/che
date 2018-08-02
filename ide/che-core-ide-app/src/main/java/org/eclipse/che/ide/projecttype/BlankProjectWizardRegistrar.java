/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.projecttype;

import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.wizard.WizardPage;

/**
 * Provides information for registering Blank project type into project wizard.
 *
 * @author Artem Zatsarynnyi
 */
public class BlankProjectWizardRegistrar implements ProjectWizardRegistrar {

  public static final String BLANK_CATEGORY = "Blank";
  public static final String BLANK_ID = "blank";

  private final List<Provider<? extends WizardPage<MutableProjectConfig>>> wizardPages;

  public BlankProjectWizardRegistrar() {
    wizardPages = new ArrayList<>();
  }

  @NotNull
  public String getProjectTypeId() {
    return BLANK_ID;
  }

  @NotNull
  public String getCategory() {
    return BLANK_CATEGORY;
  }

  @NotNull
  public List<Provider<? extends WizardPage<MutableProjectConfig>>> getWizardPages() {
    return wizardPages;
  }
}
