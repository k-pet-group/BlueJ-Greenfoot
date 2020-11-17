/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.groupwork.git;

import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.TeamworkCommandUnsupportedSetting;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.UnsupportedSettingException;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Teamwork provider for Git.
 *
 * @author Fabio Hedayioglu
 */
@OnThread(Tag.Any)
public class GitProvider implements TeamworkProvider 
{

    private String gitUrlString;
    
    @Override
    public String getProviderName() 
    {
        return "Git";
    }

    @Override
    public String[] getProtocols() 
    {
        // 'file' protocol has been removed as it is not supported currently
        return new String[]{"https", "http", "ssh", "git"};
    }

    @Override
    public String getProtocolKey(int protocol) 
    {
        return getProtocols()[protocol];
    }

    @Override
    public String getProtocolLabel(String protocolKey) 
    {
        return protocolKey;
    }

    @Override
    public TeamworkCommandResult checkConnection(TeamSettings settings) 
    {
        try
        {
            gitUrlString = makeGitUrl(settings);
            //perform a lsRemote on the remote git repo.
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
            UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(settings.getUserName(), settings.getPassword()); // set a configuration with username and password.
            lsRemoteCommand.setRemote(gitUrlString); //configure remote repository address.
            lsRemoteCommand.setCredentialsProvider(cp); //associate the repository to the username and password.
            lsRemoteCommand.setTags(false); //disable refs/tags in reference results
            lsRemoteCommand.setHeads(false); //disable refs/heads in reference results

            //It seems that ssh host fingerprint check is not working properly. 
            //Disable it in a ssh connection.
            if (gitUrlString.startsWith("ssh"))
            {
                SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                    @Override
                    protected void configure(OpenSshConfig.Host host, Session sn)
                    {
                        java.util.Properties config = new java.util.Properties();
                        config.put("StrictHostKeyChecking", "no");
                        sn.setConfig(config);
                    }

                    @Override
                    protected JSch createDefaultJSch(FS fs) throws JSchException
                    {
                        return super.createDefaultJSch(fs);
                    }
                };
                
                lsRemoteCommand.setTransportConfigCallback((Transport t) -> {
                    SshTransport sshTransport = (SshTransport) t;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });
            }
            
            lsRemoteCommand.call(); //executes the lsRemote commnand.
        }
        catch (GitAPIException ex)
        {
            if (ex instanceof TransportException)
            {
                // There was a problem in the connection. Proceed to diagnosis.
                TeamworkCommandResult diagnosis = connectionDiagnosis(gitUrlString);
                if (!diagnosis.isError())
                {
                    // We can connect to the server.
                    if (ex.getLocalizedMessage().contains("access denied or repository not exported"))
                    {
                        return new TeamworkCommandError(DialogManager.getMessage("team-denied-invalidUser"),
                                DialogManager.getMessage("team-denied-invalidUser"));
                    }
                    if (ex.getLocalizedMessage().contains("Auth fail"))
                    {
                        return new TeamworkCommandError(DialogManager.getMessage("team-denied-invalidUser"),
                                DialogManager.getMessage("team-denied-invalidUser"));
                    }
                    if (ex.getLocalizedMessage().contains("does not appear to be a git repository"))
                    {
                        String message = DialogManager.getMessage("team-noRepository-uri", ex.getLocalizedMessage());
                        return new TeamworkCommandError( message, message);
                    }
                    // http, https and git protocols do not need username nor password.
                    if (settings.getProtocol().contains("file") || settings.getProtocol().contains("http") || settings.getProtocol().contains("git"))
                    {
                        String message = DialogManager.getMessage("team-noRepository-uri", ex.getLocalizedMessage());
                        return new TeamworkCommandError(message, message);
                    }
                }
                return diagnosis;
            }
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        catch (UnsupportedSettingException ex)
        {
            return new TeamworkCommandUnsupportedSetting(ex.getLocalizedMessage());
        } 
        //if we got here, it means the command was successful.
        return new TeamworkCommandResult();
    }
    
    @Override
    public Repository getRepository(File projectDir, TeamSettings settings) throws UnsupportedSettingException
    {
        try {
            return new GitRepository(projectDir, settings.getProtocol(), makeGitUrl(settings), settings.getBranch(),
                    settings.getUserName(), settings.getPassword(), settings.getYourName(), settings.getYourEmail());
        }
        catch (UnsupportedSettingException e) {
            Debug.reportError("Unsupported Git Repository Settings " + e.getMessage());
            throw new UnsupportedSettingException(e.getLocalizedMessage());
        }
    }

