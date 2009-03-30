/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.debugger.DebuggerClass;
import bluej.debugmgr.inspector.InspectorManager;

/**
 * Action to inspect a class.
 * 
 * @author Poul Henriksen
 */
public class InspectClassAction extends AbstractAction
{
    private InspectorManager inspectorManager;
    private DebuggerClass cls;
    private Package pkg;
    private JFrame frame;
    
    public InspectClassAction(DebuggerClass cls, Package pkg, InspectorManager inspMan, JFrame parent) 
    {
        super(Config.getString("inspect.class"));
        inspectorManager = inspMan;
        this.pkg = pkg;
        this.cls = cls;
        this.frame = parent;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        inspectorManager.getClassInspectorInstance(cls, pkg, frame);
    }
}
