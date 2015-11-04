package bluej.stride.framedjava.slots;

import bluej.utility.javafx.FXRunnable;

/**
 * Created by neil on 20/02/2015.
 */
public interface UnderlineContainer
{
    // For links, not error underlines
    public static interface Underline
    {
        public int getStartPosition();
        public int getEndPosition();
        public FXRunnable getOnClick();
    }

    public void addUnderline(Underline u);
    public void removeAllUnderlines();
}
