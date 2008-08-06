package bluej.groupwork;

/**
 * Interface for listening for results from a status command.
 * 
 * @author Davin McCall
 */
public interface StatusListener
{
    /**
     * Status is available for a file.
     */
    public void gotStatus(TeamStatusInfo info);
    
    /**
     * The status operation is complete. A status handle is provided
     * to allow commit operations.
     */
    public void statusComplete(StatusHandle statusHandle);
}
