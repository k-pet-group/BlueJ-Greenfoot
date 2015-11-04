package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.slots.UnderlineContainer;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 29/06/2015.
 */
public abstract class PossibleLink
{
    protected final int startPosition;
    protected final int endPosition;
    protected final UnderlineContainer slot;
    private boolean cancelled;

    public PossibleLink(int startPosition, int endPosition, UnderlineContainer slot)
    {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.slot = slot;
    }

    @OnThread(Tag.FX)
    public synchronized void cancel()
    {
        cancelled = true;
    }

    @OnThread(Tag.Any)
    protected synchronized boolean isCancelled()
    {
        return cancelled;
    }

    public int getEndPosition()
    {
        return endPosition;
    }

    public int getStartPosition()
    {
        return startPosition;
    }

    public UnderlineContainer getSlot()
    {
        return slot;
    }
}