    /**
     * Construct a git URL based on the given team settings
     *
     * @param settings the teamwork settings to build the connection string
     * from.
     * @return the git-compatible connection string
     * @throws bluej.groupwork.UnsupportedSettingException
     */
    @OnThread(Tag.Any)
    protected String makeGitUrl(TeamSettings settings)
            throws UnsupportedSettingException 
    {
        String protocol = settings.getProtocol();
        //check if the protocol is a valid one.
        if (protocol == null || !Arrays.asList(getProtocols()).contains(protocol)){
            //the protocol is not valid. 
            throw new UnsupportedSettingException(Config.getString("team.error.unknownProtocol"));
        }
        
        String server = settings.getServer();
        if ((server == null || server.isEmpty()) /*&& !protocol.equals("file") // file protocol is unsupported currently*/ ){
            throw new UnsupportedSettingException(Config.getString("team.error.cannotParseServer"));
        }

        int port = settings.getPort(); // Port is optional, if not found it's not added to the URL
        
        String prefix = settings.getPrefix();
        if (prefix == null || prefix.isEmpty()){
            throw new UnsupportedSettingException(Config.getString("team.error.cannotParsePath"));
        }
        
        String gitUrl = protocol + "://";
        
        //There is a bug in jGit where the username is ignored in a ssh connection.
        //the workaround is to inject the username in the url string.
        
        if (protocol.contains("ssh")){
            gitUrl += settings.getUserName()+"@";
        }

        if (server != null)
            gitUrl += server;
        if(port > 0)
            gitUrl += (":" + port);
        if (prefix.length() != 0 && !prefix.startsWith("/")) {
            gitUrl += "/";
        }
        gitUrl += prefix;

        return gitUrl;
    }
    
    /**
     * This method creates a connection to the server and then diagnose
     * the possible causes. This method detects the following connection
     * problems:
     * unknown host
     * wrong protocol
     * malformed uri.
     * 
     * @param gitUrlString the string containing the connection uri;
     * @return A {@link TeamworkCommandResult} with a useful error if the problem
     *         is that we cannot connect at all to the server, but a successful
     *         result if we can connect to the server.  The username/password is not
     *         checked, and neither is the server type.  We only open a socket,
     *         no more.
     */
    public static TeamworkCommandResult connectionDiagnosis(String gitUrlString)
    {
        try {
            URI uri = new URI(gitUrlString);
            if (uri.getScheme().equals("file"))
                return new TeamworkCommandResult(); // It ain't a connection problem...
            int port = uri.getPort();
            if (port <= 0) {
                switch (uri.getScheme().toLowerCase()) {
                    case "http":
                        port = 80;
                        break;
                    case "https":
                        port = 443;
                        break;
                    case "ssh":
                        port = 22;
                        break;
                    case "git":
                        port = 9418;
                        break;
                }
            }
            Socket s = new Socket(uri.getHost(), port);
            s.close();

            //if we managed to reach this far: we can connect to the host, at the desired port.
            //no problems so far.
            return new TeamworkCommandResult();
        } catch (IOException ex) {
            if (ex instanceof UnknownHostException) {
                return new TeamworkCommandError(DialogManager.getMessage("team-cant-connect"), DialogManager.getMessage("team-cant-connect"));
            } else if (ex instanceof ConnectException) {
                //we found a valid host, however the protocol is invalid.
                return new TeamworkCommandError(DialogManager.getMessage("team-wrong-protocol"), DialogManager.getMessage("team-wrong-protocol"));
            }
            Debug.reportError(ex.getMessage());
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        } catch (URISyntaxException ex) {
            Debug.reportError(ex.getMessage());
            return new TeamworkCommandError(DialogManager.getMessage("team-malformed-uri"), DialogManager.getMessage("team-malformed-uri"));
        }
    }

    @Override
    public boolean needsEmail()
    {
        return true;
    }

    @Override
    public boolean needsName()
    {
        return true;
    }
    
    /**
     * Find the user email as configured for the repository, if any.
     * 
     * @param projectPath path to the BlueJ project
     * @return the stored name, if any, or null
     */
    @Override
    public String getYourNameFromRepo(File projectPath) 
    {
        String result = null;
        try
        {
            try (Git repo = Git.open(projectPath))
            {
                StoredConfig repoConfig = repo.getRepository().getConfig();
                result = repoConfig.getString("user", null, "name");
            }
        }
        catch (IOException ex)
        {
            Debug.reportError("Git: Could not get user name from repository", ex);
        }
        return result;
    }
    
    /**
     * Find the user email as configured for the repository, if any.
     * 
     * @param projectPath path to the BlueJ project
     * @return the stored email address, if any, or null
     */
    @Override
    public String getYourEmailFromRepo(File projectPath) 
    {
        String result = null;
        try
        {
            try (Git repo = Git.open(projectPath))
            {
                StoredConfig repoConfig = repo.getRepository().getConfig();
                result = repoConfig.getString("user", null, "email");
            }
        }
        catch (IOException ex)
        {
            Debug.reportError("Git: Could not get user email from repository", ex);
        }
        return result;
    }
}
