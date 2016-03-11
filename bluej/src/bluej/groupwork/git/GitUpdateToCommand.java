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
import bluej.utility.Debug;
import com.google.common.io.Files;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

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

            MergeCommand merge = repo.merge();
            merge.setCommit(true);
            merge.setFastForward(MergeCommand.FastForwardMode.FF);

            //before performing the merge, move package.bluej in order to avoid uneccessary conflicts.
            File packageBluejBackup = moveFile("package", "bluej");
            ObjectId headBeforeMerge = repo.getRepository().resolve("HEAD");
            merge.include(repo.getRepository().resolve("origin/master")); // merge with remote repository.
            MergeResult mergeResult = merge.call();
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
                    
                    //now we need to find out what files where affected by this merge.
                    //to do so, we compare the commits affected by this merge.
                    processChanges(repo, headBeforeMerge, mergeResult);
            }

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

    }

    private void processChanges(Git repo, ObjectId headBeforeMerge, MergeResult mergeResult)
    {
        try {

                RevTree masterTree = getTree(repo.getRepository(), repo.getRepository().resolve("HEAD"));

                RevTree baseTree = getTree(repo.getRepository(), headBeforeMerge);

                //Base and  new Head differ. We need to investigate further.
                if (baseTree != null) {
                    try (ObjectReader reader = repo.getRepository().newObjectReader()) {
                        CanonicalTreeParser masterTreeIter = new CanonicalTreeParser();
                        masterTreeIter.reset(reader, masterTree);

                        CanonicalTreeParser forkTreeIter = new CanonicalTreeParser();
                        forkTreeIter.reset(reader, baseTree);

                        //perform a diff between the local and remote tree
                        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream());
                        df.setRepository(repo.getRepository());
                        List<DiffEntry> entries = df.scan(forkTreeIter, masterTreeIter);

                        entries.stream().forEach((DiffEntry entry) -> {

                            switch (entry.getChangeType()) {
                                case ADD:
                                case COPY:
                                    listener.fileAdded(new File(this.getRepository().getProjectPath(), entry.getNewPath()));
                                    break;
                                case DELETE:
                                    listener.fileRemoved(new File(this.getRepository().getProjectPath(), entry.getOldPath()));
                                    break;
                                case MODIFY:
                                    listener.fileUpdated(new File(this.getRepository().getProjectPath(), entry.getNewPath()));
                            }
                        });

                    } catch (IncorrectObjectTypeException ex) {
                        Debug.reportError(ex.getMessage());
                    } catch (RevisionSyntaxException | IOException ex) {
                        Debug.reportError(ex.getMessage());
                    }
                }
        } catch (IOException ex) {
            Debug.reportError(ex.getMessage());
        }
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

    /**
     * move a file from a location to a temprary location.
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
