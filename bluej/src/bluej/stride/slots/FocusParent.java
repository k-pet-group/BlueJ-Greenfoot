package bluej.stride.slots;

/**
 * Created by neil on 27/04/15.
 */
public interface FocusParent<T>
{
    /**
     * Focuses the previous control, or whatever is suitable for when the up key
     * is pressed on the "src" control.
     * @param toEnd If true, and the destination for focus is a slot, move the cursor to the end of the slot
     */
    void focusUp(T src, boolean cursorToEnd);

    void focusDown(T src);

    void focusRight(T src);

    void focusEnter(T src);

    void focusLeft(T src);
}
