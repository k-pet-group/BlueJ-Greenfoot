package bluej.groupwork;

import java.io.File;

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
     */
    public TeamworkCommandResult checkConnection(TeamSettings settings);
    
    /**
     * Get a repository from the given settings
     */
    public Repository getRepository(File projectDir, TeamSettings settings);
}
