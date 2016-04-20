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

import bluej.groupwork.HistoryInfo;
import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.Debug;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * A Git history command.
 *
 * @author Fabio Heday
 */
public class GitHistoryCommand extends GitCommand
{

    private final LogHistoryListener listener;

    public GitHistoryCommand(GitRepository repository, LogHistoryListener listener)
    {
        super(repository);
        this.listener = listener;
    }

    @Override
    public TeamworkCommandResult getResult()
    {
        try (Git repo = Git.open(this.getRepository().getProjectPath())) {
            Iterable<RevCommit> logs = repo.log().call();
            logs.forEach((RevCommit rev) -> {
                ArrayList<String> files = new ArrayList<>();
                //every commit generates a revision with the commiter information
                //e.g.: name, e-mail, date and time of commit
                //to Access the commited files, we need to walk through the
                //the subtree.
                try (TreeWalk treeWalk = new TreeWalk(repo.getRepository())){
                    treeWalk.addTree(rev.getTree());
                    boolean hasNext = treeWalk.next();
                    while (hasNext) {
                        files.add(treeWalk.getPathString());
                        hasNext = treeWalk.next();
                    }
                } catch (IOException ex) {
                    Debug.reportError(ex.getMessage());
                }

                //Jgit returns the date by seconds after epoch, but Java works in
                //milliseconds. conversion is needed.
                Date date = new Date(rev.getCommitTime() * 1000L);
                String dateString = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(date);

                HistoryInfo info = new HistoryInfo(files.toArray(new String[files.size()]), "", dateString, rev.getAuthorIdent().getName(), rev.getFullMessage());
                listener.logInfoAvailable(info);
            });

        } catch (GitAPIException | IOException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        return new TeamworkCommandResult();
    }

}
