/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015  Michael Kolling and John Rosenberg 
 
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

import bluej.groupwork.git.GitRepository;
import bluej.utility.Debug;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Teamwork provider for Git.
 *
 * @author Fabio Hedayioglu
 */
public class GitProvider implements TeamworkProvider {

    @Override
    public String getProviderName() {
        return "Git";
    }

    @Override
    public String[] getProtocols() {
        return new String[]{"https", "http", "ssh", "git"};
    }

    @Override
    public String getProtocolKey(int protocol) {
        return getProtocols()[protocol];
    }

    @Override
    public String getProtocolLabel(String protocolKey) {
        return protocolKey;
    }

    @Override
    public TeamworkCommandResult checkConnection(TeamSettings settings) {

        try {

            //perform a lsRemote on the remote git repo.
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
            UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(settings.getUserName(), settings.getPassword()); // set a configuration with username and password.
            lsRemoteCommand.setRemote(makeGitUrl(settings)); //configure remote repository address.
            lsRemoteCommand.setCredentialsProvider(cp); //associate the repository to the username and password.
            lsRemoteCommand.setTags(false); //disable refs/tags in reference results
            lsRemoteCommand.setHeads(false); //disable refs/heads in reference results
            lsRemoteCommand.call(); //executes the lsRemote commnand.

        } catch (GitAPIException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        } catch (UnsupportedSettingException ex) {
            return new TeamworkCommandUnsupportedSetting(ex.getMessage());
        }
        //if we got here, it means the command was successful.
        return new TeamworkCommandResult();
    }

    @Override
    public Repository getRepository(File projectDir, TeamSettings settings) {
        try {
            Git client = Git.open(projectDir);
            return new GitRepository(projectDir, settings.getProtocol(), makeGitUrl(settings), client, settings.getUserName(), settings.getPassword());
        } catch (UnsupportedSettingException e) {
            Debug.reportError("GitProvider.getRepository", e);
            return null;
        } catch (IOException ex) {
            Debug.reportError("GitRepository.getRepository", ex);
            return null;
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
    protected String makeGitUrl(TeamSettings settings)
            throws UnsupportedSettingException {
        String protocol = settings.getProtocol();
        String server = settings.getServer();
        String prefix = settings.getPrefix();

        String gitUrl = protocol + "://" + server;
        if (prefix.length() != 0 && !prefix.startsWith("/")) {
            gitUrl += "/";
        }
        gitUrl += prefix;

        return gitUrl;
    }

}
