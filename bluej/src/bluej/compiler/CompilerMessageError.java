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
package bluej.compiler;

/**
 ** @version $Id: CompilerMessageError.java 6163 2009-02-19 18:09:55Z polle $
 ** @author Michael Cahill
 ** CompilerMessageError - thrown when there is a compiler error.
 **/

public class CompilerMessageError extends Error
{
	String filename;
	int lineNo;
	String message;
	
	public CompilerMessageError(String filename, int lineNo, String message)
	{
		super(filename + ":" + lineNo + ":" + message);
		
		this.filename = filename;
		this.lineNo = lineNo;
		this.message = message;
	}
	
	public String getFilename()
	{
		return filename;
	}
	
	public int getLineNo()
	{
		return lineNo;
	}
	
	public String getMessage()
	{
		return message;
	}
}
