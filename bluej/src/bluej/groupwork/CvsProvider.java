package bluej.groupwork;

import org.netbeans.lib.cvsclient.CVSRoot;

import bluej.Config;
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
    
    public boolean checkConnection(String protocol, String server, String prefix, String group, String userName,
            String password)
    {
        String cvsRoot = ":" + protocol + ":" + userName + ":" + password + "@" +
            server + ":" + prefix;
        if (group != null && group.length() != 0) {
            if (! cvsRoot.endsWith("/")) {
                cvsRoot += "/";
            }
            cvsRoot += group;
        }
        
        return CvsRepository.validateConnection(cvsRoot);
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
}
