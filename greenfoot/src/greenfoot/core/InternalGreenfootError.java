package greenfoot.core;

/**
 * An error for internal greenfoot problems that shouldn't happen,
 * such as remote exceptions.
 * 
 * @author Davin McCall
 */
public class InternalGreenfootError extends Error
{
    public InternalGreenfootError()
    {
        
    }
    
    public InternalGreenfootError(Throwable cause)
    {
        super(cause);
    }
    
    public InternalGreenfootError(String message, Throwable cause)
    {
        super(message, cause);
    }
    
    public InternalGreenfootError(String message)
    {
        super(message);
    }
}
