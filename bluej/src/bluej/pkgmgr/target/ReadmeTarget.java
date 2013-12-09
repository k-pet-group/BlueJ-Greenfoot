/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013  Michael Kolling and John Rosenberg 
 
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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.graph.GraphEditor;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.graphPainter.ReadmeTargetPainter;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 */
public class ReadmeTarget extends EditableTarget
{
    private static final int WIDTH = ReadmeTargetPainter.getMaxImageWidth();
    private static final int HEIGHT = ReadmeTargetPainter.getMaxImageHeight();
    private static String openStr = Config.getString("pkgmgr.packagemenu.open");
    private static final Color envOpColour = Config.ENV_COLOUR;
    
    public static final String README_ID = "@README";

    public ReadmeTarget(Package pkg)
    {
        // create the target with an identifier name that cannot be
        // a valid java name
        super(pkg, README_ID);
        
        setPos(10, 10);
        setSize(WIDTH, HEIGHT);
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

    @Override
    public void save(Properties props, String prefix)
    {   
        if (editor != null) {
            editorBounds = editor.getBounds();            
        } 
        if(editorBounds!=null) {
            props.put(prefix + ".editor.x", String.valueOf((int) editorBounds.getX()));
            props.put(prefix + ".editor.y", String.valueOf((int) editorBounds.getY()));
            props.put(prefix + ".editor.width", String.valueOf((int) editorBounds.getWidth()));
            props.put(prefix + ".editor.height", String.valueOf((int) editorBounds.getHeight()));
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
                                                 Package.readmeName, editorBounds);
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
    public void doubleClick(MouseEvent evt)
    {
        openEditor();
    }

    /*
     * Post the context menu for this target.
     */
    @Override
    public void popupMenu(int x, int y, GraphEditor editor)
    {
        JPopupMenu menu = createMenu(null);
        if (menu != null) {
            // editor.add(menu);
            menu.show(editor, x, y);
        }
    }
    
    /**
     * Construct a popup menu which displays all our parent packages.
     */
    private JPopupMenu createMenu(Class<?> cl)
    {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item;
           
        Action openAction = new OpenAction(openStr);

        item = menu.add(openAction);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        return menu;
    }

    private class OpenAction extends AbstractAction
    {
        public OpenAction(String menu)
        {
            super(menu);
        }

        public void actionPerformed(ActionEvent e)
        {
            openEditor();
        }
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
    public void recordEdit(String curSource, boolean includeOneLineEdits) { }

    @Override
    public String getTooltipText()
    {
        return "README";
    }
}
