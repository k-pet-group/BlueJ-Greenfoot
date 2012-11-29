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
package bluej.groupwork.cvsnb;

import java.io.File;
import java.util.Set;

import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.UpdateListener;

/**
 * Implementation of StatusHandle for CVS.
 * 
 * @author Davin McCall
 */
public class CvsStatusHandle implements StatusHandle
{
    private CvsRepository repository;
    
    public CvsStatusHandle(CvsRepository repository)
    {
        this.repository = repository;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#commitAll(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.lang.String)
     */
    public TeamworkCommand commitAll(Set<File> newFiles,
            Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files,
            Set<TeamStatusInfo> forceFiles, String commitComment)
    {
        return new CvsCommitCommand(repository, newFiles, binaryNewFiles, deletedFiles,
                files, forceFiles, commitComment);
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#updateTo(bluej.groupwork.UpdateListener, java.util.Set, java.util.Set)
     */
    public TeamworkCommand updateTo(UpdateListener listener, Set<File> files,
            Set<File> forceFiles)
    {
        return repository.updateFiles(listener, files, forceFiles);
    }
    
    @Override
    public Repository getRepository()
    {
        return repository;
    }
}
