/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017,2018  Michael Kolling and John Rosenberg 

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
package bluej.groupwork.ui;

import bluej.groupwork.TeamStatusInfo;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A ListView element that can show either a file status or a message string.
 *
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class UpdateStatus
{
    /** file status, if available (may be null) */
    public final TeamStatusInfo infoStatus;
    /** message status, if available (may be null) */
    public final String stringStatus;

    public UpdateStatus(TeamStatusInfo infoStatus)
    {
        this.infoStatus = infoStatus;
        this.stringStatus = null;
    }

    public UpdateStatus(String stringStatus)
    {
        this.stringStatus = stringStatus;
        this.infoStatus = null;
    }
}
