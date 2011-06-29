/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.inspector;

import greenfoot.util.GreenfootUtil;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;

/**
 * Result inspector for Greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GreenfootResultInspector extends ResultInspector
{
    /**
     * Construct a result inspector.
     */
    public GreenfootResultInspector(DebuggerObject obj, InspectorManager inspectorManager, String name, Package pkg,
            InvokerRecord ir, ExpressionInformation info)
    {
        super(obj, inspectorManager, name, pkg, ir, info);
        GreenfootUtil.makeGreenfootTitle(GreenfootResultInspector.this);
    }

    /**
     * Whether the Get button should be enabled.
     * 
     * @return True if the selected object is an actor
     */
    @Override
    protected boolean isGetEnabled()
    {
        return GreenfootInspector.isGetEnabled(selectedField);
    }

    /**
     * The "Get" button was pressed. Start dragging the selected object.
     */
    @Override
    protected void doGet()
    {
        GreenfootInspector.doGet(selectedField);
    }

}
