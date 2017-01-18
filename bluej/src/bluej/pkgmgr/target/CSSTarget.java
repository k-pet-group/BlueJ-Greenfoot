/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr.target;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Properties;

import javafx.application.Platform;
import javafx.scene.control.Label;

import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.extensions.SourceType;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A CSS file in the package directory.
 */
public class CSSTarget extends NonCodeEditableTarget
{
    private final File file;

    public CSSTarget(Package aPackage, File file)
    {
        super(aPackage, file.getName());
        this.file = file;

        Platform.runLater(() -> {
            JavaFXUtil.addStyleClass(pane, "css-target");
            pane.setCenter(new Label(file.getName()));
        });
    }

    @Override
    public @OnThread(Tag.FXPlatform) void doubleClick()
    {
        SwingUtilities.invokeLater(() -> open());
    }

    @Override
    public @OnThread(Tag.FXPlatform) void popupMenu(int x, int y, PackageEditor editor)
    {
        // TODO
    }

    @Override
    public void remove()
    {
        // TODO
    }

    @Override
    protected File getSourceFile()
    {
        return file;
    }

    @Override
    public Editor getEditor()
    {
        if(editor == null) {
            editor = EditorManager.getEditorManager().openText(
                    getSourceFile().getPath(),
                    getPackage().getProject().getProjectCharset(),
                    getSourceFile().getName(), getPackage().getProject()::getDefaultFXTabbedEditor);
        }
        return editor;
    }

    @Override
    public @OnThread(Tag.FXPlatform) boolean isMoveable()
    {
        return true;
    }

    @Override
    public @OnThread(Tag.FX) boolean isResizable()
    {
        return true;
    }
    @Override
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        props.put(prefix + ".type", "CSSTarget");
    }
}
