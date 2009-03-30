/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;

import javax.swing.*;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.editor.*;
import bluej.graph.GraphEditor;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 * @version $Id: ReadmeTarget.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ReadmeTarget extends EditableTarget
    implements ActionListener
{
    static final int WIDTH = 40;
    static final int HEIGHT = 50;
    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color colBorder = Config.getItemColour("colour.target.border");
    static final Color textfg = Config.getItemColour("colour.text.fg");
    static String openStr = Config.getString("pkgmgr.packagemenu.open");
    static String removeStr = Config.getString("pkgmgr.packagemenu.remove");
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");
    
    public static final String README_ID = "@README";

    public ReadmeTarget(Package pkg)
    {
        // create the target with an identifier name that cannot be
        // a valid java name
        super(pkg, README_ID);
        
        setPos(10, 10);
        setSize(WIDTH, HEIGHT);
    }

    public void load(Properties props, String prefix) throws NumberFormatException
    {
        if(props.getProperty(prefix + ".editor.x") != null) {
	        editorBounds = new Rectangle(Integer.parseInt(props.getProperty(prefix + ".editor.x")),
	                Integer.parseInt(props.getProperty(prefix + ".editor.y")), 
	                Integer.parseInt(props.getProperty(prefix + ".editor.width")),
	                Integer.parseInt(props.getProperty(prefix + ".editor.height")));
        }        
    }

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

    /**
     * @return the name of the (text) file this target corresponds to.
     */
    public File getSourceFile()
    {
        return new File(getPackage().getPath(), Package.readmeName);
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * For package targets, this has not yet been implemented.
     *
     * @arg directory The directory to copy into (ending with "/")
     */
    public boolean copyFiles(String directory)
    {
        return true;
    }

    public boolean isResizable()
    {
        return false;
    }
    
    public boolean isHandle() 
    {
        return false;
    }

    public boolean isMoveable()
    {
        return false;
    }

    /**
     * Although we do save some information (the editor position) about a Readme
     * this is not done via the usual target save mechanism. If the normal save
     * mechanism was used, the readme target would appear as a normal target.
     * This would result in not being able to open a project saved in a newer
     * BlueJ version with an older BlueJ version.
     */
    public boolean isSaveable()
    {
        return false;
    }

    Color getBackgroundColour()
    {
        return defaultbg;
    }

    Color getBorderColour()
    {
        return colBorder;
    }

    Color getTextColour()
    {
        return textfg;
    }

    Font getFont()
    {
        return PrefMgr.getStandardFont();
    }

    /**
     ** @return the editor object associated with this target. May be null
     **  if there was a problem opening this editor.
     **/
    public Editor getEditor()
    {
        if(editor == null)
            editor = EditorManager.getEditorManager().openText(
                                                 getSourceFile().getPath(),
                                                 Package.readmeName, editorBounds);
        return editor;
    }


    public void actionPerformed(ActionEvent e)
    {
    }


    private void openEditor()
    {
       // try to open it and if not there, create it
       if(getEditor() == null) {
           try {
               getSourceFile().createNewFile();
           }
           catch (IOException ioe) {
               ioe.printStackTrace();
           }
       }
    
       // now try again to open it
       if(getEditor() != null)
           getEditor().setVisible(true);
    }

    /**
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    public void doubleClick(MouseEvent evt)
    {
       openEditor();
    }

    /**
     * Post the context menu for this target.
     */
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
    private JPopupMenu createMenu(Class cl)
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
    
    public void remove()
    {
        // The user is not permitted to remove the readmefile
    }
    
    public void generateDoc()
    {
        // meaningless
    }

}
