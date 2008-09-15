package bluej.groupwork;

import java.io.File;

import org.netbeans.lib.cvsclient.CVSRoot;

import bluej.Config;
import bluej.groupwork.cvsnb.BlueJAdminHandler;
import bluej.groupwork.cvsnb.CvsRepository;

/**
 * Provider for CVS.
 * 
 * @author Davin McCall
 */
public class CvsProvider
    implements TeamworkProvider
{
    final static String pserverLabel = Config.getString("team.settings.pserver");
    final static String extLabel = Config.getString("team.settings.ext");
    
    final static String [] protocols = { pserverLabel, extLabel };
    final static String [] protocolKeys = { CVSRoot.METHOD_PSERVER, CVSRoot.METHOD_EXT };
    
    public String getProviderName()
    {
        return "CVS";
    }
    
    public TeamworkCommandResult checkConnection(TeamSettings settings)
    {
        String cvsRoot = makeCvsRoot(settings);
        return CvsRepository.validateConnection(cvsRoot);
    }
    
    public static String makeCvsRoot(TeamSettings settings)
    {
        String protocol = settings.getProtocol();
        String userName = settings.getUserName();
        String password = settings.getPassword();
        String server = settings.getServer();
        String prefix = settings.getPrefix();
        String group = settings.getGroup();
        
        String cvsRoot = ":" + protocol + ":" + userName + ":" + password + "@" +
            server + ":" + prefix;
        if (group != null && group.length() != 0) {
            if (! cvsRoot.endsWith("/")) {
                cvsRoot += "/";
            }
            cvsRoot += group;
        }
        else if (cvsRoot.endsWith("/")) {
            // Repository path should not end with '/'
            cvsRoot = cvsRoot.substring(0, cvsRoot.length() - 1);
        }
        
        return cvsRoot;
    }

    public String[] getProtocols()
    {
        return protocols;
    }
    
    public String getProtocolKey(int protocol)
    {
        return protocolKeys[protocol];
    }
    
    public String getProtocolLabel(String protocolKey)
    {
        int i = 0;
        while (!protocolKeys[i].equals(protocolKey)) i++;
        return protocols[i];
    }
    
    public Repository getRepository(File projectDir, TeamSettings settings)
    {
        String cvsRoot = makeCvsRoot(settings);
        BlueJAdminHandler adminHandler = new BlueJAdminHandler(projectDir);
        return new CvsRepository(projectDir, cvsRoot, adminHandler);
    }
    
}
