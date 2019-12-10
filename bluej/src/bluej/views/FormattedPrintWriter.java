/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016,2019  Michael Kolling and John Rosenberg
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 ** @author Michael Cahill
 **
 ** FormattedPrintWriter - provides formatting on top of a PrintWriter
 **/
@OnThread(value = Tag.FXPlatform,ignoreParent = true)
public abstract class FormattedPrintWriter extends PrintWriter
{
    protected enum ColorScheme
    {
        DEFAULT,
        GRAY
    }

    protected enum SizeScheme
    {
        SMALL,
        DEFAULT,
        LARGE
    }
    public FormattedPrintWriter(OutputStream out)
    {
        super(out);
    }

    public abstract void setBold(boolean bold);
    public abstract void setItalic(boolean italic);
    protected abstract void setColor(ColorScheme color);
    protected abstract void setSize(SizeScheme size);
    protected abstract void indentLine();
    
    public abstract void println(String line);
}
