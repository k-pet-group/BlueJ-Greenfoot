package bluej.prefmgr;

/**
 * 
 * @author Andrew Patterson
 * @version $Id: PrefPanelListener.java 2745 2004-07-06 19:38:04Z mik $
 */
public interface PrefPanelListener
{
    void beginEditing();
    void revertEditing();
    void commitEditing();
}
