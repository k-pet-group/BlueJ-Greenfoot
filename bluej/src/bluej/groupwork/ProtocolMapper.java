package bluej.groupwork;

import bluej.Config;
import java.io.File;
import org.netbeans.lib.cvsclient.CVSRoot;

/**
 * Utility class to map our more descriptive UI label descriptions of repository protocols
 * to the actual protocol String.
 * 
 * @author Bruce Quig
 * @version $Id: ProtocolMapper.java 4704 2006-11-27 00:07:19Z bquig $
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