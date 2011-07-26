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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * From an existing unit test we create an invoker record
 * that represents all the existing fixture declarations
 * and setup code.
 * 
 * @author  Andrew Patterson
 */
public class ExistingFixtureInvokerRecord extends InvokerRecord
{
    private List<String> fieldsSrc;
    private String setUpSrc;
    
    /**
     * Records a method call that returns a result to the user.
     * 
     * @param returnType  the Class of the return type of the method
     * @param command     the method statement to execute
     */
    public ExistingFixtureInvokerRecord()
    {
        fieldsSrc = new ArrayList<String>();
    }
    
    @Override
    public boolean hasVoidResult()
    {
        return false;
    }

    public void addFieldDeclaration(String fieldDecl)
    {
        fieldsSrc.add(fieldDecl);
    }
    
    public void setSetupMethod(String setupMethodSrc)
    {
        setUpSrc = setupMethodSrc;
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
        StringBuffer sb = new StringBuffer();

        ListIterator<String> it = fieldsSrc.listIterator();
                
        while(it.hasNext()) {
            String fieldDecl = (String) it.next();
                    
            sb.append(firstIndent);
            sb.append(fieldDecl);
            sb.append('\n');
        }

        return sb.toString();
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
        StringBuffer sb = new StringBuffer();
        sb.append(secondIndent);
        sb.append(setUpSrc);
        sb.append('\n');

        return sb.toString();
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
        return null;
    }

    @Override
    public String toExpression()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }
}
