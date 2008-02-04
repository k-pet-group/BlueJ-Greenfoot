package bluej.groupwork.cvsnb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.log.LogInformation;
import org.netbeans.lib.cvsclient.command.log.LogInformation.Revision;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.HistoryInfo;
import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.LogServerResponse;
import bluej.utility.FileUtility;

/**
 * An implementation of the log/history function.
 * 
 * @author Davin McCall
 */
public class CvsLogCommand extends CvsCommand
{
    private LogHistoryListener listener;
    
    public CvsLogCommand(CvsRepository repository, LogHistoryListener listener)
    {
        super(repository);
        this.listener = listener;
    }
    
    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        Client client = getClient();
        LogServerResponse response = repository.doGetLogHistory(client);
        
        if (! response.isError()) {
            // We have to translate the responses from the netbeans library format
            // to the internal structure used by BlueJ
            
            // map revision to List of files
            Map commits = new HashMap();
            
            List infoList = response.getInfoList();
            for (Iterator i = infoList.iterator(); i.hasNext(); ) {
                LogInformation cvsInfo = (LogInformation) i.next();
                
                // Translate the revision list
                List cvsRevisionList = cvsInfo.getRevisionList();
                for (Iterator j = cvsRevisionList.iterator(); j.hasNext(); ) {
                    Revision cvsRev = (Revision) j.next();
                    bluej.groupwork.Revision rev = new bluej.groupwork.Revision(
                            cvsRev.getAuthor(), cvsRev.getDateString(),
                            cvsRev.getMessage());
                    // cvsRev.getNumber(); // revision number
                    
                    List files = (List) commits.get(rev);
                    if (files == null) {
                        files = new ArrayList();
                    }
                    files.add(FileUtility.makeRelativePath(repository.getProjectPath(),
                            cvsInfo.getFile()));
                    
                    commits.put(rev, files);
                }
            }
            
            for (Iterator i = commits.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry) i.next();
                bluej.groupwork.Revision rev = (bluej.groupwork.Revision) entry.getKey();
                List filesList = (List) entry.getValue();
                String [] files = (String []) filesList.toArray(new String[filesList.size()]);
                HistoryInfo hinfo = new HistoryInfo(files, "", rev.getDateString(), rev.getAuthor(), rev.getMessage());
                listener.logInfoAvailable(hinfo);
            }
        }
        
        return response;
    }

}
