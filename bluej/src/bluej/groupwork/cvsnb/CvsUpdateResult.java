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
package bluej.groupwork.cvsnb;

import bluej.groupwork.UnableToParseInputException;

/**
 * This class represents the result we get back from the server when doing an
 * update. These results are lines of text that has the form <br/>
 * statuscode filename<br/>
 * the status code can be any in {A,C,M,P,R,U,?)
 * 
 * @author fisker
 */
public class CvsUpdateResult
{
    char statusCode = 'X';
    String filename;
    public static final char ADDED = 'A';
    public static final char CONFLICT = 'C';
    public static final char MODIFIED = 'M';
    public static final char PATCHED = 'P';
    public static final char REMOVED = 'R';
    public static final char UPDATED = 'U';
    public static final char UNKNOWN = '?';
    
    /**
     * Create an UpdateResult with statusCode and filename
     * @param statusCode
     * @param filename
     */
    private CvsUpdateResult(char statusCode, String filename)
    {
        this.statusCode = statusCode;
        this.filename = filename;
    }
    
    /**
     * Parse a string and create an UpdateResult. Used to parse the strings
     * coming from an update command
     * @param str the String to parse
     * @return UpdateResult the resulting UpdateResult
     * @throws UnableToParseInputException
     */
    public static CvsUpdateResult parse(String str) throws UnableToParseInputException
    {
        char statusCode = 'X';
        String filename;
                
        boolean hasRightStructure = (str != null) && (str.length() > 3);
        boolean hasRightStatusCode = false;
        boolean messageOk;
        if (hasRightStructure){
            statusCode = str.charAt(0);
            hasRightStatusCode = statusCode == ADDED ||
            statusCode == CONFLICT || statusCode == MODIFIED ||
            statusCode == PATCHED || statusCode == REMOVED || 
            statusCode == UPDATED || statusCode == UNKNOWN;
        }
        messageOk = hasRightStructure && hasRightStatusCode;
        
        if (messageOk){
            filename = str.substring(2);
            return new CvsUpdateResult(statusCode, filename);
            //System.out.println("statusCode=" + statusCode + " filename=" + filename);
        }
        else {
            throw new UnableToParseInputException(str); 
        }
    }
    
    /**
     * Get the file name and path, relative to the project.
     */
    public String getFilename()
    {
        return filename;
    }
    /**
     * @return Returns the statusCode.
     */
    public char getStatusCode()
    {
        return statusCode;
    }
    
    public String toString()
    {
        return "statusCode: " + statusCode + " filename: " + filename;
    }
}
