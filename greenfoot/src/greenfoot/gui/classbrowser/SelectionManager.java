/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
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
 * @version $Id: SelectionManager.java 6216 2009-03-30 13:41:07Z polle $
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