package bluej.stride.framedjava.errors;

import bluej.stride.framedjava.ast.SlotFragment;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 10/07/15.
 */
public class EmptyError extends SyntaxCodeError
{
    @OnThread(Tag.Any)
    public EmptyError(SlotFragment slot, String msg)
    {
        super(slot, msg);
    }
}
