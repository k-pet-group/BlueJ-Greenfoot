/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012  Michael Kolling and John Rosenberg 
 
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

import org.netbeans.lib.cvsclient.CVSRoot;

import bluej.Config;
import bluej.groupwork.cvsnb.BlueJAdminHandler;
import bluej.groupwork.cvsnb.CvsRepository;
import bluej.utility.Debug;

/**
 * Provider for CVS.
 * 
 * @author Davin McCall
 */
public class CvsProvider
    implements TeamworkProvider
{
    final static String pserverLabel = Config.getString("team.settings.pserver");
    final static String extLabel = Config.getString("team.settings.ext");
    
    final static String [] protocols = { pserverLabel, extLabel };
    final static String [] protocolKeys = { CVSRoot.METHOD_PSERVER, CVSRoot.METHOD_EXT };
    
    public String getProviderName()
    {
        return "CVS";
    }
    
    public TeamworkCommandResult checkConnection(TeamSettings settings)
    {
        try {
            String cvsRoot = makeCvsRoot(settings);
            return CvsRepository.validateConnection(cvsRoot);
        }
        catch (UnsupportedSettingException e) {
            return new TeamworkCommandUnsupportedSetting(e.getLocalizedMessage());
        }
    }
    
    public static String makeCvsRoot(TeamSettings settings)
      throws UnsupportedSettingException
    {
        String protocol = settings.getProtocol();
        String userName = settings.getUserName();
        String password = settings.getPassword();
        String server = settings.getServer();
        String prefix = settings.getPrefix();
        String group = settings.getGroup();
        
        // Password can be null if we are doing a task that doesn't require
        // connection to the server.
        if (password != null && password.contains("@")) {
            throw new UnsupportedSettingException(Config.getString("team.error.password.at"));
        }
        if (userName.contains(":")) {
            throw new UnsupportedSettingException(Config.getString("team.error.username.colon"));
        }
        
        String cvsRoot = ":" + protocol + ":" + userName + ":" + password + "@" +
            server + ":" + prefix;
        if (group != null && group.length() != 0) {
            if (! cvsRoot.endsWith("/")) {
                cvsRoot += "/";
            }
            cvsRoot += group;
        }
        else if (cvsRoot.endsWith("/")) {
            // Repository path should not end with '/'
            cvsRoot = cvsRoot.substring(0, cvsRoot.length() - 1);
        }
        
        return cvsRoot;
    }

    public String[] getProtocols()
    {
        return protocols;
    }
    
    public String getProtocolKey(int protocol)
    {
        return protocolKeys[protocol];
    }
    
    public String getProtocolLabel(String protocolKey)
    {
        int i = 0;
        while (!protocolKeys[i].equals(protocolKey)) i++;
        return protocols[i];
    }
    
    public Repository getRepository(File projectDir, TeamSettings settings)
    {
        try {
            String cvsRoot = makeCvsRoot(settings);
            BlueJAdminHandler adminHandler = new BlueJAdminHandler(projectDir);
            return new CvsRepository(projectDir, settings.getProtocol(), cvsRoot, adminHandler);
        }
        catch (UnsupportedSettingException e) {
            Debug.reportError("CvsProvider.getRepository", e);
            return null;
        }
        
    }
    
}
