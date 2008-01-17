package bluej.groupwork;

/**
 * An interface for teamwork providers - CVS, Subversion
 * 
 * @author Davin McCall
 */
public interface TeamworkProvider
{
    /**
     * Get the name of this provider ("CVS", "Subversion", etc)
     */
    public String getProviderName();
    
    /**
     * Get a list of the different protocols this provider supports (as human-
     * readable strings, not necessarily the same as what appears in the
     * repository url)
     */
    public String [] getProtocols();
    
    /**
     * Get the protocol string used internally to represent the given protocol
     * @param protocol  an index into the array returned by getProviderName()
     */
    public String getProtocolKey(int protocol);
    
    /**
     * Get the label for a given protocol key.
     */
    public String getProtocolLabel(String protocolKey);
    
    /**
     * Check that supplied information can be used to connect to a repository.
     * This might take some time to execute.
     * 
     * @param protocol  The protocol to use (an index into the array from getProtocols())
     * @param server    The server/host
     * @param prefix    The repository path (minus group part)
     * @param group     The group to which the user belongs, should be appended to
     *                  repository prefix to form complete repository path
     * @param userName  The username to connect with
     * @param password  The password to connect with
     * @return
     */
    public boolean checkConnection(String protocol, String server, String prefix,
            String group, String userName, String password);
}
