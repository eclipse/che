--
-- Copyright (c) 2012-2019 Red Hat, Inc.
-- This program and the accompanying materials are made
-- available under the terms of the Eclipse Public License 2.0
-- which is available at https://www.eclipse.org/legal/epl-2.0/
--
-- SPDX-License-Identifier: EPL-2.0
--
-- Contributors:
--   Red Hat, Inc. - initial API and implementation
--

ALTER TABLE devfile ALTER COLUMN meta_name DROP NOT NULL;
ALTER TABLE workspace ALTER COLUMN name SET NOT NULL;
CREATE UNIQUE INDEX index_che_workspace_name_account ON workspace (name, accountid);
