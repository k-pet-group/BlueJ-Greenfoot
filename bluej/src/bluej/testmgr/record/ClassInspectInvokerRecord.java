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
 * Records a single user interaction with the 
 * class inspection mechanisms of BlueJ.
 * 
 * This record is for classes accessed through inspectors
 * (not currently working).
 *
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassInspectInvokerRecord.java 6164 2009-02-19 18:11:32Z polle $
 *
 */
public class ClassInspectInvokerRecord extends InvokerRecord
{   
    private String className;

    /**
     * Class inspection 
     * 
     * @param className name of the class
     */    
    public ClassInspectInvokerRecord(String className)
    {
        this.className = className;        
    }   

    public String toFixtureDeclaration()
    {
        return null;
    }
    
    public String toFixtureSetup()
    {
        return secondIndent + className + statementEnd;
    }

    public String toTestMethod()
    {
        return firstIndent + className + statementEnd;
    }
    
    @Override
    public String toExpression()
    {
        return className;       
    }
    

    @Override
    public String getExpressionGlue()
    {
        return ".";
    }

}
