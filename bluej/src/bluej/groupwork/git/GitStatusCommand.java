/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017,2018,2020  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import static bluej.groupwork.git.GitUtilities.findForkPoint;
import static bluej.groupwork.git.GitUtilities.getBehindCount;
import static bluej.groupwork.git.GitUtilities.getDiffs;
import static bluej.groupwork.git.GitUtilities.getFileNameFromDiff;
import static bluej.groupwork.git.GitUtilities.isAheadOnly;
import bluej.utility.Debug;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.lib.IndexDiff;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Checks the status of a Git repository
 *
 * @author Fabio Hedayioglu
 */
public class GitStatusCommand extends GitCommand
{
    StatusListener listener;
    FileFilter filter;
    boolean includeRemote;

    public GitStatusCommand(GitRepository repository, StatusListener listener, FileFilter filter, boolean includeRemote)
    {
        super(repository);
        this.listener = listener;
        this.filter = filter;
        this.includeRemote = includeRemote;
    }

    @Override
    @OnThread(Tag.Worker)
    public TeamworkCommandResult getResult()
    {
        boolean didFilesChange = true;
        LinkedList<TeamStatusInfo> returnInfo = new LinkedList<>();
        File gitPath = this.getRepository().getProjectPath();

        try (Git repo = Git.open(this.getRepository().getProjectPath()))
        {
            //check local status
            org.eclipse.jgit.api.Status s = repo.status().call();

            // A file which has had changes merged as a result of a pull will be in a "unmerged"
            // state, and will appear in "uncommitted changes" as well as "conflicting" (with
            // BOTH_MODIFIED or one of the other "stages").
            
            s.getMissing().stream()
                    .filter(p -> filter.accept(new File(gitPath, p)))
                    .forEach(item -> {
                        TeamStatusInfo teamInfo = new TeamStatusInfo(new File(gitPath, item), "", null, Status.DELETED);
                        returnInfo.add(teamInfo);
                    });

            // "removed" files have been staged for removal ("git rm")
            s.getRemoved().stream()
                    .filter(p -> filter.accept(new File(gitPath, p)))
                    .forEach(item -> {
                        // Note this status might get altered below, if the file has been re-created
                        // in the meantime:
                        returnInfo.add(new TeamStatusInfo(new File(gitPath, item), "", null,
                                Status.DELETED));
                    });
            
            s.getUncommittedChanges().stream()
                    .filter(p -> filter.accept(new File(gitPath, p)))
                    .forEach(item -> {
                        TeamStatusInfo teamInfo = new TeamStatusInfo(new File(gitPath, item), "", null, Status.NEEDS_COMMIT);
                        TeamStatusInfo existingStatusInfo = getTeamStatusInfo(returnInfo, teamInfo.getFile());
                        if (existingStatusInfo == null) {
                            //add this new entry to the returnInfo.
                            returnInfo.add(teamInfo);
                        }
                    });

            s.getUntracked().stream()
                    .filter(p -> filter.accept(new File(gitPath, p)))
                    .forEach(item -> returnInfo.add(new TeamStatusInfo(new File(gitPath, item), "", null, Status.NEEDS_ADD)));

            s.getUntrackedFolders().stream()
                    .filter(p -> filter.accept(new File(gitPath, p)))
                    .forEach(item -> returnInfo.add(new TeamStatusInfo(new File(gitPath, item), "", null, Status.NEEDS_ADD)));

            Map<String, IndexDiff.StageState> conflictsMap = s.getConflictingStageState();
            s.getConflicting().stream()
                    .filter(p -> filter.accept(new File(gitPath, p)))
                    .forEach(item -> {
                        TeamStatusInfo teamInfo = getTeamStatusInfo(returnInfo, new File(gitPath, item));
                        if (teamInfo == null)
                        {
                            Debug.message("Git unexpected status: file is "
                                    + "conflicting but not otherwise noted? (" + item + ")");
                            teamInfo = new TeamStatusInfo(new File(gitPath, item), "", null, Status.NEEDS_MERGE);
                            returnInfo.add(teamInfo);
                        }
                        else
                        {
                            IndexDiff.StageState sstate = conflictsMap.get(item);
                            // Note: for local status, NEEDS_MERGE actually means "needs commit to
                            // resolve merge".
                            switch (sstate)
                            {
                                case DELETED_BY_THEM:
                                    teamInfo.setStatus(Status.CONFLICT_LMRD);
                                    break;
                                case DELETED_BY_US:
                                    teamInfo.setStatus(Status.CONFLICT_LDRM);
                                    break;
                                case BOTH_ADDED:
                                    teamInfo.setStatus(Status.CONFLICT_ADD);
                                    break;
                                case BOTH_MODIFIED:
                                    teamInfo.setStatus(Status.NEEDS_MERGE);
                                    break;
                                default:
                                    Debug.message("Git status, unknown/unhandled conflict state: " + sstate + " (" + item + ")");
                                    teamInfo.setStatus(Status.NEEDS_MERGE);
                            }
                        }
                    });

            // check for files to push to remote repository.
            List<DiffEntry> listOfDiffsLocal, listOfDiffsRemote;

            if (includeRemote) {
                //update information about remote repository.
                GitFetchCommand fetchCommand = new GitFetchCommand(this.getRepository());
                TeamworkCommandResult fetchResult = fetchCommand.getResult();
                if (fetchResult.isError()) {
                    //error updating status.
                    return fetchResult;
                }
            }

            String defaultBranchName = repo.getRepository().getBranch();
            RevCommit forkPoint = findForkPoint(repo.getRepository(), "origin/"+defaultBranchName, "HEAD");
            
            //find diffs between <default branch>/head and the forkpoint.
            listOfDiffsLocal = getDiffs(repo, "HEAD", forkPoint);
            //check for differences between forkpoint and remote repo head.
            listOfDiffsRemote = getDiffs(repo, "origin/"+defaultBranchName, forkPoint);
            updateRemoteStatus(gitPath, listOfDiffsLocal, listOfDiffsRemote, returnInfo);
            
            if (returnInfo.isEmpty()){
                didFilesChange = false;
            }

            if (listener != null) {
                // Git does not show any add up-to-date file. We need to add them manually to returnInfo.
                addUpToDateFiles(returnInfo, gitPath);
                
                while (!returnInfo.isEmpty()) {
                    TeamStatusInfo teamInfo = returnInfo.removeFirst();
                    listener.gotStatus(teamInfo);
                }
                listener.statusComplete(new GitStatusHandle(getRepository(), didFilesChange && isAheadOnly(repo), didFilesChange && getBehindCount(repo) > 0));
            }
        }
        catch (IOException | GitAPIException | NoWorkTreeException | GitTreeException ex)
        {
            Debug.reportError("Git status command exception", ex);
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        
        return new TeamworkCommandResult();
    }

    /**
     * Search a directory (recursively). For all files with no status currently recorded, add an
     * "unchanged" status entry.
     * 
     * @param returnInfo  list of file status
     * @param path        path to search
     */
    private void addUpToDateFiles(LinkedList<TeamStatusInfo> returnInfo, File path)
    {
        for (File item : path.listFiles()) {
            if (filter.accept(item)) {
                if (item.isDirectory()) {
                    addUpToDateFiles(returnInfo, item);
                }
                else {
                    TeamStatusInfo itemStatus = getTeamStatusInfo(returnInfo, item);
                    if (itemStatus == null) {
                        //file does not exist in the list, therefore it is up-to-date.
                        returnInfo.add(new TeamStatusInfo(item, "", null,
                                Status.UP_TO_DATE, Status.UP_TO_DATE));
                    }
                }
            }
        }
    }

    /**
     * checks if a file already has an entry on returnInfo
     *
     * @param returnInfo list of TeamStatusInfo to be checked
     * @param file file to check
     * @return null if there is no entry of that file. The entry if it exists.
     */
    private TeamStatusInfo getTeamStatusInfo(LinkedList<TeamStatusInfo> returnInfo, File file)
    {
        try {
            return returnInfo.stream().filter(entry -> entry.getFile().getPath().contains(file.getPath())).findFirst().get();
        } catch (Exception e) {
            return null;
        }
    }


    private Optional<DiffEntry> getDiffFromList(List<DiffEntry> list, DiffEntry entry)
    {
        Optional<DiffEntry> result;
        String entryFileName = getFileNameFromDiff(entry);
        result = list.stream().filter(p -> getFileNameFromDiff(p).equals(entryFileName)).findFirst();
        return result;
    }

    private void updateRemoteStatus(LinkedList<TeamStatusInfo> returnInfo, File file, Status remoteStatus)
    {
        TeamStatusInfo entry = getTeamStatusInfo(returnInfo, file);
        if (entry != null) {
            entry.setRemoteStatus(remoteStatus);
        } else {
            //needs to create an entry.
            entry = new TeamStatusInfo(file, "", null, Status.UP_TO_DATE, remoteStatus);
            returnInfo.add(entry);
        }
    }

    private void updateRemoteStatus(File gitPath, List<DiffEntry> listOfDiffsLocal, List<DiffEntry> listOfDiffsRemote, LinkedList<TeamStatusInfo> returnInfo)
    {
        //first check local changes that does not appear in the remote list.
        for (DiffEntry localDiffItem : listOfDiffsLocal) {
            File file = new File(gitPath, getFileNameFromDiff(localDiffItem));
            switch (localDiffItem.getChangeType()) {
                case MODIFY:
                    updateRemoteStatus(returnInfo, file, Status.NEEDS_COMMIT);
                    break;
                case DELETE:
                    updateRemoteStatus(returnInfo, file, Status.DELETED);
                    break;
                case ADD:
                    updateRemoteStatus(returnInfo, file, Status.NEEDS_ADD);
                    break;
            }
        }

        //now check for changes between the remote and local.
        for (DiffEntry remoteDiffItem : listOfDiffsRemote) {
            Optional<DiffEntry> localDiffItem = getDiffFromList(listOfDiffsLocal, remoteDiffItem);
            File file = new File(gitPath, getFileNameFromDiff(remoteDiffItem));
            switch (remoteDiffItem.getChangeType()) {
                case MODIFY:
                    if (localDiffItem.isPresent()) {
                        TeamStatusInfo entry = getTeamStatusInfo(returnInfo, file);
                        switch (localDiffItem.get().getChangeType()) {
                            case MODIFY:
                                if (entry == null){
                                    //this file was in need of a merge, however, since it does not appears 
                                    //in the local status, the merge was committed and needs to be pushed.
                                    updateRemoteStatus(returnInfo, file, Status.NEEDS_PUSH);
                                } else {
                                    updateRemoteStatus(returnInfo, file, Status.NEEDS_MERGE);
                                }
                                break;
                            case DELETE:
                                updateRemoteStatus(returnInfo, file, Status.CONFLICT_LDRM);
                                break;
                            case ADD:
                                updateRemoteStatus(returnInfo, file, Status.CONFLICT_ADD);
                                break;
                        }
                    } else {
                        //there is no localDiffItem. this means its status is unchanged.
                        updateRemoteStatus(returnInfo, file, Status.NEEDS_UPDATE);
                    }
                    break;
                case DELETE:
                    if (localDiffItem.isPresent()) {
                        switch (localDiffItem.get().getChangeType()) {
                            case MODIFY:
                                updateRemoteStatus(returnInfo, file, Status.CONFLICT_LMRD);
                                break;
                            case DELETE:
                                updateRemoteStatus(returnInfo, file, Status.DELETED);
                                break;
                            case ADD:
                                updateRemoteStatus(returnInfo, file, Status.NEEDS_COMMIT);
                                break;
                        }
                    } else {
                        //no localDiffItem. Its status is unchanged (up to date).
                        updateRemoteStatus(returnInfo, file, Status.REMOVED);
                    }
                    break;
                case ADD:
                    if (localDiffItem.isPresent()) {
                        switch (localDiffItem.get().getChangeType()) {
                            case ADD:
                                updateRemoteStatus(returnInfo, file, Status.CONFLICT_ADD);
                                break;
                        }
                    } else {
                        updateRemoteStatus(returnInfo, file, Status.NEEDS_CHECKOUT);
                        if (!file.exists()){
                            //this file will be added, but does not exist in the local repository.
                            TeamStatusInfo tsi = getTeamStatusInfo(returnInfo, file);
                            tsi.setStatus(Status.NEEDS_CHECKOUT);
                        }
                    }
            }
        }
    }
}
