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
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
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

            s.getMissing().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_REMOVED)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getUncommittedChanges().stream().filter(p -> filter.accept(new File(gitPath, p))).map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSCOMMIT)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
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

            ObjectId masterId = repo.getRepository().resolve("master");

            RevTree masterTree = getTree(repo.getRepository(), masterId);

            ObjectId originMaster = repo.getRepository().resolve("origin/master");

            RevTree originMasterTree = getTree(repo.getRepository(), originMaster);

            //local and remote repositories differ. We need to investigate further.
            if (originMaster != null) {
                DiffCommand diffCommand = repo.diff();
                try (ObjectReader reader = repo.getRepository().newObjectReader()) {
                    CanonicalTreeParser localTreeIter = new CanonicalTreeParser();
                    localTreeIter.reset(reader, masterTree);
                    CanonicalTreeParser remoteTreeIter = new CanonicalTreeParser();
                    remoteTreeIter.reset(reader, originMasterTree);
                    //perform a diff between the local and remote tree
                    List<DiffEntry> diffs = repo.diff()
                            .setNewTree(localTreeIter)
                            .setOldTree(remoteTreeIter)
                            .call();

                    //filter the results
                    //then add the entries on the returnInfo
                    diffs.stream().filter(p -> filter.accept(new File(gitPath, p.getNewPath())))
                            .map((item) -> new TeamStatusInfo(new File(gitPath, item.getNewPath()), "", null, TeamStatusInfo.STATUS_BLANK, getRemoteStatusInfo(item)))
                            .forEach((TeamStatusInfo teamInfo) -> {
                                TeamStatusInfo status = getTeamStatusInfo(returnInfo, teamInfo.getFile());
                                //avoid duplicate entries, 
                                if (status == null) {
                                    //create a new entry.
                                    returnInfo.add(teamInfo);
                                } else {
                                    //update an exisiting entry.
                                    status.setRemoteStatus(teamInfo.getRemoteStatus());
                                }
                            });

                } catch (IOException ex) {
                    Debug.reportError("Git status command exception", ex);
                }
            }

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
        for (TeamStatusInfo status : returnInfo) {
            if (status.getFile().getPath().contains(file.getPath())) {
                //file already exists in the list. Return it.
                return status;
            }
        }
        return null;
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
                return TeamStatusInfo.STATUS_NEEDSADD;
            case DELETE:
                return TeamStatusInfo.STATUS_REMOVED;
            case MODIFY:
                return TeamStatusInfo.STATUS_NEEDSCOMMIT;
            case RENAME:
                return TeamStatusInfo.STATUS_RENAMED;
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

}
