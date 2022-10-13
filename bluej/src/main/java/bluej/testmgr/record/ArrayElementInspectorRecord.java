/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2011  Michael Kolling and John Rosenberg 
 
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

public class ArrayElementInspectorRecord extends InvokerRecord
{
    private InvokerRecord parentIr;
    private int element;
    
    public ArrayElementInspectorRecord(InvokerRecord parentIr, int element)
    {
        this.parentIr = parentIr;
        this.element = element;
    }
    
    @Override
    public boolean hasVoidResult()
    {
        return false;
    }    
    
    @Override
    public String toExpression()
    {
        return parentIr.toExpression() + "[" + element + "]";
    }

    @Override
    public String toFixtureDeclaration(String firstIndent)
    {
        return null;
    }

    @Override
    public String toFixtureSetup(String secondIndent)
    {
        return null;
    }

    @Override
    public String toTestMethod(PkgMgrFrame pmf, String secondIndent)
    {
        return null;
    }
}
