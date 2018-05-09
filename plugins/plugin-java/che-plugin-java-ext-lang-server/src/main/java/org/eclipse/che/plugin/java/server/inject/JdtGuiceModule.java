/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.java.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.file.Paths;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.plugin.java.server.refactoring.RefactoringManager;
import org.eclipse.che.plugin.java.server.rest.JavaFormatterService;
import org.eclipse.che.plugin.java.server.rest.JavaNavigationService;
import org.eclipse.che.plugin.java.server.rest.RefactoringService;
import org.eclipse.core.internal.filebuffers.FileBuffersPlugin;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/** @author Evgen Vidolob */
@DynaModule
public class JdtGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(JavaNavigationService.class);
    bind(JavaFormatterService.class);
    bind(ResourcesPlugin.class).asEagerSingleton();
    bind(JavaPlugin.class).asEagerSingleton();
    bind(FileBuffersPlugin.class).asEagerSingleton();
    bind(RefactoringManager.class).asEagerSingleton();
    bind(RefactoringService.class);
  }

  @Provides
  @Named("che.jdt.settings.dir")
  @Singleton
  protected String provideSettings(@Named("che.workspace.metadata") String wsMetadata) {
    return Paths.get(System.getProperty("user.home"), wsMetadata, "settings").toString();
  }

  @Provides
  @Named("che.jdt.workspace.index.dir")
  @Singleton
  protected String provideIndex(@Named("che.workspace.metadata") String wsMetadata) {
    return Paths.get(System.getProperty("user.home"), wsMetadata, "index").toString();
  }
}
