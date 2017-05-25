/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2017  Michael Kolling and John Rosenberg
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Represents the result of a teamwork command.<p>
 * 
 * This base class represents the default case - no error.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class TeamworkCommandResult
{
    /**
     * Did an error occur during the processing of the command?
     * This includes authentication problems and command cancellation.
     */
    public boolean isError()
    {
        return false;
    }
    
    /**
     * Did the command fail due to authentication failure - invalid
     * username/password?
     */
    public boolean wasAuthFailure()
    {
        return false;
    }
    
    /**
     * Was the command aborted? (in this case, isError/wasAuthFailure will both
     * return false).
     */
    public boolean wasAborted()
    {
        return false;
    }
    
    /**
     * Get the error message explaining the problem that occurred.
     */
    public String getErrorMessage()
    {
        return null;
    }
}
