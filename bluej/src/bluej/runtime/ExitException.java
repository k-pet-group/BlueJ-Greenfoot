package bluej.runtime;

/**
 ** Exception to mark System.exit() call from user code.
 **
 ** @author Michael Kolling
 **/

public final class ExitException extends SecurityException
{
    public ExitException(String text)
    {
	super(text);
    }
}
