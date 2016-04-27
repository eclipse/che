/*******************************************************************************
 * Copyright (c) 2014-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.editor.orion.client;

import com.google.inject.Inject;

import org.eclipse.che.ide.jseditor.client.defaulteditor.EditorBuilder;
import org.eclipse.che.ide.jseditor.client.editorconfig.AutoSaveTextEditorConfiguration;
import org.eclipse.che.ide.jseditor.client.texteditor.ConfigurableTextEditor;
import org.eclipse.che.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;

/**
 * Builder for Orion editor.
 *
 * @author Artem Zatsarynnyi
 */
public class OrionEditorBuilder implements EditorBuilder {

    private final OrionTextEditorFactory orionTextEditorFactory;

    @Inject
    public OrionEditorBuilder(OrionTextEditorFactory orionTextEditorFactory) {
        this.orionTextEditorFactory = orionTextEditorFactory;
    }

    @Override
    public ConfigurableTextEditor buildEditor() {
        final EmbeddedTextEditorPresenter<OrionEditorWidget> editor = orionTextEditorFactory.createTextEditor();
        editor.initialize(new AutoSaveTextEditorConfiguration());
        return editor;
    }
}
