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
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.Debug;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

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

            s.getConflicting().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSMERGE)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getUntracked().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSADD)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getUntrackedFolders().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSADD)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getRemoved().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSADD)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            //check for files to push to remote repository.
            List<DiffEntry> listOfDiffs;

            if (includeRemote) {
                //update information about remote repository.
                GitFetchCommand fetchCommand = new GitFetchCommand(this.getRepository());
                fetchCommand.getResult();
            }

            RevCommit forkPoint = findForkPoint(repo.getRepository(), "origin/master", "HEAD");

            //find diffs between master/head and the forkpoint.
            //this will produce the list of file to push.
            listOfDiffs = getDiffs(repo, "HEAD", forkPoint);
            findOutPushNeeded(gitPath, listOfDiffs, returnInfo);

            //check for differences between forkpoint and remote repo head.
            //this will produce the list of files to pull.
            listOfDiffs = getDiffs(repo, "origin/master", forkPoint);
            diff(gitPath, listOfDiffs, returnInfo);

        } catch (IOException | GitAPIException | NoWorkTreeException ex) {
            Debug.reportError("Git status command exception", ex);
        }
        if (listener != null) {
            while (!returnInfo.isEmpty()) {
                TeamStatusInfo teamInfo = (TeamStatusInfo) returnInfo.removeFirst();
                listener.gotStatus(teamInfo);
            }
            listener.statusComplete(new GitStatusHandle(getRepository()));
        }
        return new TeamworkCommandResult();
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

    /**
     * get the TeamStatus for the status of the remote entry.
     *
     * @param entry the remote entry
     * @return the TeamStatus integer correspondent to that entry.
     */
    private int getRemoteStatusInfo(DiffEntry entry)
    {
        switch (entry.getChangeType()) {
            case ADD:
            case COPY:
                return TeamStatusInfo.REMOTE_STATUS_ADDED;
            case DELETE:
                return TeamStatusInfo.REMOTE_STATUS_DELETED;
            case MODIFY:
                return TeamStatusInfo.REMOTE_STATUS_MODIFIED;
            case RENAME:
                return TeamStatusInfo.REMOTE_STATUS_RENAMED;
        }
        return TeamStatusInfo.STATUS_WEIRD;
    }

    /**
     * given a objectID, returns the RevTree it belongs to.
     *
     * @param repo the repository
     * @param objID the objectId
     * @return the tree if found.
     * @throws IncorrectObjectTypeException
     * @throws IOException
     */
    private RevTree getTree(Repository repo, ObjectId objID) throws IncorrectObjectTypeException, IOException
    {
        RevTree tree;
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(objID);

            // a commit points to a tree
            tree = walk.parseTree(commit.getTree().getId());

        }
        return tree;
    }

    public static RevCommit findForkPoint(Repository repository, String base, String tip) throws IOException
    {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit tipCommit = walk.lookupCommit(repository.resolve(tip));
            List<ReflogEntry> reflog = repository.getReflogReader(base).getReverseEntries();
            if (reflog.isEmpty()) {
                return null; // no fork point.
            }
            for (int i = 0; i <= reflog.size(); i++) {
                ObjectId id = i < reflog.size() ? reflog.get(i).getNewId() : reflog.get(i - 1).getOldId();
                RevCommit commit = walk.lookupCommit(id);
                if (walk.isMergedInto(commit, tipCommit)) {
                    //found the fork point.
                    walk.parseBody(commit);
                    return commit;
                }
            }
        }
        return null; //no fork point.
    }

    private List<DiffEntry> getDiffs(Git repo, String masterString, RevCommit forkPoint)
    {
        List<DiffEntry> diffs = new ArrayList<>();
        try {

            ObjectId master = repo.getRepository().resolve(masterString);

            RevTree masterTree = getTree(repo.getRepository(), master);

            ObjectId branchBId = repo.getRepository().resolve(forkPoint.getName());

            RevTree ForkTree = getTree(repo.getRepository(), branchBId);

            //Head and  repositories differ. We need to investigate further.
            if (ForkTree != null) {
                try (ObjectReader reader = repo.getRepository().newObjectReader()) {
                    CanonicalTreeParser masterTreeIter = new CanonicalTreeParser();
                    masterTreeIter.reset(reader, masterTree);

                    CanonicalTreeParser forkTreeIter = new CanonicalTreeParser();
                    forkTreeIter.reset(reader, ForkTree);

 
                    //perform a diff between the local and remote tree
                    DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream());
                    df.setRepository(repo.getRepository());
                    List<DiffEntry> entries = df.scan(forkTreeIter, masterTreeIter);

                    entries.stream().forEach((entry) -> {
                        diffs.add(entry);
                    });
                    
                }            
            }
        } catch (IncorrectObjectTypeException ex) {
            Debug.reportError(ex.getMessage());
        } catch (RevisionSyntaxException | IOException ex) {
            Debug.reportError(ex.getMessage());
        }
        return diffs;
    }

    private void findOutPushNeeded(File gitPath, List<DiffEntry> diffs, LinkedList<TeamStatusInfo> returnInfo)
    {
        //filter the results
        //then add the entries on the returnInfo
        for (DiffEntry entry : diffs) {
            if (filter.accept(new File(gitPath, entry.getOldPath()))) {
                File file;
                if (entry.getChangeType() != DiffEntry.ChangeType.DELETE) {
                    file = new File(gitPath, entry.getNewPath());
                } else {
                    file = new File(gitPath, entry.getOldPath());
                }
                TeamStatusInfo existingStatusInfo = getTeamStatusInfo(returnInfo, file);
                //if there is no existing status info, need to add to the list

                if (existingStatusInfo == null) {
                    //needs to add the file to the list.
                    TeamStatusInfo item = new TeamStatusInfo(file, "", null, TeamStatusInfo.STATUS_NEEDS_PUSH, TeamStatusInfo.STATUS_NEEDS_PUSH);
                    returnInfo.add(item);
                } else {
                    //there is a status for this file.
                    //we need to update the remote status.
                    existingStatusInfo.setRemoteStatus(getRemoteStatusInfo(entry));
                }

            }
        }

    }

    /**
     * checks for changes between local master and origin/master
     *
     * @param repo git repository
     * @param gitPath local git repository path
     * @param returnInfo list of files where change was detected.
     */
    private void diff(File gitPath, List<DiffEntry> diffs, LinkedList<TeamStatusInfo> returnInfo)
    {

        //filter the results
        //then add the entries on the returnInfo
        for (DiffEntry entry : diffs) {
            if (filter.accept(new File(gitPath, entry.getOldPath()))) {
                int gitRemoteStatusInfo = getRemoteStatusInfo(entry);
                File file;
                if (gitRemoteStatusInfo == TeamStatusInfo.REMOTE_STATUS_ADDED || gitRemoteStatusInfo == TeamStatusInfo.REMOTE_STATUS_MODIFIED) {
                    file = new File(gitPath, entry.getNewPath());
                } else {
                    file = new File(gitPath, entry.getOldPath());
                }
                TeamStatusInfo item = new TeamStatusInfo(file, "", null, TeamStatusInfo.STATUS_BLANK, gitRemoteStatusInfo);
                TeamStatusInfo localStatusInfo = getTeamStatusInfo(returnInfo, file);
                if (localStatusInfo == null) {
                    //file does not appear in the current returnInfo.
                    //this means that either the file is uptodate 
                    //locally or it needs to be added, since it
                    //appears in the remote repo.
                    if (entry.getChangeType() == DiffEntry.ChangeType.ADD) {
                        if (file.exists()) {
                            //file exists locally, therefore local status is updtodate
                            item.setStatus(TeamStatusInfo.STATUS_UPTODATE);
                        } else {
                            //file does not exists locally, therefore local repo needs
                            //to be updated.
                            item.setStatus(TeamStatusInfo.STATUS_NEEDSUPDATE);
                        }
                    }
                    if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                        if (file.exists()) {
                            //file exits locally, therefore it was deleted in the remote.
                            item.setStatus(TeamStatusInfo.STATUS_UPTODATE);
                        } else {
                            //file was deleted locally. Needs to be pushed.
                            item.setRemoteStatus(TeamStatusInfo.STATUS_DELETED);
                            item.setStatus(TeamStatusInfo.STATUS_UPTODATE);
                        }
                    }
                    if (entry.getChangeType() == DiffEntry.ChangeType.MODIFY) {
                        //if we got a modify, there must be a local file.
                        //update local status.
                        item.setStatus(TeamStatusInfo.STATUS_UPTODATE);
                    }

                    if (entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
                        item.setRemoteStatus(TeamStatusInfo.STATUS_RENAMED);
                        item.setStatus(TeamStatusInfo.STATUS_UPTODATE);
                    }

                    returnInfo.add(item);
                } else {
                    //update remote status.
                    localStatusInfo.setRemoteStatus(item.getRemoteStatus());
                }
            }
        }
    }

}
