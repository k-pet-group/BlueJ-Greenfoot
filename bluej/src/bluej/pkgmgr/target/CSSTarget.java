/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import bluej.editor.flow.FlowEditor;
import bluej.pkgmgr.target.actions.EditAction;
import bluej.pkgmgr.target.actions.RemoveAction;
import bluej.utility.javafx.AbstractOperation;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import bluej.Config;
import bluej.editor.Editor;
import bluej.pkgmgr.Package;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A CSS file in the package directory.
 */
public class CSSTarget extends NonCodeEditableTarget
{
    private static final String openStr = Config.getString("pkgmgr.cssmenu.open");
    private static final String removeStr = Config.getString("pkgmgr.cssmenu.remove");


    private final File file;

    public CSSTarget(Package aPackage, File file)
    {
        super(aPackage, file.getName(), "" /* CSS already in name */);
        this.file = file;

        JavaFXUtil.addStyleClass(pane, "css-target");
        JavaFXUtil.addStyleClass(pane, "css-target-id-" + file.getName().replace(".", "-"));
        Label name = new Label(file.getName());
        BorderPane.setAlignment(name, Pos.CENTER);
        pane.setTop(name);
    }

    @Override
    public @OnThread(Tag.FXPlatform) void doubleClick(boolean openInNewWindow)
    {
        Editor editor = getEditor();
        if(editor == null)
        {
            getPackage().showError("error-open-source");
        }
        else
        {
            editor.setEditorVisible(true, openInNewWindow);
        }
    }

    @Override
    public List<? extends AbstractOperation<Target>> getContextOperations()
    {
        return List.of(new EditAction(), new RemoveAction());
    }

    @Override
    public void remove()
    {
        getPackage().removeTarget(this);
        file.delete();
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
            FlowEditor flowEditor = new FlowEditor(newWindow -> {
                if (newWindow)
                {
                    return getPackage().getProject().createNewFXTabbedEditor();
                }
                else
                {
                    return getPackage().getProject().getDefaultFXTabbedEditor();
                }
            }, getSourceFile().getName(), this, null, null, () -> {}, new ReadOnlyBooleanWrapper(false), false);
            flowEditor.showFile(file.getAbsolutePath(), StandardCharsets.UTF_8, false, null);
            this.editor = flowEditor;
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
