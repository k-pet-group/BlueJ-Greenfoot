/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.List;

import org.netbeans.lib.cvsclient.command.FileInfoContainer;
import org.netbeans.lib.cvsclient.command.status.StatusInformation;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;



/**
 * This class is used for registering and storing cvs status request information. 
 *
 * @author bquig
 */
public class StatusServerResponse extends BasicServerResponse
{
    private List<StatusInformation> infoEvents;

    /**
     * Creates a new instance of StatusServerResponse
     */
    public StatusServerResponse()
    {
        infoEvents = new ArrayList<StatusInformation>();
    }

    /**
     * Keep each status info container so that it can be queried and used
     */
    public void fileInfoGenerated(FileInfoEvent infoEvent)
    {
        FileInfoContainer info = infoEvent.getInfoContainer();

        if (info instanceof StatusInformation) {
            infoEvents.add((StatusInformation) info);
        }
    }

    public List<StatusInformation> getStatusInformation()
    {
        return infoEvents;
    }
}
