/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.core.db.h2;

import static com.google.common.base.MoreObjects.firstNonNull;

import org.eclipse.che.core.db.JNDIDataSourceFactory;

/**
 * Environment params based JNDI data source factory for H2SQL.
 *
 * @author Sergii Kabashniuk
 */
public class H2SQLJndiDataSourceFactory extends JNDIDataSourceFactory {

  public static final String DEFAULT_USERNAME = "";
  public static final String DEFAULT_PASSWORD = "";
  public static final String DEFAULT_URL = "jdbc:h2:che";
  public static final String DEFAULT_DRIVER__CLASS__NAME = "org.h2.Driver";
  public static final String DEFAULT_MAX__TOTAL = "8";
  public static final String DEFAULT_MAX__IDLE = "2";
  public static final String DEFAULT_MAX__WAIT__MILLIS = "-1";

  public H2SQLJndiDataSourceFactory() throws Exception {
    super(
        firstNonNull(System.getenv("CHE_JDBC_USERNAME"), DEFAULT_USERNAME),
        firstNonNull(System.getenv("CHE_JDBC_PASSWORD"), DEFAULT_PASSWORD),
        firstNonNull(System.getenv("CHE_JDBC_URL"), DEFAULT_URL),
        firstNonNull(System.getenv("CHE_JDBC_DRIVER__CLASS__NAME"), DEFAULT_DRIVER__CLASS__NAME),
        firstNonNull(System.getenv("CHE_JDBC_MAX__TOTAL"), DEFAULT_MAX__TOTAL),
        firstNonNull(System.getenv("CHE_JDBC_MAX__IDLE"), DEFAULT_MAX__IDLE),
        firstNonNull(System.getenv("CHE_JDBC_MAX__WAIT__MILLIS"), DEFAULT_MAX__WAIT__MILLIS));
  }
}
