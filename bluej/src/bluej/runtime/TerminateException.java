package bluej.runtime;

/**
 ** Exception to mark interactive "terminate" operation by user.
 **
 ** @author Michael Kolling
 **/

public final class TerminateException extends Throwable
{
    public TerminateException(String text)
    {
	super(text);
    }
}
