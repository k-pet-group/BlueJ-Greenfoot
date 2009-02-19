/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
 * Records a call to a programs static void main()
 * entry point. Because this does not return a result
 * and tests should not depend on side effects from
 * static statements, we create a record for this
 * but the record does not result in any unit test
 * code being created.
 *
 * @author  Andrew Patterson
 * @version $Id: StaticVoidMainMethodInvokerRecord.java 6164 2009-02-19 18:11:32Z polle $
 */
public class StaticVoidMainMethodInvokerRecord extends InvokerRecord
{
    public StaticVoidMainMethodInvokerRecord()
    {
    }

    public String toFixtureDeclaration()
    {
        return null;
    }
    
    public String toFixtureSetup()
    {
        return null;
    }

	public String toTestMethod()
	{
        return null;
	}

    @Override
    public String toExpression()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }

    @Override
    public String getExpressionGlue()
    {
        throw new RuntimeException("Method not implemented for this type.");
    }
}
