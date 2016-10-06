/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.part.explorer.project.macro;

import com.google.common.base.Joiner;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.resource.Path;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link ExplorerCurrentFileParentPathProvider}
 *
 * @author Artem Zatsarynnyi
 */
@RunWith(GwtMockitoTestRunner.class)
public class ExplorerCurrentFileParentPathProviderTest extends AbstractExplorerMacroProviderTest {

    private ExplorerCurrentFileParentPathProvider provider;

    @Before
    public void init() throws Exception {
        super.init();
        provider = new ExplorerCurrentFileParentPathProvider(projectExplorer, promiseProvider, appContext);
    }

    @Test
    public void testGetKey() throws Exception {
        assertSame(provider.getKey(), ExplorerCurrentFileParentPathProvider.KEY);
    }

    @Test
    public void getValue() throws Exception {
        initWithOneFile();

        provider.getValue();

        verify(promiseProvider).resolve(eq(Path.valueOf(PROJECTS_ROOT).append(FOLDER_PATH).toString()));
    }

    @Test
    public void getMultipleValues() throws Exception {
        initWithTwoFiles();

        provider.getValue();

        verify(promiseProvider).resolve(eq(Joiner.on(", ").join(Path.valueOf(PROJECTS_ROOT).append(FOLDER_PATH).toString(),
                                                                Path.valueOf(PROJECTS_ROOT).append(FOLDER_PATH).toString())));
    }

    @Test
    public void getEmptyValues() throws Exception {
        initWithNoFiles();

        provider.getValue();

        verify(promiseProvider).resolve(eq(""));
    }
}