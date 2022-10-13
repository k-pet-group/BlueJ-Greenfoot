/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012  Michael Kolling and John Rosenberg 
 
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
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls with no result.
 *
 * @author  Andrew Patterson
 */
public class VoidMethodInvokerRecord extends InvokerRecord
{
    protected String command;
    private String [] argumentValues;
    
    public VoidMethodInvokerRecord(String command, String [] argVals)
    {
        this.command = command;
        this.argumentValues = argVals;
    }
    
    @Override
    public String [] getArgumentValues()
    {
        return argumentValues;
    }
    
    @Override
    public boolean hasVoidResult()
    {
        return true;
    }

    /**
     * Construct a declaration for any objects constructed
     * by this invoker record.
     * 
     * @return null because a void method results in no objects
     */
    @Override
    public String toFixtureDeclaration(String firstIndent)
    {
        return null;
    }
    
    /**
     * Construct a portion of an initialisation method for
     * this invoker record.
     *  
     * @return a String reprenting the object initialisation
     *         src or null if there is none. 
     */    
    @Override
    @OnThread(Tag.FXPlatform)
    public String toFixtureSetup(String secondIndent)
    {
        // code for the fixture setup involves just inserting the method call
        return secondIndent + command + statementEnd;
    }

    @Override
    public String toTestMethod(PkgMgrFrame pmf, String secondIndent)
    {
        // code for the test method involves just inserting the method call
        return secondIndent + command + statementEnd;
    }

    @Override
    public String toExpression()
    {
        return command;
    }

    @Override
    public String getOriginalCommand()
    {
        return command;
    }
    
    
}
