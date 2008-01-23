package bluej.groupwork;

/**
 * An interface to represent a teamwork command.
 * 
 * @author Davin McCall
 */
public interface TeamworkCommand
{
    /**
     * Cancel execution of the command. This can be called from any
     * thread, and should return immediately.
     */
    public void cancel();
    
    /**
     * Complete execution of the command, and get the result.
     * Command execution might not begin until this method is called.
     */
    public TeamworkCommandResult getResult();
}
