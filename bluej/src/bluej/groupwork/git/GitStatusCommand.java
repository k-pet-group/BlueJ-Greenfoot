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

import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import static bluej.groupwork.git.GitUtillities.findForkPoint;
import static bluej.groupwork.git.GitUtillities.getBehindCount;
import static bluej.groupwork.git.GitUtillities.getDiffs;
import static bluej.groupwork.git.GitUtillities.getFileNameFromDiff;
import bluej.utility.Debug;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.revwalk.RevCommit;
import static bluej.groupwork.git.GitUtillities.isAheadOnly;
import java.util.Map;
import org.eclipse.jgit.lib.IndexDiff;

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
    public TeamworkCommandResult getResult()
    {
        boolean didFilesChange = true;
        LinkedList<TeamStatusInfo> returnInfo = new LinkedList<>();
        File gitPath = this.getRepository().getProjectPath();

        try (Git repo = Git.open(this.getRepository().getProjectPath())) {

            //check local status
            Status s = repo.status().call();

            s.getMissing().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_DELETED)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getUncommittedChanges().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSCOMMIT)).forEach((teamInfo) -> {
                TeamStatusInfo existingStatusInfo = getTeamStatusInfo(returnInfo, teamInfo.getFile());
                if (existingStatusInfo == null) {
                    //add this new entry to the returnInfo.
                    returnInfo.add(teamInfo);
                }
            });


            s.getUntracked().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSADD)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getUntrackedFolders().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSADD)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getRemoved().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_REMOVED)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });
            
            s.getConflicting().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSMERGE)).forEach((teamInfo) -> {
                TeamStatusInfo existingStatusInfo = getTeamStatusInfo(returnInfo, teamInfo.getFile());
                if (existingStatusInfo == null){
                    returnInfo.add(teamInfo);
                }
            });
            
            Map<String, IndexDiff.StageState> conflictsMap = s.getConflictingStageState();
            conflictsMap.keySet().stream().forEach(key -> {
                File f = new File(gitPath, key);
                TeamStatusInfo statusInfo = getTeamStatusInfo(returnInfo, f);
                if (statusInfo == null) {
                    statusInfo = new TeamStatusInfo(f, "", null, TeamStatusInfo.STATUS_BLANK);
                }
                IndexDiff.StageState state = conflictsMap.get(key);
                switch (state) {
                    case DELETED_BY_THEM:
                        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_BLANK){
                            statusInfo.setStatus(TeamStatusInfo.STATUS_CONFLICT_LMRD);
                        } else if (statusInfo.getStatus() == TeamStatusInfo.STATUS_NEEDSCOMMIT && !statusInfo.getFile().exists()){
                            //if the file doesn't exist, but git report as needs commit, it means that the file was in a LMRD state,
                            //but the user choose to delete it.
                            statusInfo.setStatus(TeamStatusInfo.STATUS_DELETED);
                        }
                        break;
                    case DELETED_BY_US:
                        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_BLANK){
                            statusInfo.setStatus(TeamStatusInfo.STATUS_CONFLICT_LDRM);
                        } else if (!statusInfo.getFile().exists()){
                            statusInfo.setStatus(TeamStatusInfo.STATUS_DELETED);
                        }
                        break;
                    case BOTH_ADDED:
                        statusInfo.setStatus(TeamStatusInfo.STATUS_CONFLICT_ADD);
                        break;
                    case BOTH_MODIFIED:
                        //if status is needs commit, it means that the conflict was resolved.
                        if (statusInfo.getStatus() != TeamStatusInfo.STATUS_NEEDSCOMMIT){
                            statusInfo.setStatus(TeamStatusInfo.STATUS_NEEDSMERGE);
                        }
                        break;
                }

            });
                
            

            //check for files to push to remote repository.
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

            RevCommit forkPoint = findForkPoint(repo.getRepository(), "origin/master", "HEAD");
            
            //find diffs between master/head and the forkpoint.
            listOfDiffsLocal = getDiffs(repo, "HEAD", forkPoint);
            //check for differences between forkpoint and remote repo head.
            listOfDiffsRemote = getDiffs(repo, "origin/master", forkPoint);
            updateRemoteStatus(gitPath, listOfDiffsLocal, listOfDiffsRemote, returnInfo);
            
            if (returnInfo.isEmpty()){
                didFilesChange = false;
            }

            //Git does not show any add up-to-date file. We need to add them maually to returnInfo.
            addUpToDateFiles(returnInfo, gitPath);
            if (listener != null) {
                while (!returnInfo.isEmpty()) {
                    TeamStatusInfo teamInfo = returnInfo.removeFirst();
                    listener.gotStatus(teamInfo);
                }
                listener.statusComplete(new GitStatusHandle(getRepository(), didFilesChange && isAheadOnly(repo), didFilesChange && getBehindCount(repo) > 0));
            }
        } catch (IOException | GitAPIException | NoWorkTreeException | GitTreeException ex) {
            Debug.reportError("Git status command exception", ex);
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        
        return new TeamworkCommandResult();
    }

    private void addUpToDateFiles(LinkedList<TeamStatusInfo> returnInfo, File gitPath)
    {
        for (File item : gitPath.listFiles()) {
            if (!filter.accept(item)) {
                continue; // only process acceptable files.
            }
            TeamStatusInfo itemStatus = getTeamStatusInfo(returnInfo, item);
            if (itemStatus == null) {
                //file does not exist in the list, therefore it is up-to-date.
                returnInfo.add(new TeamStatusInfo(item, "", null, TeamStatusInfo.STATUS_UPTODATE, TeamStatusInfo.REMOTE_STATUS_UPTODATE));
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

    private void updateRemoteStatus(LinkedList<TeamStatusInfo> returnInfo, File file, int remoteStatus)
    {
        TeamStatusInfo entry = getTeamStatusInfo(returnInfo, file);
        if (entry != null) {
            entry.setRemoteStatus(remoteStatus);
        } else {
            //needs to create an entry.
            entry = new TeamStatusInfo(file, "", null, TeamStatusInfo.STATUS_UPTODATE, remoteStatus);
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
                    updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_NEEDSCOMMIT);
                    break;
                case DELETE:
                    updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_DELETED);
                    break;
                case ADD:
                    updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_NEEDSADD);
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
                                    updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_NEEDS_PUSH);
                                } else {
                                    updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_NEEDSMERGE);
                                }
                                break;
                            case DELETE:
                                updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_CONFLICT_LDRM);
                                break;
                            case ADD:
                                updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_CONFLICT_ADD);
                                break;
                        }
                    } else {
                        //there is no localDiffItem. this means its status is unchanged.
                        updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_NEEDSUPDATE);
                    }
                    break;
                case DELETE:
                    if (localDiffItem.isPresent()) {
                        switch (localDiffItem.get().getChangeType()) {
                            case MODIFY:
                                updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_CONFLICT_LMRD);
                                break;
                            case DELETE:
                                updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_DELETED);
                                break;
                            case ADD:
                                updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_NEEDSCOMMIT);
                                break;
                        }
                    } else {
                        //no localDiffItem. Its status is unchanged (up to date).
                        updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_REMOVED);
                    }
                    break;
                case ADD:
                    if (localDiffItem.isPresent()) {
                        switch (localDiffItem.get().getChangeType()) {
                            case ADD:
                                updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_CONFLICT_ADD);
                                break;
                        }
                    } else {
                        updateRemoteStatus(returnInfo, file, TeamStatusInfo.STATUS_NEEDSCHECKOUT);
                        if (!file.exists()){
                            //this file will be added, but does not exist in the local repository.
                            TeamStatusInfo tsi = getTeamStatusInfo(returnInfo, file);
                            tsi.setStatus(TeamStatusInfo.STATUS_NEEDSCHECKOUT);
                        }
                    }
            }
        }
    }


}
