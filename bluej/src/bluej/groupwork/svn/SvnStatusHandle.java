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
package bluej.groupwork.svn;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.UpdateListener;

/**
 * Implementation of StatusHandle for Subversion.
 * 
 * @author davmac
 */
public class SvnStatusHandle implements StatusHandle
{
    private SvnRepository repository;
    private long version;
    
    public SvnStatusHandle(SvnRepository repository, long version)
    {
        this.repository = repository;
        this.version = version;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#commitAll(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.lang.String)
     */
    public TeamworkCommand commitAll(Set<File> newFiles,
            Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files,
            Set<TeamStatusInfo> forceFiles, String commitComment)
    {
        Set<File> forceFileSet = new HashSet<File>();
        for (Iterator<TeamStatusInfo> i = forceFiles.iterator(); i.hasNext(); ) {
            forceFileSet.add(i.next().getFile());
        }
        
        if (version != -1) {
            return new SvnCommitCommand(repository, newFiles, binaryNewFiles, deletedFiles,
                    files, forceFileSet, version, commitComment);
        }
        else {
            // The working copy is up-to-date
            return new SvnCommitAllCommand(repository, newFiles, binaryNewFiles,
                    deletedFiles, files, commitComment);
        }
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#updateTo(bluej.groupwork.UpdateListener, java.util.Set, java.util.Set)
     */
    public TeamworkCommand updateTo(UpdateListener listener, Set<File> files,
            Set<File> forceFiles)
    {
        return new SvnUpdateToCommand(repository, listener,
                version, files, forceFiles);
    }

    @Override
    public Repository getRepository()
    {
        return repository;
    }
}
