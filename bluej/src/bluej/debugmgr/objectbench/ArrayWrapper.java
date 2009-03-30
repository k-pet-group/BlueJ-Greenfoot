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
package bluej.debugmgr.objectbench;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import bluej.debugger.*;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;

/**
 * A wrapper around array objects.
 * 
 * The array wrapper is represented by a few red ovals that are visible on the
 * object bench.
 * 
 * @author Andrew Patterson
 * @author Bruce Quig
 * @version $Id: ArrayWrapper.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ArrayWrapper extends ObjectWrapper
{
    public static int WORD_GAP = 8;
    public static int SHADOW_SIZE = 3;
    public static int ARRAY_GAP = 3;
    
    

    public ArrayWrapper(PkgMgrFrame pmf, ObjectBench ob, DebuggerObject obj, String instanceName)
    {
        super(pmf, ob, obj, obj.getGenType(), instanceName);
    }

    /**
     * Creates the popup menu structure by parsing the object's class
     * inheritance hierarchy.
     * 
     * @param className
     *            class name of the object for which the menu is to be built
     */
    protected void createMenu(String className)
    {
        menu = new JPopupMenu(getName());
        JMenuItem item;

        //        item.addActionListener(
        //            new ActionListener() {
        //                public void actionPerformed(ActionEvent e) {
        // /*invokeMethod(e.getSource());*/ }
        //           });

        // add inspect and remove options
        menu.add(item = new JMenuItem(inspect));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                inspectObject();
            }
        });
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);

        menu.add(item = new JMenuItem(remove));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                removeObject();
            }
        });
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);

        add(menu);
    }

    /**
     * draw a UML style object (array) instance
     */
    protected void drawUMLStyle(Graphics2D g)
    {
        g.setFont(PrefMgr.getStandardFont());

        drawUMLObjectShape(g, HGAP + ARRAY_GAP*2, (VGAP / 2) + ARRAY_GAP*2, WIDTH - 10, HEIGHT - 10,  SHADOW_SIZE, 8);
        drawUMLObjectShape(g, HGAP + ARRAY_GAP, (VGAP / 2) + ARRAY_GAP, WIDTH - 10, HEIGHT - 10,  SHADOW_SIZE, 8);
        drawUMLObjectShape(g, HGAP, (VGAP / 2), WIDTH - 10, HEIGHT - 10,  SHADOW_SIZE, 8);

        drawUMLObjectText(g, HGAP, (VGAP / 2), WIDTH - 10, 3, getName() + ":", displayClassName);

    }
}
