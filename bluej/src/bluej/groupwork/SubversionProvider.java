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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.tigris.subversion.javahl.*;

import bluej.Config;
import bluej.groupwork.svn.SvnRepository;
import bluej.utility.Debug;

import org.tigris.subversion.javahl.Revision;

/**
 * Teamwork provider for Subversion.
 * 
 * @author Davin McCall
 */
public class SubversionProvider implements TeamworkProvider
{
    SVNClientInterface client;
    
    public SubversionProvider()
    {
        client = getClient();
        if (client == null) {
            throw new RuntimeException("Can't initialize Subversion provider.");
        }
    }
    
    /**
     * Get a handle to the subversion client interface (SVNKit/JavaHL).
     */
    private SVNClientInterface getClient()
    {
        SVNClientInterface client = null;
        
        try {
            Class<?> clientImplClass = Class.forName("org.tmatesoft.svn.core.javahl.SVNClientImpl");
            
            Method newInstanceMethod = clientImplClass.getMethod("newInstance", new Class[0]);
            Object svnClient = newInstanceMethod.invoke(null, new Object[0]);
            
            if (svnClient instanceof SVNClientInterface) {
                client = (SVNClientInterface) svnClient;
            }
            else {
                Debug.message("Subversion client class does not implement SVNClientInterface");
            }
        }
        catch (ClassNotFoundException cnfe) {}
        catch (NoSuchMethodException nsme) {
            Debug.message("No \"newInstance()\" method in SVNClientImpl class.");
            nsme.printStackTrace();
        }
        catch (LinkageError le) {}
        catch (InvocationTargetException ite) {
            Debug.message("Error while instantiating subversion client implementation.");
            ite.printStackTrace();
            if (ite.getCause() != null) {
                ite.getCause().printStackTrace();
            }
        }
        catch (IllegalAccessException iae) {}
    
        return client;
    }
    
    public String getProviderName()
    {
        return "Subversion";
    }
    
    @SuppressWarnings("deprecation")
    public TeamworkCommandResult checkConnection(TeamSettings settings)
    {
        client.username(settings.getUserName());
        client.password(settings.getPassword());
        
        try {
            client.info2(makeSvnUrl(settings), Revision.HEAD, Revision.HEAD, false);
            return new TeamworkCommandResult();
        }
        catch (ClientException ce) {
            return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
        }
        catch (UnsupportedSettingException e) {
            return new TeamworkCommandUnsupportedSetting(e.getLocalizedMessage());
        }
        
    }
    
    public String[] getProtocols()
    {
        return new String [] { "svn", "svn+ssh", "http", "https" };
    }
    
    public String getProtocolKey(int protocol)
    {
        return getProtocols()[protocol];
    }
    
    public String getProtocolLabel(String protocolKey)
    {
        return protocolKey;
    }
    
    public Repository getRepository(File projectDir, TeamSettings settings)
    {
        try {
            SVNClientInterface client = getClient();
            client.username(settings.getUserName());
            client.password(settings.getPassword());
            return new SvnRepository(projectDir, settings.getProtocol(), makeSvnUrl(settings), client);
        }
        catch (UnsupportedSettingException e) {
            Debug.reportError("SubversionProvider.getRepository", e);
            return null;
        }
    }
    
    /**
     * Construct a subversion URL based on the given team settings
     */
    protected String makeSvnUrl(TeamSettings settings)
      throws UnsupportedSettingException
    {
        String protocol = settings.getProtocol();
        String userName = settings.getUserName();
        // String password = settings.getPassword();
        String server = settings.getServer();
        String prefix = settings.getPrefix();
        String group = settings.getGroup();
        
        if (userName.contains("@")) {
            throw new UnsupportedSettingException(Config.getString("team.error.username.at"));
        }
        
        String svnUrl = protocol + "://" + userName + "@" + server;
        if (prefix.length() != 0 && ! prefix.startsWith("/")) {
            svnUrl += "/";
        }
        svnUrl += prefix;
        if (group != null && group.length() != 0) {
            if (! svnUrl.endsWith("/")) {
                svnUrl += "/";
            }
            svnUrl += group;
        }
        else if (svnUrl.endsWith("/")) {
            // Repository path should not end with '/'
            svnUrl = svnUrl.substring(0, svnUrl.length() - 1);
        }

        return svnUrl;
    }
}
