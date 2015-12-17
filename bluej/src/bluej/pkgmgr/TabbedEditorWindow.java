package bluej.pkgmgr;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A shared interface for SwingTabbedEditor and FXTabbedEditor to share the common
 * methods for saving/restoring editor window positions.
 */
@OnThread(Tag.Swing)
public interface TabbedEditorWindow
{
    public int getX();
    public int getY();
    public int getWidth();
    public int getHeight();
}
