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

package bluej.groupwork.git;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

/**
 *
 * @author heday
 */
public class GitCloneCommand extends GitCommand 
{

    private final File clonePath;
    private final String moduleName;
    private final CloneCommand cloneCommand;

    public GitCloneCommand(GitRepository repository, File projectPath) 
    {
        super(repository);
        this.clonePath = projectPath.getParentFile().getAbsoluteFile(); //git 
        //automatically create the project's directory
        moduleName = projectPath.getName();
        cloneCommand = Git.cloneRepository();
        //disable ssh host fingerprint check.
        if (repository.getReposUrl().startsWith("ssh")){
            disableFingerprintCheck();
        }
    }

    @Override
    public TeamworkCommandResult getResult() 
    {

        try {
            String reposUrl = getRepository().getReposUrl();
            
            cloneCommand.setDirectory(clonePath);
            cloneCommand.setURI(reposUrl);
            cloneCommand.call();
            if (!isCancelled()) {
                return new TeamworkCommandResult();
            }
            
            return new TeamworkCommandAborted();
        } catch (GitAPIException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
    }

    /**
     * Disable ssh's fingerprint check
     */
    private void disableFingerprintCheck()
    {
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                    @Override
                    protected void configure(OpenSshConfig.Host host, Session sn) {
                        java.util.Properties config = new java.util.Properties();
                        config.put("StrictHostKeyChecking", "no");
                        sn.setConfig(config);
                    }

                    @Override
                    protected JSch createDefaultJSch(FS fs) throws JSchException {
                        return super.createDefaultJSch(fs);
                    }
                };
                
                cloneCommand.setTransportConfigCallback((Transport t) -> {
                    SshTransport sshTransport = (SshTransport) t;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
        });
                cloneCommand.setCredentialsProvider(getRepository().getCredentialsProvider());
    }

}
