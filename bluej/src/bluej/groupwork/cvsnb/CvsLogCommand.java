package bluej.groupwork.cvsnb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.log.LogInformation;
import org.netbeans.lib.cvsclient.command.log.LogInformation.Revision;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.LogServerResponse;

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
            List infoList = response.getInfoList();
            for (Iterator i = infoList.iterator(); i.hasNext(); ) {
                LogInformation cvsInfo = (LogInformation) i.next();
                
                // Translate the revision list
                List cvsRevisionList = cvsInfo.getRevisionList();
                List revisionList = new ArrayList(cvsRevisionList.size());
                for (Iterator j = cvsRevisionList.iterator(); j.hasNext(); ) {
                    Revision cvsRev = (Revision) j.next();
                    bluej.groupwork.Revision rev = new bluej.groupwork.Revision(
                            cvsRev.getAuthor(), cvsRev.getNumber(),
                            cvsRev.getDateString(), cvsRev.getMessage());
                    revisionList.add(rev);
                }
             
                bluej.groupwork.LogInformation info = new bluej.groupwork.LogInformation(
                        cvsInfo.getFile(), revisionList);
                
                // Send the entry to the listener
                listener.logInfoAvailable(info);
            }
        }
        
        return response;
    }

}
