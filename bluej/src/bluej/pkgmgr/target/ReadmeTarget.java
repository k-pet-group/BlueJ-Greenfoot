/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014,2015,2016  Michael Kolling and John Rosenberg
 
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

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import bluej.Config;
import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.extensions.SourceType;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.utility.Debug;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 */
public class ReadmeTarget extends EditableTarget
{
    private static final int WIDTH = 30;
    private static final int HEIGHT = 50;
    private static final String openStr = Config.getString("pkgmgr.packagemenu.open");
    private static final Color envOpColour = Config.ENV_COLOUR;
    
    public static final String README_ID = "@README";

    public ReadmeTarget(Package pkg)
    {
        // create the target with an identifier name that cannot be
        // a valid java name
        super(pkg, README_ID);
        
        Platform.runLater(() -> {
            setPos(10, 10);
            setSize(WIDTH, HEIGHT);
        });
    }

    @Override
    public void load(Properties props, String prefix) throws NumberFormatException
    {
        if(props.getProperty(prefix + ".editor.x") != null) {
            editorBounds = new Rectangle(Integer.parseInt(props.getProperty(prefix + ".editor.x")),
                    Integer.parseInt(props.getProperty(prefix + ".editor.y")), 
                    Integer.parseInt(props.getProperty(prefix + ".editor.width")),
                    Integer.parseInt(props.getProperty(prefix + ".editor.height")));
        }        
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
    @OnThread(Tag.FXPlatform)
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
    public boolean isSaveable()
    {
        return false;
    }

    @Override
    public Editor getEditor()
    {
        if(editor == null) {
            editor = EditorManager.getEditorManager().openText(
                                                 getSourceFile().getPath(),
                                                 getPackage().getProject().getProjectCharset(),
                                                 Package.readmeName, getPackage().getProject()::getDefaultFXTabbedEditor);
        }
        return editor;
    }


    private void openEditor()
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
           editor.setVisible(true);
       }
    }

    /*
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void doubleClick()
    {
        SwingUtilities.invokeLater(() -> openEditor());
    }

    /*
     * Post the context menu for this target.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void popupMenu(int x, int y, PackageEditor editor)
    {
        ContextMenu menu = createMenu();
        if (menu != null) {
            // editor.add(menu);
            menu.show(getNode(), x, y);
        }
    }
    
    @OnThread(Tag.FXPlatform)
    private ContextMenu createMenu()
    {
        MenuItem open = new MenuItem(openStr);
        open.setOnAction(e -> SwingUtilities.invokeLater(() -> openEditor()));
        
        return new ContextMenu(open);
    }

    @Override
    public void remove()
    {
        // The user is not permitted to remove the readmefile
    }
    
    @Override
    public void generateDoc()
    {
        // meaningless
    }

    @Override
    public String getProperty(String key) 
    {
        return null;
    }

    @Override
    public void setProperty(String key, String value) { }
    
    @Override
    public void recordEdit(SourceType sourceType, String curSource, boolean includeOneLineEdits, StrideEditReason reason) { }

    @Override
    public void recordClose() { }

    @Override
    public void recordOpen() { }

    @Override
    public void recordSelected() { }

    @Override
    public void recordShowErrorMessage(int identifier, List<String> quickFixes) { }

    @Override
    public void recordEarlyErrors(List<DiagnosticWithShown> diagnostics) { }

    @Override
    public void recordLateErrors(List<DiagnosticWithShown> diagnostics) { }

    @Override
    public void recordFix(int errorIdentifier, int fixIndex) { }

    @Override
    public void recordCodeCompletionStarted(Integer line, Integer column, String xpath, Integer index, String stem) { }

    @Override
    public void recordCodeCompletionEnded(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, String replacement) { }

    @Override
    public void recordUnknownCommandKey(String enclosingFrameXpath, int cursorIndex, char key) { }

    @Override
    public void recordShowErrorIndicator(int identifier) { }

    @Override
    public void compile(Editor editor, CompileReason reason, CompileType type) {}
    
    @Override
    @OnThread(Tag.Any)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type) {}
}
