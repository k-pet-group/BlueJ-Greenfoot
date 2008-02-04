package bluej.groupwork;

import java.util.Set;

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
    
    /**
     * After a status command, get a command which can be used to
     * update the working copy to the same revision(s) as was 
     * shown in the status.
     */
    public TeamworkCommand getUpdateTo(UpdateListener listener, Set files, Set forceFiles);
}
