package bluej.prefmgr;

/**
 * 
 * @author Andrew Patterson
 * @version $Id: PrefPanelListener.java 1819 2003-04-10 13:47:50Z fisker $
 */
public interface PrefPanelListener
{
    void beginEditing();
    void revertEditing();
    void commitEditing();
}
