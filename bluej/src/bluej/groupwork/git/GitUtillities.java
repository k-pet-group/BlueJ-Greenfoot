/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016  Michael Kolling and John Rosenberg 
 
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
import bluej.utility.Debug;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/**
 * Class containing static methods for manipulating/navigating Git tree.
 *
 * @author heday
 */
public class GitUtillities
{

    /**
     * given a objectID, returns the RevTree it belongs to.
     *
     * @param repo the repository
     * @param objID the objectId
     * @return the tree if found.
     * @throws IncorrectObjectTypeException
     * @throws IOException
     */
    public static RevTree getTree(Repository repo, ObjectId objID) throws IncorrectObjectTypeException, IOException
    {
        RevTree tree;
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(objID);

            // a commit points to a tree
            tree = walk.parseTree(commit.getTree().getId());

        }
        return tree;
    }
    
    /**
     * get the diffs between two revtrees.
     * @param repo repository
     * @param masterString 
     * @param forkPoint
     * @return 
     */
    public static List<DiffEntry> getDiffs(Git repo, String masterString, RevCommit forkPoint)
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
    
    /**
     * Find the last point in two branches where they where the same.
     * @param repository
     * @param base
     * @param tip
     * @return
     * @throws IOException 
     */
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
    
    public static String getFileNameFromDiff(DiffEntry entry)
    {
        String result;
        if (entry == null) {
            return "";
        }
        if (entry.getChangeType() != DiffEntry.ChangeType.DELETE) {
            result = entry.getNewPath();
        } else {
            result = entry.getOldPath();
        }
        return result;
    }
    
    
    /**
     * return a diff from a list based on the file name.
     * @param entry
     * @param list
     * @return 
     */
    public static DiffEntry getDiffFromList(DiffEntry entry, List<DiffEntry> list)
    {
        File entryFile = new File(getFileNameFromDiff(entry));
        return getDiffFromList(entryFile, list);
    }

    /**
     * return a diff from a list based on the file name.
     *
     * @param entry
     * @param list
     * @return
     */
    public static DiffEntry getDiffFromList(File entryFile, List<DiffEntry> list)
    {
        for (DiffEntry e : list) {
            File fe = new File(getFileNameFromDiff(e));
            if (entryFile.equals(fe)) {
                return e;
            }
        }
        return null;
    }
    
    /**
     * checks if the repository is ahead and if behindCount = 0.
     * @param repo
     * @return
     * @throws IOException 
     */
    public static boolean isAheadOnly(Git repo) throws IOException
    {
        BranchTrackingStatus bts = BranchTrackingStatus.of(repo.getRepository(), repo.getRepository().getBranch());
        if (bts == null){
            //There is no remote tracking brunch. This happens in new repositories
            return false;
        }
        int aheadCount = bts.getAheadCount();
        int behindCount = bts.getBehindCount();
        
        return behindCount == 0 && aheadCount > 0;
    }
    
    /**
     * get the number of commits the repository is ahead the remote.
     * @param repo
     * @return
     * @throws IOException 
     */
    public static int getAheadCount(Git repo) throws IOException, GitTreeException
    {
        BranchTrackingStatus bts = BranchTrackingStatus.of(repo.getRepository(), repo.getRepository().getBranch());
        if (bts == null){
            throw new GitTreeException(Config.getString("team.error.noHeadBranch"));
        }
        int aheadCount = bts.getAheadCount();
        return aheadCount;
    }
    
    /**
     * checks if the repository is behind and if aheadCount = 0.
     *
     * @param repo
     * @return
     * @throws IOException
     */
    public static boolean isBehindOnly(Git repo) throws IOException
    {
        BranchTrackingStatus bts = BranchTrackingStatus.of(repo.getRepository(), repo.getRepository().getBranch());
        if (bts == null){
            return false;
            //There is no remote tracking brunch. This happens in new repositories
        }
        int aheadCount = bts.getAheadCount();
        int behindCount = bts.getBehindCount();

        return aheadCount == 0 && behindCount > 0;
    }
    
    /**
     * get the number of commits the repository is behind the remote.
     * @param repo
     * @return
     * @throws IOException 
     */
    public static int getBehindCount(Git repo) throws IOException, GitTreeException
    {
        BranchTrackingStatus bts = BranchTrackingStatus.of(repo.getRepository(), repo.getRepository().getBranch());
        if (bts == null){
            return 0;
            //throw new GitTreeException(Config.getString("team.error.noHeadBranch"));
        }
        int behindCount = bts.getBehindCount();
        return behindCount;
    }
    
}
