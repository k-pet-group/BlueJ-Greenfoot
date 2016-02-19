/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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

import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResults;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;

/**
 * Git command to pull project changes from the upstream repository.
 *
 * @author Fabio Heday
 */
public class GitMergeCommand extends GitCommand implements UpdateResults
{

    private final Set<File> files;
    private final Set<File> forceFiles;
    private final UpdateListener listener;
    private final List<File> conflicts = new ArrayList<>();
    private final Set<File> binaryConflicts = new HashSet<>();

    public GitMergeCommand(GitRepository repository, UpdateListener listener, Set<File> files, Set<File> forceFiles)
    {
        super(repository);
        this.files = files;
        this.forceFiles = forceFiles;
        this.listener = listener;
    }

    @Override
    public TeamworkCommandResult getResult()
    {

        try (Git repo = Git.open(this.getRepository().getProjectPath())) {
            ObjectId masterId = repo.getRepository().resolve("master");
            ObjectId originMaster = repo.getRepository().resolve("origin/master");

            if (originMaster != null) {

                ResolveMerger resolveMerger = (ResolveMerger) MergeStrategy.RESOLVE.newMerger(repo.getRepository());

                boolean isMergeSuccessful = resolveMerger.merge(false, new ObjectId[]{masterId, originMaster});
                if (isMergeSuccessful) {
                    return new TeamworkCommandResult(); //nothing to be done.
                } else {
                    //Check what went wrong.
                    Map<String, ResolveMerger.MergeFailureReason> mergeConflicts = resolveMerger.getFailingPaths();
                    LinkedList<String> keys = new LinkedList<>(mergeConflicts.keySet());
                    for (String key : keys) {
                        File f = new File(this.getRepository().getProjectPath(), key);
                        this.conflicts.add(f);
                    }
                }

                if (!conflicts.isEmpty() || !binaryConflicts.isEmpty()) {
                    listener.handleConflicts(this);
                }
            }
        } catch (IOException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        return new TeamworkCommandResult();
    }

    @Override
    public List<File> getConflicts()
    {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Set<File> getBinaryConflicts()
    {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void overrideFiles(Set<File> files)
    {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    

}
