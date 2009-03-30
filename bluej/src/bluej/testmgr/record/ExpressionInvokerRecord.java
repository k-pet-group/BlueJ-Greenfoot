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

import bluej.utility.JavaUtils;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls with no result.
 *
 * @author  Bruce Quig
 * @version $Id: ExpressionInvokerRecord.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ExpressionInvokerRecord extends MethodInvokerRecord 
{
    
	/**
	 * @param command
	 */
	public ExpressionInvokerRecord(String command) 
    {
		super(JavaUtils.getJavaUtils().genTypeFromClass(Object.class), command, null, null);
	}
    
    /**
     * @return A string representing the assignment statement
     *         
     */
    protected String benchAssignmentTypecast()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append(benchName);
        sb.append(" = ");
        sb.append(command);

        return sb.toString();
    }   

}
