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

import java.io.File;

/**
 * An interface for teamwork providers - CVS, Subversion
 * 
 * @author Davin McCall
 */
public interface TeamworkProvider
{
    /**
     * Get the name of this provider ("CVS", "Subversion", etc)
     */
    public String getProviderName();
    
    /**
     * Get a list of the different protocols this provider supports (as human-
     * readable strings, not necessarily the same as what appears in the
     * repository url)
     */
    public String [] getProtocols();
    
    /**
     * Get the protocol string used internally to represent the given protocol
     * @param protocol  an index into the array returned by getProviderName()
     */
    public String getProtocolKey(int protocol);
    
    /**
     * Get the label for a given protocol key.
     */
    public String getProtocolLabel(String protocolKey);
    
    /**
     * Check that supplied information can be used to connect to a repository.
     * This might take some time to execute.
     */
    public TeamworkCommandResult checkConnection(TeamSettings settings);
    
    /**
     * Get a repository from the given settings
     */
    public Repository getRepository(File projectDir, TeamSettings settings);
}
