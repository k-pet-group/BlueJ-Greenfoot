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
package bluej.debugger;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * A description of an exception.
 *
 * @author Michael Kolling
 */
@OnThread(Tag.Any)
public final class ExceptionDescription
{
    private String className;
    private String text;
    private List<SourceLocation> stack;

    /**
     * Construct an exception description.
     * 
     * @param className  The name of the exception class
     * @param text       The exception message
     * @param stack      The stack trace for the exception
     */
    public ExceptionDescription(String className, String text, 
                                List<SourceLocation> stack)
    {
        this.className = className;
        this.text = text;
        this.stack = stack;
    }

    public ExceptionDescription(String text)
    {
        this.className = null;
        this.text = text;
        this.stack = null;
    }

    /**
     * Return the name of the exception class.
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * Return the text of the exception.
     */    
    public String getText()
    {
        return text;
    }

    /**
     * Return the stack (a list of SourceLocation objects) for the exception
     * location. Element 0 in the list is the current frame, higher numbers
     * are caller frames.
     */
    public List<SourceLocation> getStack()
    {
        return stack;
    }

    public String toString()
    {
        return className + ": " + text;
    }

}
