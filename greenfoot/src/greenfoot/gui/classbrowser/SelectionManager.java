package greenfoot.gui.classbrowser;

import javax.swing.event.EventListenerList;

/**
 * @author Poul Henriksen
 * @version $Id: SelectionManager.java 3124 2004-11-18 16:08:48Z polle $
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
        if (source != selected && source.isSelected()) {
            selected = source;
        }
        else if (source == selected) {
            selected = null;
        }
        else {
            return;
        }
        fireSelectionChangeEvent(selected);
    }

}