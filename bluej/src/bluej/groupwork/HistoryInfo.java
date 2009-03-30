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
package bluej.groupwork;

/**
 * Represents a single element of history information. This includes:
 * Filename, revision, date, user, comment
 * 
 * @author Davin McCall
 * @version $Id: HistoryInfo.java 6215 2009-03-30 13:28:25Z polle $
 */
public class HistoryInfo
{
    private String [] files;
    private String revision;
    private String date;
    private String user;
    private String comment;
    
    public HistoryInfo(String [] files, String revision, String date, String user, String comment)
    {
        this.files = files;
        this.revision = revision;
        this.date = date;
        this.user = user;
        this.comment = comment;
    }
    
    public String [] getFiles()
    {
        return files;
    }
    
    public String getRevision()
    {
        return revision;
    }
    
    public String getDate()
    {
        return date;
    }
    
    public String getUser()
    {
        return user;
    }
    
    public String getComment()
    {
        return comment;
    }
}
