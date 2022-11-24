/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2018  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Represents a set of update results from an update, and provides a method to decide
 * conflict resolution in the case of binary file conflicts.
 * 
 * @author Davin McCall
 */
public interface UpdateResults
{
    /**
     * Check whether a merge commit is needed to complete the merge.
     */
    public boolean mergeCommitNeeded();
    
    /**
     * Get a list of File objects that represents conflicts. 
     */
    public List<File> getConflicts();
    
    /**
     * Get the set of files which had binary conflicts. These are files which
     * have been modified both locally and in the repository. A decision needs to
     * be made about which version (local or repository) is to be retained; use
     * the overrideFiles() method to finalise this decision.
     */
    public Set<File> getBinaryConflicts();
    
    /**
     * Once the initial update has finished and the binary conflicts are known,
     * this method must be called to select whether to keep the local or use the
     * remove version of the conflicting files.
     *  
     * @param files  A set of files to fetch from the repository, overwriting the
     *               local version. (For any file not in the set, the local version
     *               is retained). 
     */
    @OnThread(Tag.FXPlatform)
    public void overrideFiles(Set<File> files);

}
