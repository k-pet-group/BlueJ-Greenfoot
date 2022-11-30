/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011  Michael Kolling and John Rosenberg 
 
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
 * Records a single user interaction with the object construction
 * mechanisms of BlueJ.
 *
 * @author  Andrew Patterson
 */
public class ConstructionInvokerRecord extends InvokerRecord
{
    private String type;
    private String name;
    private String command;
    private String [] argumentValues;
    
    public ConstructionInvokerRecord(String type, String name, String command, String [] argVals)
    {
        this.type = type;
        this.name = name;
        this.command = command;
        this.argumentValues = argVals;
    }
    
    @Override
    public boolean hasVoidResult()
    {
        return false;
    }
    
    @Override
    public String [] getArgumentValues()
    {
        return argumentValues;
    }

    @Override
    public String getResultName()
    {
        return name;
    }
    
    @Override
    public String getResultTypeString()
    {
        return type;
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
        return firstIndent + fieldDeclarationStart + type + " " + name + statementEnd;       
    }

    /**
     * Construct a portion of an initialisation method for
     * this invoker record.
     *  
     * @return a String reprenting the object initialisation
     *         src or null if there is none. 
     */
    @Override
    public String toFixtureSetup(String secondIndent)
    {
        return secondIndent + name + " = " + command + statementEnd;          
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
        return secondIndent + type + " " + name + " = " + command + statementEnd;
    }

    @Override
    public String toExpression()
    {
        return command;
    }
}
