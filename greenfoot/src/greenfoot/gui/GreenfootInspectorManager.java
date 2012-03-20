/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011, 2012  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import greenfoot.gui.inspector.GreenfootClassInspector;
import greenfoot.gui.inspector.GreenfootObjectInspector;
import greenfoot.gui.inspector.GreenfootResultInspector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.ClassInspectInvokerRecord;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.DialogManager;

/**
 * An inspector manager for Greenfoot projects.
 * 
 * @author Davin McCall
 */
public class GreenfootInspectorManager implements InspectorManager
{
    /** This holds all object inspectors for a world. */
    private Map<DebuggerObject, Inspector> objectInspectors = new HashMap<DebuggerObject, Inspector> ();
    /** This holds all class inspectors for a world. */
    private Map<String, Inspector> classInspectors = new HashMap<String, Inspector> ();

    @Override
    public void removeInspector(DebuggerObject obj)
    {
        objectInspectors.remove(obj);
    }

    @Override
    public void removeInspector(DebuggerClass cls)
    {
        classInspectors.remove(cls.getName());
    }

    @Override
    public ObjectInspector getInspectorInstance(DebuggerObject obj,
            String name, Package pkg, InvokerRecord ir, JFrame parent)
    {
        ObjectInspector inspector = (ObjectInspector) objectInspectors.get(obj);
        
        if (inspector == null) {
            inspector = new GreenfootObjectInspector(obj, this, name, pkg, ir, parent);
            objectInspectors.put(obj, inspector);
            inspector.setVisible(true);
        }
        else {
            inspector.update();
            inspector.updateLayout();
            inspector.setVisible(true);
            inspector.bringToFront();
        }
        
        return inspector;
    }

    @Override
    public ClassInspector getClassInspectorInstance(DebuggerClass clss,
            Package pkg, JFrame parent)
    {
        ClassInspector inspector = (ClassInspector) classInspectors.get(clss.getName());

        if (inspector == null) {
            ClassInspectInvokerRecord ir = new ClassInspectInvokerRecord(clss.getName());
            inspector = new GreenfootClassInspector(clss, this, pkg, ir, parent);
            classInspectors.put(clss.getName(), inspector);
        } else {
            inspector.update();
            inspector.updateLayout();
        }
        
        inspector.setVisible(true);
        inspector.bringToFront();

        return inspector;
    }

    @Override
    public ResultInspector getResultInspectorInstance(DebuggerObject obj,
            String name, Package pkg, InvokerRecord ir,
            ExpressionInformation info, JFrame parent)
    {
        ResultInspector inspector = (ResultInspector) objectInspectors.get(obj);
        
        if (inspector == null) {
            inspector = new GreenfootResultInspector(obj, this, name, pkg, ir, info);
            objectInspectors.put(obj, inspector);
            DialogManager.centreWindow(inspector, parent);
            inspector.setVisible(true);
        }
        else {
            inspector.update();
            inspector.updateLayout();
            inspector.setVisible(true);
            inspector.bringToFront();
        }

        return inspector;
    }

    @Override
    public boolean inTestMode()
    {
        // Greenfoot does not support testing:
        return false;
    }

    /**
     * Removes all inspector instances for this project.
     * This is used when VM is reset or the project is recompiled.
     */
    public void removeAllInspectors()
    {
        for (Iterator<Inspector> it = objectInspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }
        objectInspectors.clear();
        
        for (Iterator<Inspector> it = classInspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }
        classInspectors.clear();
    }
}
