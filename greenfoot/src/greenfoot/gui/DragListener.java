package greenfoot.gui;

/**
 * Interface to use for objects that need to receive notification about a
 * drag/drop operation.
 * 
 * @author Davin McCall
 * @version $Id: DragListener.java 3165 2004-11-25 02:07:14Z davmac $
 */
public interface DragListener
{
    /**
     * A drag-n-drop operation has finished.
     */
    public void dragFinished(Object o);
}
