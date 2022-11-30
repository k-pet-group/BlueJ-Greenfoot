/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017,2018  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.UpdateListener;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Set;

/**
 *
 * Implementation of StatusHandle for Git
 * 
 * @author Fabio Hedayioglu
 */
@OnThread(Tag.FXPlatform)
public class GitStatusHandle implements StatusHandle
{
    private final GitRepository repository;
    private final boolean pushNeeded, pullNeeded;

    @OnThread(Tag.Any)
    public GitStatusHandle(GitRepository repository, boolean pushNeeded, boolean pullNeeded)
    {
        this.repository = repository;
        this.pushNeeded = pushNeeded;
        this.pullNeeded = pullNeeded;
    }

    @Override
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files, Set<TeamStatusInfo> forceFiles, String commitComment)
    {
        //we dont' need a list of binary files and regular files. merge them.
        newFiles.addAll(binaryNewFiles);

        return new GitCommitAllCommand(repository, newFiles, deletedFiles, files, commitComment);

    }

    @Override
    @OnThread(Tag.FXPlatform)
    public TeamworkCommand updateTo(UpdateListener listener, Set<File> files, Set<File> forceFiles)
    {
        return new GitUpdateToCommand(repository, listener, forceFiles);
    }

    @Override
    public Repository getRepository()
    {
        return repository;
    }

    @Override
    public TeamworkCommand pushAll(Set<File> filesToPush)
    {
        return new GitPushChangesCommand(repository);
    }

    @Override
    @OnThread(Tag.Any)
    public boolean pushNeeded()
    {
        return pushNeeded;
    }

    @Override
    @OnThread(Tag.Any)
    public boolean pullNeeded()
    {
        return pullNeeded;
    }
    
}
