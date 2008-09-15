package bluej.groupwork.svn;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import org.tigris.subversion.javahl.ChangePath;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.LogMessage;
import org.tigris.subversion.javahl.Revision;
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
            LogMessage [] messages = client.logMessages(projectPath.getAbsolutePath(),
                    Revision.START, Revision.HEAD, false, true);
            
            for (int i = 0; i < messages.length; i++) {
                String revision = "" + messages[i].getRevisionNumber();
                
                Date theDate = messages[i].getDate();
                String date = "";
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(theDate);
                date += padInt(calendar.get(Calendar.YEAR), 4); 
                date += "/" + padInt(calendar.get(Calendar.MONTH), 2);
                date += "/" + padInt(calendar.get(Calendar.DAY_OF_MONTH), 2);
                date += " " + padInt(calendar.get(Calendar.HOUR_OF_DAY), 2);
                date += ":" + padInt(calendar.get(Calendar.MINUTE), 2);
                date += ":" + padInt(calendar.get(Calendar.SECOND), 2);
                
                ChangePath [] paths = messages[i].getChangedPaths();
                
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
                
                HistoryInfo info = new HistoryInfo(strPaths, revision, date,
                        messages[i].getAuthor(), messages[i].getMessage());
                listener.logInfoAvailable(info);
            }
            
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
