/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016  Michael Kolling and John Rosenberg 
 
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

import bluej.groupwork.TeamworkCommand;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Base class for Git commands.
 * It also disables SSH fingerprint's check whenever applicable.
 * 
 * @author Fabio Hedayioglu
 */
public abstract class GitCommand implements TeamworkCommand
{

    private boolean cancelled = false;
    private final GitRepository repository;

    @OnThread(Tag.Any)
    public GitCommand(GitRepository repository)
    {
        this.repository = repository;
    }
    
    
    
    
    /**
     * Prepare a TransportCommand to be executed later.
     * This method checks if the command is using ssh. if it is,
     * then it will disable SSH's fingerprint detection and use
     * username and password authentication.
     * @param command The command to be configured.
     */
    public void disableFingerprintCheck(TransportCommand command)
    {
        //check if the command is not null and conects via ssh.
        if (command != null && (repository.getReposUrl().startsWith("ssh") || repository.getReposUrl().startsWith("https"))) {
            //disable ssh host fingerprint check.
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory()
            {
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

            command.setTransportConfigCallback((Transport t) -> {
                if (t instanceof SshTransport) {
                    SshTransport sshTransport = (SshTransport) t;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
            //add credentials to both ssh and https transports.
            //github uses this information inorder to provide writing access 
            //to https connections.
            command.setCredentialsProvider(getRepository().getCredentialsProvider());
        }
    }

    @Override
    public void cancel()
    {
        if (! cancelled) {
            cancelled = true;
        }
    }

    /**
     * Check whether this command has been cancelled.
     *
     * @return true if this command has been cancelled.
     */
    public boolean isCancelled()
    {
        return cancelled;
    }

    /**
     * Get a handle to the repository.
     */
    protected GitRepository getRepository()
    {
        return repository;
    }
    
}
