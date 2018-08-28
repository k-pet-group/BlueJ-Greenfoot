/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2015,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
package bluej.groupwork.svn;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.TeamworkCommandUnsupportedSetting;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.UnsupportedSettingException;
import bluej.utility.Debug;

import org.tigris.subversion.javahl.*;
import org.tigris.subversion.javahl.Revision;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

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
        
        String svnUrl = null;
        try {
            svnUrl = makeSvnUrl(settings);
            client.info2(svnUrl, Revision.HEAD, Revision.HEAD, false);
            return new TeamworkCommandResult();
        }
        catch (ClientException ce) {
            Debug.log("Subversion connection error:");
            Debug.reportError(svnUrl, ce);
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
    
    public Repository getRepository(File projectDir, TeamSettings settings) throws UnsupportedSettingException
    {
        try {
            SVNClientInterface client = getClient();
            client.username(settings.getUserName());
            client.password(settings.getPassword());
            return new SvnRepository(projectDir, settings.getProtocol(), makeSvnUrl(settings), client);
        }
        catch (UnsupportedSettingException e) {
            Debug.reportError("Unsupported Subversion Repository Settings " + e.getMessage());
            throw new UnsupportedSettingException(e.getLocalizedMessage());
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

    @Override
    public boolean needsEmail()
    {
        return false;
    }

    @Override
    public boolean needsName()
    {
        return false;
    }

    @Override
    public String getYourEmailFromRepo(File projectPath)
    {
        return null;
    }

    @Override
    public String getYourNameFromRepo(File projectPath)
    {
        return null;
    }
    
    @Override
    public double getWorkingCopyVersion(File projectDir)
    {
        double result;
        SvnWcGeneration wcGen = null;
        try {
            wcGen = SvnOperationFactory.detectWcGeneration(projectDir, false);
        } catch (SVNException ex) {
            Debug.message(ex.getMessage());
        }
        if (wcGen != null && wcGen.compareTo(SvnWcGeneration.V16) == 0) {
            result = 1.6;
        } else if (wcGen != null && wcGen.compareTo(SvnWcGeneration.V17) == 0) {
            result = 1.7;
        } else {
            result = -1; // unknown version.
        }
        return result;
    }

    @Override
    public boolean isDVCS()
    {
        return false;
    }
}
