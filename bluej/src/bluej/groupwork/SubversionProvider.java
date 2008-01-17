package bluej.groupwork;

/**
 * Teamwork provider for Subversion.
 * 
 * @author Davin McCall
 */
public class SubversionProvider implements TeamworkProvider
{
    public String getProviderName()
    {
        return "Subversion";
    }
    
    public boolean checkConnection(String protocol, String server, String prefix, String group, String userName,
            String password)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    public String[] getProtocols()
    {
        return new String [] { "svn", "svn+ssh", "http" };
    }
    
    public String getProtocolKey(int protocol)
    {
        return getProtocols()[protocol];
    }
    
    public String getProtocolLabel(String protocolKey)
    {
        return protocolKey;
    }
}
