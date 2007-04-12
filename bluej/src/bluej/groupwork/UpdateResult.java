package bluej.groupwork;

/**
 * Represents one of the results (updated files) from an update command.
 * 
 * @author Davin McCall
 */
public interface UpdateResult
{
    /**
     * Get the file name and path, relative to the project.
     */
    public String getFilename();
}
