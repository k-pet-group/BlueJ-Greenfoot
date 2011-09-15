/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 
 
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

import java.io.Serializable;

/**
 * A compiler diagostic (error, warning, or other note)
 * 
 * @author Davin McCall
 */
public class Diagnostic implements Serializable
{
    public static int ERROR = 0;
    public static int WARNING = 1;
    public static int NOTE = 2;
    
    private int type;
    private String message;
    private String fileName;
    private long startLine;
    private long startColumn;
    private long endLine;
    private long endColumn;
    
    /**
     * Constructor for Diagnostic objects representing notes. 
     */
    public Diagnostic(int type, String message)
    {
        this.type = type;
        this.message = message;
    }
    
    /**
     * Constructor for error and warning diagnostics associated with
     * a particular position in the source code.
     * 
     * @param type  ERROR, WARNING or NOTE.
     * @param message  The diagnostic message
     * @param fileName The file associated with the diagnostic (might be null).
     * @param startLine  The line where the error/problem begins (less than 1 == unknown)
     * @param startColumn The column where the error/problem begins; must be valid if
     *                    {@code startLine} is greater than 0. Tab stops are every 8 spaces.
     * @param endLine    The line where the error/problem ends; must be valid if
     *                    {@code startLine} is greater than 0
     * @param endColumn  The column where the error/problem ends; must be valid if
     *                    {@code startLine} is greater than 0. Tab stops are every 8 spaces.
     */
    public Diagnostic(int type, String message, String fileName,
            long startLine, long startColumn, long endLine, long endColumn)
    {
        this.type = type;
        this.message = message;
        this.fileName = fileName;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
    
    /**
     * Get the type of the diagnostic - ERROR, WARNING or NOTE.
     */
    public int getType()
    {
        return type;
    }
    
    /**
     * Get the end column of the error. Return is valid only if {@code getStartLine()} returns
     * a valid line number (greater than 0). Caller should be prepared for this value to be slightly
     * inaccurate (it might extend past the actual end of the line). Tab stops are every
     * 8 spaces.
     */
    public long getEndColumn()
    {
        return endColumn;
    }
    
    /**
     * Get the end line of the error. Return is valid only if {@code getStartLine()} returns
     * a valid line number (greater than 0).
     */
    public long getEndLine()
    {
        return endLine;
    }
    
    /**
     * Set the diagnostic message (the message to be presented to the end user).
     */
    public void setMessage(String message)
    {
        this.message = message;
    }
    
    /**
     * Get the diagnostic message which can be presented to the end user.
     */
    public String getMessage()
    {
        return message;
    }
    
    /**
     * Get the starting column of the error/problem. Return is only valid if
     * {@code getStartLine()} returns a valid line (greater than 0). Tab stops
     * are every 8 spaces.
     */
    public long getStartColumn()
    {
        return startColumn;
    }
    
    /**
     * Get the starting line of the error/problem, if known. Return is valid if
     * it is greater than 0.
     */
    public long getStartLine()
    {
        return startLine;
    }
    
    /**
     * Get the filename associated with the error/problem. May be null.
     */
    public String getFileName()
    {
        return fileName;
    }
}
