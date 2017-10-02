/**
 * ***************************************************************************** Copyright (c) 2016
 * Rogue Wave Software, Inc. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Rogue Wave Software, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.plugin.zdb.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.debug.shared.model.Location;
import org.eclipse.che.api.debug.shared.model.impl.LocationImpl;
import org.eclipse.che.api.fs.server.FsManager;
import org.eclipse.che.api.fs.server.FsPathResolver;
import org.eclipse.che.api.project.server.api.ProjectManager;

/**
 * Zend debugger location handler. This class is responsible for bidirectional mapping/converting
 * locations that are specific for Che Virtual File System and Zend Debugger engine local file
 * system.
 *
 * @author Bartlomiej Laczkowski
 */
@Singleton
public class ZendDbgLocationHandler {

  private final FsPathResolver fsPathResolver;
  private final FsManager fsManager;
  private final ProjectManager projectManager;

  @Inject
  public ZendDbgLocationHandler(
      FsPathResolver fsPathResolver, FsManager fsManager, ProjectManager projectManager) {
    this.fsPathResolver = fsPathResolver;
    this.fsManager = fsManager;
    this.projectManager = projectManager;
  }

  public static final Location createVFS(
      String target, String resourceProjectPath, int lineNumber) {
    return new LocationImpl(target, lineNumber, false, 0, resourceProjectPath, null, -1);
  }

  public static final Location createDBG(String resourcePath, int lineNumber) {
    return new LocationImpl(resourcePath, lineNumber, false, 0, null, null, -1);
  }

  /**
   * Convert DBG specific location to VFS one.
   *
   * @return VFS specific location.
   */
  public Location convertToVFS(Location dbgLocation) {
    String remotePath = dbgLocation.getTarget();
    String wsPath = fsPathResolver.toAbsoluteWsPath(remotePath);
    if (!fsManager.exists(wsPath)) {
      return null;
    }

    String resourceProjectPath =
        projectManager
            .getClosest(wsPath)
            .orElseThrow(() -> new IllegalArgumentException("Can't find project"))
            .getPath();
    String target = fsPathResolver.getName(wsPath);
    int lineNumber = dbgLocation.getLineNumber();
    return new LocationImpl(
        target,
        lineNumber,
        false,
        0,
        resourceProjectPath,
        dbgLocation.getMethod(),
        dbgLocation.getThreadId());
  }

  /**
   * Convert VFS specific location to DBG one.
   *
   * @return DBG specific location.
   */
  public Location convertToDBG(Location vfsLocation) {
    return new LocationImpl(
        vfsLocation.getTarget(),
        vfsLocation.getLineNumber(),
        false,
        0,
        vfsLocation.getResourceProjectPath(),
        null,
        -1);
  }
}
