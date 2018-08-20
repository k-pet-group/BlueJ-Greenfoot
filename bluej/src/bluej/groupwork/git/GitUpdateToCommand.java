/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
import bluej.utility.Debug;
import bluej.utility.DialogManager;

import static bluej.groupwork.git.GitUtilities.findForkPoint;
import static bluej.groupwork.git.GitUtilities.getDiffFromList;
import static bluej.groupwork.git.GitUtilities.getDiffs;
import static bluej.groupwork.git.GitUtilities.getFileNameFromDiff;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Platform;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Git command to pull project changes from the upstream repository.
 *
 * @author Fabio Heday
 */
@OnThread(Tag.Worker)
public class GitUpdateToCommand extends GitCommand implements UpdateResults
{
    private final Set<File> forceFiles;
    private final UpdateListener listener;
    // Conflicts written to on worker thread, read from on GUI thread:
    @OnThread(Tag.Any)
    private final List<File> conflicts = new ArrayList<>();
    @OnThread(Tag.Any)
    private final Set<File> binaryConflicts = new HashSet<>();
    private List<DiffEntry> listOfDiffsLocal;
    @OnThread(Tag.Any)
    private List<DiffEntry> listOfDiffsRemote;

    @OnThread(Tag.Any)
    public GitUpdateToCommand(GitRepository repository, UpdateListener listener, Set<File> files, Set<File> forceFiles)
    {
        super(repository);
        this.forceFiles = forceFiles;
        this.listener = listener;
    }

    @Override
    @OnThread(Tag.Worker)
    public TeamworkCommandResult getResult()
    {
        try (Git repo = Git.open(this.getRepository().getProjectPath())) {
            File gitPath = this.getRepository().getProjectPath();

            MergeCommand merge = repo.merge();
            merge.setCommit(true);
            merge.setFastForward(MergeCommand.FastForwardMode.FF);
            merge.setStrategy(MergeStrategy.RECURSIVE);

            if (! forceFiles.isEmpty())
            {
                Path basePath = Paths.get(this.getRepository().getProjectPath().toString());
                CheckoutCommand ccommand = repo.checkout();
                for (File f : forceFiles)
                {
                    ccommand.addPath(GitUtilities.getRelativeFileName(basePath, f));
                }
                ccommand.call();
            }

            ObjectId headBeforeMerge = repo.getRepository().resolve("HEAD");
            ObjectId headOfRemoteBeforeMerge = repo.getRepository().resolve("origin/master");

            RevCommit forkPoint = findForkPoint(repo.getRepository(), "origin/master", "HEAD");
            merge.include(repo.getRepository().resolve("origin/master")); // merge with remote repository.
            MergeResult mergeResult = merge.call();
            Map<String, int[][]> allConflicts;
            
            switch (mergeResult.getMergeStatus()) {
                case FAST_FORWARD:
                    // No conflicts; this was a fast-forward.
                    break;
                case CONFLICTING:
                    // Update the conflicts list.
                    allConflicts = mergeResult.getConflicts();
                    allConflicts.keySet().stream().map((path) -> new File(gitPath, path)).forEach((f) -> {
                        conflicts.add(f);
                    });
                    break;
                case FAILED:
                    // Proceed with conflict resolution if jGit managed to identify conflicts.
                    allConflicts = mergeResult.getConflicts();
                    if (allConflicts != null)
                    {
                        // I am not convinced this can actually happen, but this code exists so I
                        // will leave it for now and log the occurrence:
                        Debug.log("Git merge FAILED with conflicts list? conflicts = " + allConflicts);
                        allConflicts.keySet().stream().forEach(path -> {
                            conflicts.add(new File(gitPath, path));
                        });
                    }
                    else
                    {
                        // The cause(s) can be found via getFailingPaths(), in practice the reason
                        // is that either the tree or index is dirty (contains uncommitted
                        // changes).
                        String conflictMessage = DialogManager.getMessage("team-commit-needed");
                        return new TeamworkCommandError(conflictMessage, conflictMessage);
                    }
                    break;
                case ALREADY_UP_TO_DATE:
                    // Nothing changed.
                    break;
                default:
                    Debug.reportError("Unknown/unhandled Git merge status: " + mergeResult.getMergeStatus());
            }

            // now we need to find out what files where affected by this merge.
            // to do so, we compare the commits affected by this merge.
            listOfDiffsLocal = getDiffs(repo, headBeforeMerge.getName(), forkPoint);
            listOfDiffsRemote = getDiffs(repo, headOfRemoteBeforeMerge.getName(), forkPoint);
            processChanges(repo, conflicts);
            
            if (!conflicts.isEmpty() || !binaryConflicts.isEmpty()) {
                Platform.runLater(() -> listener.handleConflicts(this));
            }
        }
        catch (IOException ex)
        {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        catch (CheckoutConflictException ex)
        {
            // This occurs when the working tree has modifications to files that would be updated. 
            String conflictMessage = DialogManager.getMessage("team-commit-needed");
            return new TeamworkCommandError(conflictMessage, conflictMessage);
        }
        catch (GitAPIException ex)
        {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        return new TeamworkCommandResult();
    }

    @Override
    @OnThread(Tag.Any)
    public boolean mergeCommitNeeded()
    {
        // Note that we only issue a handle-conflicts call if there were conflicts, in which case
        // a merge commit is certainly needed.
        return true;
    }
    
    @Override
    @OnThread(Tag.Any)
    public List<File> getConflicts()
    {
        return conflicts;
    }

    @Override
    @OnThread(Tag.Any)
    public Set<File> getBinaryConflicts()
    {
        return binaryConflicts;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void overrideFiles(Set<File> files)
    {
        for (File f : files) {
            DiffEntry remoteDiffItem = getDiffFromList(new File(f.getName()), listOfDiffsRemote);
            if (remoteDiffItem != null && remoteDiffItem.getChangeType() == DiffEntry.ChangeType.DELETE) {
                //remove file.
                f.delete();
                listener.fileRemoved(f);
            } else {
                listener.fileModified(f);
            }
        }
    }

    @OnThread(Tag.Worker)
    private void processChanges(Git repo, List<File> conflicts)
    {
        for (DiffEntry remoteDiffItem : listOfDiffsRemote) {
            File file = new File(this.getRepository().getProjectPath(), getFileNameFromDiff(remoteDiffItem));
            DiffEntry localDiffItem = getDiffFromList(remoteDiffItem, listOfDiffsLocal);
            
            switch (remoteDiffItem.getChangeType()) {
                case ADD:
                case COPY:
                    Platform.runLater(() -> listener.fileModified(file));
                    break;
                case DELETE:
                    if (localDiffItem != null && localDiffItem.getChangeType() == DiffEntry.ChangeType.MODIFY) {
                        //use the file selection mechanism.
                        conflicts.remove(file);
                        binaryConflicts.add(file);
                    }
                    if (!file.exists()) {
                        Platform.runLater(() -> listener.fileRemoved(file));
                    } else {
                        Platform.runLater(() -> listener.fileModified(file));
                    }
                    break;
                case MODIFY:
                    Platform.runLater(() -> listener.fileModified(file));
                    break;
            }
        }

    }
}
