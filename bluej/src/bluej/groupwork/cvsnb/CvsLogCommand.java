/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
    
    @SuppressWarnings("unchecked")
    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        Client client = getClient();
        LogServerResponse response = repository.doGetLogHistory(client);
        
        if (! response.isError()) {
            // We have to translate the responses from the netbeans library format
            // to the internal structure used by BlueJ
            
            // map revision to List of files
            Map<bluej.groupwork.Revision,List<String>> commits = new HashMap<bluej.groupwork.Revision,
                    List<String>>();
            
            List<LogInformation> infoList = response.getInfoList();
            for (Iterator<LogInformation> i = infoList.iterator(); i.hasNext(); ) {
                LogInformation cvsInfo = i.next();
                
                // Translate the revision list
                List<Revision> cvsRevisionList = cvsInfo.getRevisionList();
                for (Iterator<Revision> j = cvsRevisionList.iterator(); j.hasNext(); ) {
                    Revision cvsRev = j.next();
                    bluej.groupwork.Revision rev = new bluej.groupwork.Revision(
                            cvsRev.getAuthor(), cvsRev.getDateString(),
                            cvsRev.getMessage());
                    // cvsRev.getNumber(); // revision number
                    
                    List<String> files = commits.get(rev);
                    if (files == null) {
                        files = new ArrayList<String>();
                    }
                    files.add(FileUtility.makeRelativePath(repository.getProjectPath(),
                            cvsInfo.getFile()));
                    
                    commits.put(rev, files);
                }
            }
            
            for (Iterator<Map.Entry<bluej.groupwork.Revision,List<String>>> i = commits.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<bluej.groupwork.Revision,List<String>> entry = i.next();
                bluej.groupwork.Revision rev = (bluej.groupwork.Revision) entry.getKey();
                List<String> filesList = entry.getValue();
                String [] files = (String []) filesList.toArray(new String[filesList.size()]);
                HistoryInfo hinfo = new HistoryInfo(files, "", rev.getDateString(), rev.getAuthor(), rev.getMessage());
                listener.logInfoAvailable(hinfo);
            }
        }
        
        return response;
    }

}
