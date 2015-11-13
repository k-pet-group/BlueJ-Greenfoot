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

import bluej.Config;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
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

        TeamworkCommandResult result;
        org.eclipse.jgit.lib.Repository repo;
        try {
            File tmpFile = File.createTempFile("git_test", "");
            tmpFile.deleteOnExit();
            //create repository

//            Git git = Git.init().setDirectory(tmpFile).call();
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repo = builder.setGitDir(tmpFile).readEnvironment().findGitDir().build();
            Git git = Git.wrap(repo);
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(settings.getUserName(), settings.getPassword());
            

            Collection<Ref> remoteRefs = git.lsRemote()
                    .setCredentialsProvider(cp)
                    .setRemote(makeGitUrl(settings))
                    .setTags(true)
                    .setHeads(false)
                    .call();
            repo.close();
            git.close();
            tmpFile.delete();
        } catch (IOException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        } catch (GitAPIException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        } catch (UnsupportedSettingException ex) {
            Logger.getLogger(GitProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new TeamworkCommandResult();
    }

    @Override
    public Repository getRepository(File projectDir, TeamSettings settings) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Construct a git URL based on the given team settings
     * @param settings the teamwork settings to build the connection string from.
     * @return the git-compatible connection string
     * @throws bluej.groupwork.UnsupportedSettingException
     */
    protected String makeGitUrl(TeamSettings settings)
      throws UnsupportedSettingException
    {
        String protocol = settings.getProtocol();
        String server = settings.getServer();
        String prefix = settings.getPrefix();
        
        String gitUrl = protocol + "://" + server;
        if (prefix.length() != 0 && ! prefix.startsWith("/")) {
            gitUrl += "/";
        }
        gitUrl += prefix;

        return gitUrl;
    }
    
}
