/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2016  Michael Kolling and John Rosenberg
 
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

    public static enum DiagnosticOrigin
    {
        JAVAC("javac"), STRIDE_EARLY("stride_early"), STRIDE_LATE("stride_late"), UNKNOWN("unknown");

        private String serverOrigin;

        private DiagnosticOrigin(String serverOrigin)
        {
            this.serverOrigin = serverOrigin;
        }
    }

    // The type: ERROR, WARNING or NOTE as above
    private final int type;
    // The diagnostic message
    private String message;
    // The file name (may be null), without path.
    private final String fileName;
    // Start line (begins at 1, but may be 0 or negative if N/A)
    private final long startLine;
    // Start column (begins at 1, but may be 0 or negative if N/A)
    private final long startColumn;
    // End line (begins at 1, but may be 0 or negative if N/A)
    private final long endLine;
    // End column (begins at 1, but may be 0 or negative if N/A)
    private final long endColumn;
    // The XPath location of the error.  Null if N/A.  Set after the constructor.
    private String xpath = null;
    // The start index within the XPath-located item.  Negative if N/A.  Set after the constructor.
    private int xmlStart = -1;
    // The start index within the XPath-located item.  Negative if N/A.  Set after the constructor.
    private int xmlEnd = -1;
    // The origin of the message, e.g. JAVAC
    private final DiagnosticOrigin origin;
    // The identifier of the diagnostic, used to tally up with later shown_error_message events.
    // May be -1 if it wasn't a compiler error with specific location
    private final int diagnosticIdentifier;

    
    /**
     * Constructor for Diagnostic objects representing notes. 
     */
    public Diagnostic(int type, String message)
    {
        this(type, message, null, -1, -1, -1, -1, DiagnosticOrigin.UNKNOWN, -1);
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
     * @param origin     The origin of the error message, e.g. "javac" or "stride_late".
     * @param identifier The identifier of the diagnostic.  Used to match up with later events
     *                   about the same diagnostic, such as shown_error_message events.
     */
    public Diagnostic(int type, String message, String fileName,
            long startLine, long startColumn, long endLine, long endColumn, DiagnosticOrigin origin, int identifier)
    {
        this.type = type;
        this.message = message;
        this.fileName = fileName;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.origin = origin;
        this.diagnosticIdentifier = identifier;
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
     * This can change because we try to make the message more helpful to the user,
     * e.g. by suggesting likely mis-spellings.
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

    /**
     * Gets the internal identifier of the diagnostic (unique during this session)
     */
    public int getIdentifier()
    {
        return diagnosticIdentifier;
    }

    public String getXPath()
    {
        return xpath;
    }

    /**
     * Sets the XPath, and start and end indexes within the item.
     */
    public void setXPath(String XPath, int xmlStart, int xmlEnd)
    {
        this.xpath = XPath;
        this.xmlStart = xmlStart;
        this.xmlEnd = xmlEnd;
    }

    public int getXmlStart()
    {
        return xmlStart;
    }

    public int getXmlEnd()
    {
        return xmlEnd;
    }

    public String getOrigin()
    {
        return origin.serverOrigin;
    }
}
