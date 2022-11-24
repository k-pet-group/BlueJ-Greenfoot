/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2016  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr.inspector;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;

import javafx.scene.Node;
import javafx.stage.Window;


/**
 * 
 * Interface for a manager that creates and holds references to inspectors
 * 
 * @author Poul Henriksen
 */
public interface InspectorManager
{
    /**
     * 
     * Remove and dispose the inspector.
     * 
     * @param obj Object that the inspector inspects
     */
    public void removeInspector(DebuggerObject obj);
    
    /**
     * 
     * Remove and dispose the inspector.
     * 
     * @param cls Class that the inspector inspects
     */
    public void removeInspector(DebuggerClass cls);
    
    /**
     * Return an ObjectInspector for an object. The inspector is visible.
     *
     * @param info
     *            The information about the the expression that gave this result
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param parent
     *            The parent frame of this frame
     * @param animateFromCentre
     * @return The Viewer value
     */
    public ObjectInspector getInspectorInstance(DebuggerObject obj,
                                                String name, Package pkg, InvokerRecord ir, Window parent, Node animateFromCentre);
    
    /**
     * Return a ClassInspector for a class. The inspector is visible.
     *
     * @param name
     *            The name of this object or "null" if it is not on the object
     *            bench
     * @param getEnabled
     *            if false, the "get" button is permanently disabled
     * @param clss
     *            The class displayed by this viewer
     * @param pkg
     *            The package all this belongs to
     * @param parent
     *            The parent frame of this frame
     * @param animateFromCentre
     * @return The Viewer value
     */
    public ClassInspector getClassInspectorInstance(DebuggerClass clss,
                                                    Package pkg, Window parent, Node animateFromCentre);

    /**
     * Return an ObjectInspector for an object. The inspector is visible.
     * 
     * @param obj The object displayed by this viewer
     * @param name The name of this object or "null" if the name is unobtainable
     * @param pkg The package all this belongs to
     * @param ir the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param info The information about the the expression that gave this
     *            result
     * @param parent The parent frame of this frame
     * @return The Viewer value
     */
    public ResultInspector getResultInspectorInstance(DebuggerObject obj,
        String name, Package pkg, InvokerRecord ir, ExpressionInformation info,
        Window parent);
    
    /**
     * Whether we are in testing mode. If true, the inspectors should show testing stuff.
     * 
     */
    public boolean inTestMode();
}
