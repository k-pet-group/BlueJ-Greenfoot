/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017  Michael Kolling and John Rosenberg
 
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
 * A class to represent team settings
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class TeamSettings
{
    private TeamworkProvider provider;
    private String protocol;
    private String server;
    private String prefix;
    private String group;
    private String username;
    private String password;
    private String yourName;
    private String yourEmail;
    
    public TeamSettings(TeamworkProvider provider, String protocol, String server,
            String prefix, String group, String username, String password)
    {
        this.provider = provider;
        this.protocol = protocol;
        this.server = server;
        this.prefix = prefix;
        this.group = group;
        this.username = username;
        this.password = password;
    }
    
    public TeamworkProvider getProvider()
    {
        return provider;
    }
    
    public String getProtocol()
    {
        return protocol;
    }
    
    public String getServer()
    {
        return server;
    }
    
    public String getPrefix()
    {
        return prefix;
    }
    
    public String getGroup()
    {
        return group;
    }
    
    public String getUserName()
    {
        return username;
    }
    
    public String getPassword()
    {
        return password;
    }

    /**
     * @return the yourName
     */
    public String getYourName()
    {
        return yourName;
    }

    /**
     * @param yourName the yourName to set
     */
    public void setYourName(String yourName)
    {
        this.yourName = yourName;
    }

    /**
     * @return the yourEmail
     */
    public String getYourEmail()
    {
        return yourEmail;
    }

    /**
     * @param yourEmail the yourEmail to set
     */
    public void setYourEmail(String yourEmail)
    {
        this.yourEmail = yourEmail;
    }
    
    /**
     * produces an URI connection string for display purposes.
     * @param protocol the string containting the protocol
     * @param server the server address
     * @param prefix the repository path in the server
     * @param userName the user name used for login
     * @return the connection string in URI format.
     */
    public static String getURI(String protocol, String server, String prefix){
        
        String gitUrl = protocol + "://";


        gitUrl += server;
        if (prefix.length() != 0 && !prefix.startsWith("/")) {
            gitUrl += "/";
        }
        gitUrl += prefix;

        return gitUrl;
    }
}
