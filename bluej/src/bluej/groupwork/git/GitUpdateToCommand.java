/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2018,2020  Michael Kolling and John Rosenberg
 
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
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.merge.MergeStrategy;
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

    /**
     * Construct a GitUpdateToCommand command object.
     * 
     * <p>Note that this always "updates to" (merges with) origin/<default branch>. The fetch should have
     * been performed previously. 
     * 
     * @param repository  the repository
     * @param listener    the listener for notification of file changes
     * @param forceFiles  the files to "force update"
     */
    @OnThread(Tag.Any)
    public GitUpdateToCommand(GitRepository repository, UpdateListener listener, Set<File> forceFiles)
    {
        super(repository);
        this.forceFiles = forceFiles;
        this.listener = listener;
    }

    @Override
    @OnThread(Tag.Worker)
    public TeamworkCommandResult getResult()
    {
        try (Git repo = Git.open(this.getRepository().getProjectPath()))
        {
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

            repo.getRepository().getListenerList().addWorkingTreeModifiedListener(e -> {
                for (String modified : e.getModified())
                {
                    Platform.runLater(() -> {
                        // Oddly (perhaps a bug), JGit reports removed files as "modified" if
                        // there is a conflict on update. We detect if a file is actually
                        // removed by checking if it exists:
                        File f = new File(getRepository().getProjectPath(), modified);
                        if (f.exists())
                        {
                            listener.fileModified(f);
                        }
                        else
                        {
                            listener.fileRemoved(f);
                        }
                    });
                }
                
                for (String deleted : e.getDeleted())
                {
                    Platform.runLater(() -> {
                        listener.fileRemoved(new File(getRepository().getProjectPath(), deleted));
                    });
                }
            });

            merge.include(repo.getRepository().resolve("origin/"+repo.getRepository().getBranch())); // merge with remote repository.
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
                case MERGED:
                case ALREADY_UP_TO_DATE:
                    // Changes merged and committed as a merge, or nothing changed.
                    break;
                    
                default:
                    Debug.reportError("Unknown/unhandled Git merge status: " + mergeResult.getMergeStatus());
            }

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
        if (! files.isEmpty())
        {
            try (Git repo = Git.open(this.getRepository().getProjectPath()))
            {
                Path basePath = Paths.get(this.getRepository().getProjectPath().toString());
                CheckoutCommand ccommand = repo.checkout();
                for (File f : files)
                {
                    ccommand.addPath(GitUtilities.getRelativeFileName(basePath, f));
                }
                ccommand.setStage(Stage.THEIRS);
                ccommand.setForce(true);
                ccommand.call();
            }
            catch (IOException | GitAPIException exc)
            {
                Debug.reportError("Git override files failed", exc);
            }
        }
    }
}
