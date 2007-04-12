package bluej.groupwork.cvs;

import bluej.Config;
import org.netbeans.lib.cvsclient.CVSRoot;

/**
 * Utility class to map our more descriptive UI label descriptions of repository protocols
 * to the actual protocol String.
 * 
 * @author Bruce Quig
 * @version $Id: ProtocolMapper.java 4916 2007-04-12 03:57:23Z davmac $
 */
public class ProtocolMapper
{
    final static String pserverLabel = Config.getString("team.settings.pserver");
    final static String extLabel = Config.getString("team.settings.ext");
    
    public static String getProtocol(String protocolLabel)
    {
        if(pserverLabel.equals(protocolLabel))
            return CVSRoot.METHOD_PSERVER;
        else if(extLabel.equals(protocolLabel))
            return CVSRoot.METHOD_EXT;
        return protocolLabel;
    }
    
}