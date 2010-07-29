/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 
 
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
package bluej.testmgr.record;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Records a single user interaction with the 
 * object inspection mechanisms of BlueJ.
 * 
 * This record is for objects accessed through inspectors
 * (not currently working).
 *
 * @author  Andrew Patterson
 */
public class ObjectInspectInvokerRecord extends InvokerRecord
{
    private String name;
    private InvokerRecord parentIr;

    /**
     * Object inspection from an initial result.
     * 
     * @param type
     * @param name
     */    
    public ObjectInspectInvokerRecord(String name)
    {
        this.name = name;
    }

    /**
     * Object inspection from another inspector.
     * 
     * @param type
     * @param name
     * @param ir
     */
    public ObjectInspectInvokerRecord(String name, InvokerRecord ir)
    {
        this.name = name;
        this.parentIr = ir;
    }

    public String toFixtureDeclaration()
    {
        return null;
    }
    
    public String toFixtureSetup()
    {
        return null;
    }

    public String toTestMethod(PkgMgrFrame pmf)
    {
        return null;
    }

    @Override
    public String toExpression()
    {
        if(parentIr != null) {
            return parentIr.toExpression() + "." + name; 
        }
        else {
            return name;
        }
    }

    public void incUsageCount()
    {
        if(parentIr != null) {
            parentIr.incUsageCount();     
        }
    }
    
}
