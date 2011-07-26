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

/**
 * An invoker record for "get" operations on array elements.
 * 
 * @author Davin McCall
 */
public class ArrayElementGetRecord extends InvokerRecord
{
    /** The invoker record for the inspector where the Get button was pressed, that resulted in the creation of this GetInvokerRecord. */
    private InvokerRecord parentIr;
    
    /** Index of the element to get */
    private int elementIndex;
    
    /** Type of the element */
    private String elementType;
    
    /** Name of the object as it appears on the object bench */
    private String objName;
    
    /** Type of the object (on the bench) */
    private String objType;


    public ArrayElementGetRecord(String elementType, int elementIndex, InvokerRecord parentIr)
    {
        this.parentIr = parentIr;
        this.elementType = elementType;
        this.elementIndex = elementIndex;
        parentIr.incUsageCount();
    }

    @Override
    public boolean hasVoidResult()
    {
        return false;
    }
    
    /**
     * Give this method invoker record a name on the object bench.
     * 
     * @param name Name of the object a it appears on the object bench.
     * @param type The type that the object is on the actual bench.
     */
    @Override
    public void setBenchName(String name, String type)
    {
        objName = name;
        objType = type;
    }

    /**
     * Construct a declaration for any objects constructed
     * by this invoker record.
     * 
     * @return a String representing the object declaration
     *         src or null if there is none.
     */    
    @Override
    public String toFixtureDeclaration(String firstIndent)
    {
        return firstIndent + fieldDeclarationStart + objType + " " + objName + statementEnd;    
    }

    /**
     * Construct a portion of an initialisation method for
     * this invoker record.
     *  
     * @return a String representing the object initialisation
     *         src or null if there is none. 
     */    
    @Override
    public String toFixtureSetup(String secondIndent)
    {
        return secondIndent + objName + " = " + toExpression() + statementEnd;          
    }

    /**
     * Construct a portion of a test method for this
     * invoker record.
     * 
     * @return a String representing the test method src
     */
    @Override
    public String toTestMethod(PkgMgrFrame pmf, String secondIndent)
    {
        return secondIndent + objType + " " + objName + " = " + toExpression() + statementEnd;
    }
    
    @Override
    public String toExpression()
    {
        if(! objType.equals(elementType)) {
            return "((" + objType + ") " + parentIr.toExpression() + "[" + elementIndex + "])";
        }
        else {
            return parentIr.toExpression() + "[" + elementIndex + "]";
        }
    }

}
