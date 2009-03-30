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

import java.util.List;

/**
 ** 
 ** 
 **
 ** @author Michael Kolling
 **/

public final class ExceptionDescription
{
    private String className;
    private String text;
    private List stack;

    public ExceptionDescription(String className, String text, 
                                List stack)
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
     * Return the file the exception was thrown from.
     */    
    //     public String getSourceFile()
    //     {
    // 	return sourceFile;
    //     }

    //     /**
    //      * Return the line number in the source file where this exception was
    //      * thrown.
    //      */
    //     public int getLineNumber()
    //     {
    // 	return lineNumber;
    //     }

    /**
     * Return the stack (a list of SourceLocation objects) for the exception
     * location. Element 0 in the list is the current frame, higher numbers
     * are caller frames.
     */
    public List getStack()
    {
        return stack;
    }

    public String toString()
    {
        return className + ": " + text;
    }

}
