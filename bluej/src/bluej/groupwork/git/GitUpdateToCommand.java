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

import bluej.Config;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResults;
import static bluej.groupwork.git.GitUtillities.findForkPoint;
import static bluej.groupwork.git.GitUtillities.getDiffFromList;
import static bluej.groupwork.git.GitUtillities.getDiffs;
import static bluej.groupwork.git.GitUtillities.getFileNameFromDiff;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Git command to pull project changes from the upstream repository.
 *
 * @author Fabio Heday
 */
public class GitUpdateToCommand extends GitCommand implements UpdateResults
{

    private final Set<File> files;
    private final Set<File> forceFiles;
    private final UpdateListener listener;
    private final List<File> conflicts = new ArrayList<>();
    private final Set<File> binaryConflicts = new HashSet<>();
    private List<DiffEntry> listOfDiffsLocal, listOfDiffsRemote;

    public GitUpdateToCommand(GitRepository repository, UpdateListener listener, Set<File> files, Set<File> forceFiles)
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
            File gitPath = this.getRepository().getProjectPath();

            MergeCommand merge = repo.merge();
            merge.setCommit(true);
            merge.setFastForward(MergeCommand.FastForwardMode.FF);
            merge.setStrategy(MergeStrategy.RECURSIVE);

            //before performing the merge, move package.bluej in order to avoid uneccessary conflicts.
            File packageBluejBackup = moveFile("package", "bluej");

            ObjectId headBeforeMerge = repo.getRepository().resolve("HEAD");
            ObjectId headOfRemoteBeforeMerge = repo.getRepository().resolve("origin/master");

            RevCommit forkPoint = findForkPoint(repo.getRepository(), "origin/master", "HEAD");
            merge.include(repo.getRepository().resolve("origin/master")); // merge with remote repository.
            MergeResult mergeResult = merge.call();
            Map<String, int[][]> allConflicts;
            switch (mergeResult.getMergeStatus()) {
                case FAST_FORWARD:
                    //no conflicts. this was a fast-forward merge. files where only added.
                    //if package.bluej is in forceFiles, then leave the repo as it is.
                    if (packageBluejBackup != null) {
                        if (!forceFiles.stream().anyMatch(file -> file.getName().equals("package.bluej"))) {
                            //package.bluej is not in the forceFiles list, therefore must be restored.
                            //move package.bluej back.
                            Files.move(packageBluejBackup, new File(getRepository().getProjectPath(), "package.bluej"));
                        } else {
                            //remove the backup copy.
                            packageBluejBackup.delete();
                        }
                    }
                    break;
                case CONFLICTING:
                    //update the head to compare in order to process the changes.
                    //update the conflicts list.
                    allConflicts = mergeResult.getConflicts();
                    allConflicts.keySet().stream().map((path) -> new File(gitPath, path)).forEach((f) -> {
                        conflicts.add(f);
                    });
                    break;
                case FAILED:
                    //proceed with conflict resolution if jGit managed to identify conflicts.
                    allConflicts = mergeResult.getConflicts();
                    if (allConflicts != null) {
                        allConflicts.keySet().stream().map((path) -> new File(gitPath, path)).forEach((f) -> {
                            conflicts.add(f);
                        });
                    } else {
                        return new TeamworkCommandError(Config.getString("team.error.needsPull"), Config.getString("team.error.needsPull"));
                    }
            }
            //now we need to find out what files where affected by this merge.
            //to do so, we compare the commits affected by this merge.
            
            listOfDiffsLocal = getDiffs(repo, headBeforeMerge.getName(), forkPoint);
            listOfDiffsRemote = getDiffs(repo, headOfRemoteBeforeMerge.getName(), forkPoint);
            processChanges(repo, conflicts);
            
            if (!conflicts.isEmpty() || !binaryConflicts.isEmpty()) {
                listener.handleConflicts(this);
            }
        } catch (IOException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        } catch (CheckoutConflictException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        } catch (GitAPIException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        return new TeamworkCommandResult();
    }

    @Override
    public List<File> getConflicts()
    {
        return conflicts;
    }

    @Override
    public Set<File> getBinaryConflicts()
    {
        return binaryConflicts;
    }

    @Override
    public void overrideFiles(Set<File> files)
    {
        for (File f : files) {
            DiffEntry remoteDiffItem = getDiffFromList(new File(f.getName()), listOfDiffsRemote);
            if (remoteDiffItem != null && remoteDiffItem.getChangeType() == DiffEntry.ChangeType.DELETE) {
                //remove file.
                f.delete();
                listener.fileRemoved(f);
            } else {
                listener.fileUpdated(f);
            }
        }
    }

    private void processChanges(Git repo, List<File> conflicts)
    {
        for (DiffEntry remoteDiffItem : listOfDiffsRemote) {
            File file = new File(this.getRepository().getProjectPath(), getFileNameFromDiff(remoteDiffItem));
            DiffEntry localDiffItem = getDiffFromList(remoteDiffItem, listOfDiffsLocal);
            
            switch (remoteDiffItem.getChangeType()) {
                case ADD:
                case COPY:
                    listener.fileAdded(file);
                    break;
                case DELETE:
                    if (localDiffItem != null && localDiffItem.getChangeType() == DiffEntry.ChangeType.MODIFY) {
                        //use the file selection mechanism.
                        conflicts.remove(file);
                        binaryConflicts.add(file);
                    }
                    if (!file.exists()) {
                        listener.fileRemoved(file);
                    } else {
                        listener.fileUpdated(file);
                    }
                    break;
                case MODIFY:
                    listener.fileUpdated(file);
                    break;
            }
        }

    }

    /**
     * move a file from a location to a temporary location.
     *
     * @param fileName
     * @param extension
     * @return the new file.
     * @throws IOException
     */
    private File moveFile(String fileName, String extension) throws IOException
    {
        File result = null;
        File projectPath = getRepository().getProjectPath();
        File[] matchingFiles;
        matchingFiles = projectPath.listFiles((File dir, String name) -> name.startsWith(fileName) && name.endsWith(extension));
        //there must be exactly one file.
        if (matchingFiles.length == 1) {
            result = File.createTempFile(fileName, extension);
            Files.move(matchingFiles[0], result);
        }

        return result;
    }

}
