/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2018,2019  Michael Kolling and John Rosenberg
 
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Class containing static methods for manipulating/navigating Git tree.
 *
 * @author heday
 */
public class GitUtilities
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
     * Get the diffs between two revisions.
     * 
     * @param repo reference to the repository
     * @param revId   the commit (branch, etc) to diff to
     * @param forkPoint  the commit to diff from
     */
    public static List<DiffEntry> getDiffs(Git repo, String revId, RevCommit forkPoint)
    {
        if (forkPoint == null)
        {
            return Collections.emptyList();
        }

        List<DiffEntry> diffs = new ArrayList<>();
        try
        {
            ObjectId master = repo.getRepository().resolve(revId);
            RevTree masterTree = getTree(repo.getRepository(), master);
            ObjectId branchBId = repo.getRepository().resolve(forkPoint.getName());
            RevTree ForkTree = getTree(repo.getRepository(), branchBId);

            // Head and  repositories differ. We need to investigate further.
            if (ForkTree != null)
            {
                try (ObjectReader reader = repo.getRepository().newObjectReader())
                {
                    CanonicalTreeParser masterTreeIter = new CanonicalTreeParser();
                    masterTreeIter.reset(reader, masterTree);

                    CanonicalTreeParser forkTreeIter = new CanonicalTreeParser();
                    forkTreeIter.reset(reader, ForkTree);

                    // perform a diff between the local and remote tree:
                    try (DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream()))
                    {
                        df.setRepository(repo.getRepository());
                        List<DiffEntry> entries = df.scan(forkTreeIter, masterTreeIter);

                        entries.stream().forEach((entry) -> {
                            diffs.add(entry);
                        });
                    }
                }
            }
        }
        catch (IncorrectObjectTypeException ex)
        {
            Debug.reportError(ex.getMessage());
        }
        catch (RevisionSyntaxException | IOException ex)
        {
            Debug.reportError(ex.getMessage());
        }
        return diffs;
    }

    /**
     * Find the last point in two branches where they where the same.
     * 
     * @param repository  the repository
     * @param base        the name of the first ref
     * @param tip         the name of the second ref
     * @return  the merge point, or null if there is none.
     * @throws IOException  if an IO error occurs
     */
    public static RevCommit findForkPoint(Repository repository, String base, String tip) throws IOException
    {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit tipCommit = walk.lookupCommit(repository.resolve(tip));
            RevCommit baseCommit = walk.lookupCommit(repository.resolve(base));
            
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(tipCommit);
            walk.markStart(baseCommit);
            RevCommit mergeBase = walk.next();
            return mergeBase;
        }
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
     *
     * @param entryFile
     * @param list
     * @return
     */
    @OnThread(Tag.Any)
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

    /**
     * Calculates the path of a file relative to the project. It also makes sure that the
     * separator is a Unix standard one, i.e. "/", as this is what jGit lib is expecting.
     * see: http://bugs.bluej.org/browse/BLUEJ-1084
     *
     * @param basePath The project path
     * @param file     The file which relative path is needed
     * @return         The relative path of the file to the project
     */
    public static String getRelativeFileName(Path basePath, File file)
    {
        String fileName = basePath.relativize(file.toPath()).toString();
        if (!File.separator.equals("/"))
        {
            fileName = fileName.replace(File.separator, "/");
        }
        return fileName;
    }
}
