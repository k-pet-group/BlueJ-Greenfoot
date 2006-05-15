package greenfoot.gui.classbrowser;

import javax.swing.event.EventListenerList;

/**
 * The SelectionManager manages notification of selection changes from a group
 * of selectable components to a list of listeners.
 *
 * At any time, there is (at most) one selected component, and when the selected 
 * component changes, all listeners are notified.
 *
 * @author Poul Henriksen
 * @version $Id: SelectionManager.java 4269 2006-05-15 16:36:05Z davmac $
 */
public class SelectionManager
    implements SelectionListener
{
    Selectable selected = null;
    private EventListenerList listenerList = new EventListenerList();

    public Object getSelected()
    {
        return selected;
    }

    protected void fireSelectionChangeEvent(Selectable selectable)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                ((SelectionListener) listeners[i + 1]).selectionChange(selectable);
            }
        }
    }

    /**
     * Add a changeListener to listen for changes in this LanguagePack.
     * ChangeEvents are fired when the file is saved.
     * 
     * @param l
     *            Listener to add
     */
    public void addSelectionChangeListener(SelectionListener l)
    {
        listenerList.add(SelectionListener.class, l);
    }

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.ui.classbrowser.SelectionListener#selectionChange(dk.sdu.mip.dit.ui.classbrowser.Selectable)
     */
    public void selectionChange(Selectable source)
    {
        if (source == selected) {
            if (! source.isSelected()) {
                selected = null;
                fireSelectionChangeEvent(selected);
            }
        }
        else {
            if (source.isSelected()) {
                selected = source;
                fireSelectionChangeEvent(selected);
            }
        }
    }

}