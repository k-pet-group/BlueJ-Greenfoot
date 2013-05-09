/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013  Michael Kolling and John Rosenberg 
 
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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.graph.GraphEditor;
import bluej.graph.Moveable;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;

/**
 * A sub package (or parent package)
 * 
 * @author Michael Cahill
 */
public class PackageTarget extends Target
    implements Moveable
{
    static final int MIN_WIDTH = 60;
    static final int MIN_HEIGHT = 40;

    private static final int TAB_HEIGHT = 12;

    static String openStr = Config.getString("pkgmgr.packagemenu.open");
    static String removeStr = Config.getString("pkgmgr.packagemenu.remove");

    static final Color envOpColour = Config.ENV_COLOUR;

    static final BasicStroke normalStroke = new BasicStroke(1);
    static final BasicStroke selectedStroke = new BasicStroke(3);

    private int ghostX;
    private int ghostY;
    private int ghostWidth;
    private int ghostHeight;
    private boolean isDragging;
    private boolean isMoveable = true;

    public PackageTarget(Package pkg, String baseName)
    {
        super(pkg, baseName);

        setSize(calculateWidth(baseName), DEF_HEIGHT + TAB_HEIGHT);
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
    public void doubleClick(MouseEvent evt)
    {
        getPackage().getEditor().raiseOpenPackageEvent(this, getPackage().getQualifiedName(getBaseName()));
    }

    /**
     * Disply the context menu.
     */
    @Override
    public void popupMenu(int x, int y, GraphEditor graphEditor)
    {
        JPopupMenu menu = createMenu();
        if (menu != null) {
            menu.show(graphEditor, x, y);
        }
    }

    /**
     * Construct a popup menu which displays all our parent packages.
     */
    private JPopupMenu createMenu()
    {
        JPopupMenu menu = new JPopupMenu(getBaseName());

        Action openAction = new OpenAction(openStr, this, getPackage().getQualifiedName(getBaseName()));
        addMenuItem(menu, openAction);
        
        Action removeAction = new RemoveAction(removeStr, this);
        addMenuItem(menu, removeAction);

        return menu;
    }

    private void addMenuItem(JPopupMenu menu, Action action)
    {
        JMenuItem item = menu.add(action);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
    }

    private class OpenAction extends AbstractAction
    {
        private Target t;
        private String pkgName;

        public OpenAction(String menu, Target t, String pkgName)
        {
            super(menu);
            this.t = t;
            this.pkgName = pkgName;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getPackage().getEditor().raiseOpenPackageEvent(t, pkgName);
        }
    }

    private class RemoveAction extends AbstractAction
    {
        private Target t;

        public RemoveAction(String menu, Target t)
        {
            super(menu);
            this.t = t;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getPackage().getEditor().raiseRemoveTargetEvent(t);
        }
    }

    @Override
    public void remove()
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
        if (pmf.askRemovePackage(this)) {
            deleteFiles();
            getPackage().getProject().removePackage(getQualifiedName());
            getPackage().removeTarget(this);
        }
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
    public void setSize(int width, int height)
    {
        super.setSize(Math.max(width, MIN_WIDTH), Math.max(height, MIN_HEIGHT));
        setGhostSize(0, 0);
    }

    @Override
    public void setPos(int x, int y)
    {
        super.setPos(x, y);
        setGhostPosition(0, 0);
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostX()
    {
        return ghostX;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostY()
    {
        return ghostY;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostWidth()
    {
        return ghostWidth;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostHeight()
    {
        return ghostHeight;
    }

    /**
     * Set the position of the ghost image given a delta to the real size.
     */
    public void setGhostPosition(int deltaX, int deltaY)
    {
        this.ghostX = getX() + deltaX;
        this.ghostY = getY() + deltaY;
    }

    /**
     * Set the size of the ghost image given a delta to the real size.
     */
    public void setGhostSize(int deltaX, int deltaY)
    {
        ghostWidth = Math.max(getWidth() + deltaX, MIN_WIDTH);
        ghostHeight = Math.max(getHeight() + deltaY, MIN_HEIGHT);
    }

    /**
     * Set the target's position to its ghost position.
     */
    public void setPositionToGhost()
    {
        super.setPos(ghostX, ghostY);
        setSize(ghostWidth, ghostHeight);
        isDragging = false;
    }

    /** 
     * Ask whether we are currently dragging. 
     */
    public boolean isDragging()
    {
        return isDragging;
    }

    /**
     * Set whether or not we are currently dragging this class
     * (either moving or resizing).
     */
    public void setDragging(boolean isDragging)
    {
        this.isDragging = isDragging;
    }

    @Override
    public boolean isMoveable()
    {
        return isMoveable;
    }

    @Override
    public void setIsMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;
    }

    @Override
    public String getTooltipText()
    {
        return getIdentifierName();
    }
}
