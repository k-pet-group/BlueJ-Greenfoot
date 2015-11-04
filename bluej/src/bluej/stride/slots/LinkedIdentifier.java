package bluej.stride.slots;

import bluej.stride.framedjava.slots.UnderlineContainer;
import bluej.utility.javafx.FXRunnable;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Gets a slot, if a slot has already been associated with this fragment, either
 * because that slot generated this fragment, or because this fragment has previously
 * had a slot registered (via registerSlot).
 */
//@OnThread(Tag.FX)
//public abstract SLOT getSlot();

//@OnThread(Tag.FX)
//public abstract void registerSlot(SLOT slot);


@OnThread(Tag.FX)
public class LinkedIdentifier implements TextSlot.Underline
{
    private final String name;
    private final int startPosition; // Within slot
    private final int endPosition; // Within slot
    private final FXRunnable onClick;
    private final UnderlineContainer slot;
    @OnThread(Tag.Any)
    public LinkedIdentifier(String name, int startPosition, int endPosition,
                            UnderlineContainer slot, FXRunnable onClick)
    {
        this.name = name;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.onClick = onClick;
        this.slot = slot;
    }

    public void show() { slot.addUnderline(this); }

    @Override
    public int getStartPosition()
    {
        return startPosition;
    }

    @Override
    public int getEndPosition()
    {
        return endPosition;
    }

    @Override
    public FXRunnable getOnClick()
    {
        return onClick;
    }

    public String getName()
    {
        return name;
    }
}
