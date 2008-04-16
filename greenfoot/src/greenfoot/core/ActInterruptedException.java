package greenfoot.core;

/**
 * Exception that indicates an interruption of the user code.
 * 
 * @author Poul Henriksen
 */
public class ActInterruptedException extends RuntimeException
{
    public ActInterruptedException()
    {
        super();
    }

    public ActInterruptedException(String message)
    {
        super(message);
    }

    public ActInterruptedException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ActInterruptedException(Throwable cause)
    {
        super(cause);
    }
}
