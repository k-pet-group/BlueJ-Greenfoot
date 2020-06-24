/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import bluej.editor.Editor;
import bluej.editor.flow.FlowEditor;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.actions.EditAction;
import bluej.utility.Debug;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 */
public class ReadmeTarget extends NonCodeEditableTarget
{
    private static final String openStr = Config.getString("pkgmgr.packagemenu.open");

    public static final String README_ID = "@README";

    // Images
    @OnThread(Tag.FXPlatform)
    private static Image readmeImage;
    @OnThread(Tag.FXPlatform)
    private static Image selectedReadmeImage;
    @OnThread(Tag.FXPlatform)
    private ImageView imageView;

    public ReadmeTarget(Package pkg)
    {
        // create the target with an identifier name that cannot be
        // a valid java name
        super(pkg, README_ID, "" /* Already named README... */);

        if (readmeImage == null)
            readmeImage = Config.getImageAsFXImage("image.readme");
        if (selectedReadmeImage == null)
            selectedReadmeImage= Config.getImageAsFXImage("image.readme-selected");

        setPos(10, 10);
        setSize((int)readmeImage.getWidth(), (int)readmeImage.getHeight());
        JavaFXUtil.addStyleClass(pane, "readme-target");
        pane.setTop(null);
        imageView = new ImageView();
        imageView.setImage(readmeImage);
        pane.setCenter(imageView);
    }

    @Override
    public void load(Properties props, String prefix) throws NumberFormatException
    {
    }

    /*
     * @return the name of the (text) file this target corresponds to.
     */
    @Override
    public File getSourceFile()
    {
        return new File(getPackage().getPath(), Package.readmeName);
    }

    @Override
    @OnThread(Tag.FX)
    public boolean isResizable()
    {
        return false;
    }
    
    /*
     * Although we do save some information (the editor position) about a Readme
     * this is not done via the usual target save mechanism. If the normal save
     * mechanism was used, the readme target would appear as a normal target.
     * This would result in not being able to open a project saved in a newer
     * BlueJ version with an older BlueJ version.
     */
    @Override
    @OnThread(Tag.Any)
    public boolean isSaveable()
    {
        return false;
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
            flowEditor.showFile(getSourceFile().getAbsolutePath(), StandardCharsets.UTF_8, false, null);
            this.editor = flowEditor;
        }
        return editor;
    }


    private void openEditor(boolean openInNewWindow)
    {
        if (editor == null) {
            if (! getSourceFile().exists()) {
                try {
                    getSourceFile().createNewFile();
                }
                catch (IOException ioe) {
                    Debug.reportError("Couldn't open README", ioe);
                }
            }
        }
        
       // now try again to open it
       if(getEditor() != null) {
           editor.setEditorVisible(true, openInNewWindow);
       }
    }

    /*
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void doubleClick(boolean openInNewWindow)
    {
        openEditor(openInNewWindow);
    }

    @Override
    public List<? extends AbstractOperation<Target>> getContextOperations()
    {
        return List.of(new EditAction());
    }

    @Override
    public void remove()
    {
        // The user is not permitted to remove the readmefile
    }
        
    @Override
    public @OnThread(Tag.FXPlatform) void setSelected(boolean selected)
    {
        super.setSelected(selected);
        imageView.setImage(selected ? selectedReadmeImage : readmeImage);

    }
}
