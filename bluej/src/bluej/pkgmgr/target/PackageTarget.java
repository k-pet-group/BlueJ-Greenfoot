/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2016  Michael Kolling and John Rosenberg 
 
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Properties;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A sub package (or parent package)
 * 
 * @author Michael Cahill
 */
public class PackageTarget extends Target
{
    static final int MIN_WIDTH = 60;
    static final int MIN_HEIGHT = 40;

    private static final int TAB_HEIGHT = 12;

    static final String openStr = Config.getString("pkgmgr.packagemenu.open");
    static final String removeStr = Config.getString("pkgmgr.packagemenu.remove");

    static final Color envOpColour = Config.ENV_COLOUR;

    static final BasicStroke normalStroke = new BasicStroke(1);
    static final BasicStroke selectedStroke = new BasicStroke(3);

    @OnThread(Tag.FXPlatform)
    private boolean isMoveable = true;

    public PackageTarget(Package pkg, String baseName)
    {
        super(pkg, baseName);

        Platform.runLater(() -> {setSize(calculateWidth(baseName), DEF_HEIGHT + TAB_HEIGHT);});
    }

    /**
     * Return the target's base name (ie the name without the package name). eg.
     * Target
     */
    public String getBaseName()
    {
        return getIdentifierName();
    }

    /**
     * Return the target's name, including the package name. eg. bluej.pkgmgr
     */
    public String getQualifiedName()
    {
        return getPackage().getQualifiedName(getBaseName());
    }

    @Override
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        super.load(props, prefix);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        props.put(prefix + ".type", "PackageTarget");
    }

    /**
     * Deletes applicable files (directory and ALL contentes) prior to this
     * PackageTarget being removed from a Package.
     */
    public void deleteFiles()
    {
        deleteDir(new File(getPackage().getPath(), getBaseName()));
    }

    /**
     * Delete a directory recursively.
     * This method will delete all files and subdirectories in any
     * directory without asking questions. Use with care.
     *
     * @param directory   The directory that will be deleted.
     */
    private void deleteDir(File directory)
    {
        File[] fileList = directory.listFiles();

        // If it is a file or an empty directory, delete
        if (fileList == null) {
            try{
                directory.delete();
            } catch (SecurityException se){
                Debug.message("Trouble deleting: "+directory+se);
            }
        }
        else {
            if (getPackage().getProject().prepareDeleteDir(directory)) {
                // delete all subdirectories
                for(int i=0;i<Array.getLength(fileList);i++) {
                    deleteDir(fileList[i]);
                }

                // then delete the directory (when it is empty)
                directory.delete();
            }
        }
    }
    
    /**
     * Called when a package icon in a GraphEditor is double clicked. Creates a
     * new PkgFrame when a package is drilled down on.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void doubleClick()
    {
        SwingUtilities.invokeLater(() -> {getPackage().getEditor().raiseOpenPackageEvent(this, getPackage().getQualifiedName(getBaseName()));});
    }

    /**
     * Disply the context menu.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void popupMenu(int x, int y, PackageEditor graphEditor)
    {
        ContextMenu menu = createMenu();
        if (menu != null) {
            menu.show(graphEditor, x, y);
        }
    }

    /**
     * Construct a popup menu which displays all our parent packages.
     */
    @OnThread(Tag.FXPlatform)
    private ContextMenu createMenu()
    {
        MenuItem open = new MenuItem(openStr);
        open.setOnAction(e -> SwingUtilities.invokeLater(() -> {
            getPackage().getEditor().raiseOpenPackageEvent(this, getPackage().getQualifiedName(getBaseName()));
        }));
        
        MenuItem remove = new MenuItem(removeStr);
        remove.setOnAction(e -> SwingUtilities.invokeLater(() -> {
            getPackage().getEditor().raiseRemoveTargetEvent(this);
        }));
        
        return new ContextMenu(open, remove);
    }
    
    @Override
    public void remove()
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
        String name = getQualifiedName();
        PkgMgrFrame[] f = PkgMgrFrame.getAllProjectFrames(pmf.getProject(), name);

        Platform.runLater(() ->
        {
            if (f != null)
            {
                DialogManager.showErrorFX(pmf.getFXWindow(), "remove-package-open");
            }
            else
            {
                // Check they realise that this will delete ALL the files.
                int response = DialogManager.askQuestionFX(pmf.getFXWindow(), "really-remove-package");

                // if they agree
                if (response == 0)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        deleteFiles();
                        getPackage().getProject().removePackage(getQualifiedName());
                        getPackage().removeTarget(this);
                    });
                }
            }
        });
    }

    /**
     * Removes the package associated with this target unconditionally.
     */
    public void removeImmediate()
    {
        deleteFiles();
        getPackage().removeTarget(this);
        getPackage().getProject().removePackage(getQualifiedName());
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setSize(int width, int height)
    {
        super.setSize(Math.max(width, MIN_WIDTH), Math.max(height, MIN_HEIGHT));
    }

    @OnThread(Tag.FXPlatform)
    public boolean isMoveable()
    {
        return isMoveable;
    }

    @OnThread(Tag.FXPlatform)
    public void setIsMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;
    }
}
