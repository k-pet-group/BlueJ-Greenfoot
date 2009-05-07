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
package bluej.testmgr.record;

/**
 * Records a single user interaction with the 
 * object inspection mechanisms of BlueJ.
 * 
 * This record is for objects accessed through inspectors
 * (not currently working).
 *
 * @author  Andrew Patterson
 * @version $Id: ObjectInspectInvokerRecord.java 6312 2009-05-07 04:44:13Z davmac $
 */
public class ObjectInspectInvokerRecord extends InvokerRecord
{
    private String name;
    private InvokerRecord parentIr;
    private boolean isArray;

	/**
	 * Object inspection from an initial result.
	 * 
	 * @param type
	 * @param name
	 */    
    public ObjectInspectInvokerRecord(String name, boolean isArray)
    {
        this.name = name;
        //TODO create an ArrayInspectInvokerRecord instead?
        this.isArray = isArray;
    }

	/**
	 * Object inspection from another inspector.
	 * 
	 * @param type
	 * @param name
	 * @param ir
	 */
    public ObjectInspectInvokerRecord(String name,  boolean isArray, InvokerRecord ir)
    {
        this.name = name;
        this.isArray = isArray;
        this.parentIr = ir;
    }

    public String toFixtureDeclaration()
    {
        throw new UnsupportedOperationException();
    }
    
    public String toFixtureSetup()
    {
        throw new UnsupportedOperationException();
    }

	public String toTestMethod()
	{
        throw new UnsupportedOperationException();
	}
	
	@Override
    public String toExpression()
    {
	    if(parentIr instanceof MethodInvokerRecord) {
	        // This means we are inspecting a method result, and not a named object
	        // POLLE Maybe don't set the name and use that instead of instanceof
	        return parentIr.toExpression();
	    }
        else if(parentIr != null) {
            return parentIr.toExpression() + parentIr.getExpressionGlue() + name; 
        }
	    else {
	        return name;
	    }
	}

    @Override
    public String getExpressionGlue()
    {
        if(isArray) {
            return "";
        } else {
            return ".";
        }
    }

    public void incUsageCount()
    {
        if(parentIr != null) {
            parentIr.incUsageCount();     
        }
    }
    
}
