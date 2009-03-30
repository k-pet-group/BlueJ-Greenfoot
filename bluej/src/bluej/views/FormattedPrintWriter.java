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
package bluej.views;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 ** @version $Id: FormattedPrintWriter.java 6215 2009-03-30 13:28:25Z polle $
 ** @author Michael Cahill
 **
 ** FormattedPrintWriter - provides formatting on top of a PrintWriter
 **/
public abstract class FormattedPrintWriter extends PrintWriter
{
	public FormattedPrintWriter(Writer out)
	{
		super(out);
	}
	
	public FormattedPrintWriter(Writer out, boolean autoFlush)
	{
		super(out, autoFlush);
	}
	
	public FormattedPrintWriter(OutputStream out)
	{
		super(out);
	}
	
	public FormattedPrintWriter(OutputStream out, boolean autoFlush)
	{
		super(out, autoFlush);
	}
	
	boolean bold = false;
	public void setBold(boolean bold)
	{
		if(this.bold == bold)
			return;	// nothing to do

		if(bold)
			startBold();
		else
			endBold();
			
		this.bold = bold;
	}
	protected abstract void startBold();
	protected abstract void endBold();
	
	boolean italic = false;
	public void setItalic(boolean italic)
	{
		if(this.italic == italic)
			return;	// nothing to do

		if(italic)
			startItalic();
		else
			endItalic();
			
		this.italic = italic;
	}
	protected abstract void startItalic();
	protected abstract void endItalic();

	protected abstract void indentLine();
}
