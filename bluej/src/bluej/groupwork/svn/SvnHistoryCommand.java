/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.svn;

import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.tigris.subversion.javahl.ChangePath;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.LogDate;
import org.tigris.subversion.javahl.LogMessageCallback;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.RevisionRange;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.groupwork.HistoryInfo;
import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * A subversion history command.
 * 
 * @author Davin McCall
 */
public class SvnHistoryCommand extends SvnCommand
{
    private LogHistoryListener listener;
    
    public SvnHistoryCommand(SvnRepository repository, LogHistoryListener listener)
    {
        super(repository);
        this.listener = listener;
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        File projectPath = getRepository().getProjectPath();

        try {
            RevisionRange rr = new RevisionRange(Revision.START, Revision.HEAD);
            
            client.logMessages(projectPath.getAbsolutePath(),
                    Revision.HEAD,
                    new RevisionRange[] {rr},
                    false /* don't stopOnCopy */,
                    true  /* do discoverPath */,
                    false /* don't includeMergedRevisions */,
                    new String[] {"svn:author", "svn:date", "svn:log"},
                    0     /* give us all commits */,
                    new LogMessageCallback() {
                        @Override
                        public void singleMessage(ChangePath[] paths,
                                long revision, Map revProps,
                                boolean hasChildren)
                        {
                            if (paths.length == 0) {
                                return;
                            }
                            
                            String revisionStr = Long.toString(revision);
                            Object ldate = revProps.get("svn:date");
                            
                            try {
                                Date theDate = new LogDate(ldate.toString()).getDate();
                                String date = "";
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(theDate);
                                date += padInt(calendar.get(Calendar.YEAR), 4); 
                                date += "/" + padInt(calendar.get(Calendar.MONTH) + 1, 2);
                                date += "/" + padInt(calendar.get(Calendar.DAY_OF_MONTH), 2);
                                date += " " + padInt(calendar.get(Calendar.HOUR_OF_DAY), 2);
                                date += ":" + padInt(calendar.get(Calendar.MINUTE), 2);
                                date += ":" + padInt(calendar.get(Calendar.SECOND), 2);

                                String [] strPaths = new String[paths.length];
                                for (int j = 0; j < paths.length; j++) {
                                    strPaths[j] = paths[j].getPath();
                                    int index = strPaths[j].indexOf(File.separator, 1);
                                    if (index != -1) {
                                        strPaths[j] = strPaths[j].substring(index + 1);
                                    }
                                    else {
                                        // The project directory itself
                                        strPaths[j] = strPaths[j].substring(1);
                                    }
                                }
                                
                                String author = revProps.get("svn:author").toString();
                                String message = revProps.get("svn:log").toString();
                                HistoryInfo info = new HistoryInfo(strPaths, revisionStr,
                                        date, author, message);
                                listener.logInfoAvailable(info);
                            }
                            catch (ParseException pe) {
                                
                            }
                        }
                    });
            
            return new TeamworkCommandResult();
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
            }
        }

        return new TeamworkCommandAborted();
    }

    /**
     * Pad an integer to the given number of digits (using leading noughts).
     */
    private static String padInt(int number, int digits)
    {
        String result = Integer.toString(number);
        while (result.length() < digits) {
            result = "0" + result;
        }
        return result;
    }
}
