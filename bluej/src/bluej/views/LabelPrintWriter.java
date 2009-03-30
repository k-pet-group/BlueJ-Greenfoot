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

import bluej.utility.MultiLineLabel;
import java.awt.Label;

/**
 ** @version $Id: LabelPrintWriter.java 6215 2009-03-30 13:28:25Z polle $
 ** @author Michael Cahill
 **
 ** LabelPrintWriter - create a MultiLineLabel containing the output to a
 ** 	FormattedPrintWriter
 **/
public class LabelPrintWriter extends FormattedPrintWriter
{
	MultiLineLabel label;
	
	public LabelPrintWriter(int align)
	{
		// PrintWriter needs to be passed a valid outputstream
		// even if we are going to not actually print to it.
		// We pass it the standard System output stream
		super(System.out);

		label = new MultiLineLabel(align);
	}
	
	public LabelPrintWriter()
	{
		this(Label.LEFT);
	}
	
	public MultiLineLabel getLabel()
	{
		return label;
	}
	
	protected void startBold()
	{
		label.setBold(true);
	}
	
	protected void endBold()
	{
		label.setBold(false);
	}
	
	protected void startItalic()
	{
		label.setItalic(true);
	}

	protected void endItalic()
	{
		label.setItalic(false);
	}

	protected void indentLine()
	{
		label.addText("\t");
	}
	
	public void println(String str)
	{
		label.addText(str);
	}
}
