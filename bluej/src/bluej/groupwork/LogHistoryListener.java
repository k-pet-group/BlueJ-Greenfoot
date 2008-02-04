package bluej.groupwork;

/**
 * An interface for receiving log/history information
 * 
 * @author Davin McCall
 */
public interface LogHistoryListener
{
    /**
     * Some log/history information is available, during execution of a log command.
     */
    public void logInfoAvailable(HistoryInfo logInfo);
}
