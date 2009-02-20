/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package greenfoot.export;

import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;
import greenfoot.export.mygame.MyGameClient;

import javax.swing.event.EventListenerList;



/**
 * Class to publish scenarios to a website. 
 * 
 * @author Poul Henriksen
 *
 */
public class WebPublisher extends MyGameClient
{
    private EventListenerList listenerList = new EventListenerList();
        
    /**
     * Create a new webpublisher. 
     * @see greenfoot.export.gameserver.GameServer#submit(String, String, String, String, String)
     */
    public WebPublisher() {

    }
       
    /**
     * Do not call this method. Use the listener interface instead.
     * 
     * @see #addPublishListener(PublishListener)
     */
    public void error(String s) {
        firePublishEvent(new PublishEvent(s, PublishEvent.ERROR));
    }

    /**
     * Do not call this method. Use the listener interface instead.
     * 
     * @see #addPublishListener(PublishListener)
     */
    public void status(String s) {
        firePublishEvent(new PublishEvent(s, PublishEvent.STATUS));
    }
    
    /**
     * Do not call this method. Use the listener interface instead.
     * 
     * @see #addPublishListener(PublishListener)
     */
    public void progress(int bytes)
    {
        firePublishEvent(new PublishEvent(bytes, PublishEvent.PROGRESS));
    }

    private void firePublishEvent(PublishEvent event)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == PublishListener.class) {
                if(event.getType() == PublishEvent.ERROR) {
                    ((PublishListener) listeners[i + 1]).errorRecieved(event);
                } 
                else if(event.getType() == PublishEvent.STATUS) {
                    ((PublishListener) listeners[i + 1]).statusRecieved(event);
                }
                else if(event.getType() == PublishEvent.PROGRESS) {
                    ((PublishListener) listeners[i + 1]).progressMade(event);
                }
            }
        }
    }

    /**
     * Add a publishListener to listen for status and error messages.
     * 
     * @param l Listener to add
     */
    public void addPublishListener(PublishListener l)
    {
        listenerList.add(PublishListener.class, l);
    }
    
    /**
     * Remove a publishListener.
     * 
     * @param l  Listener to remove
     */
    public void removePublishListener(PublishListener l)
    {
        listenerList.remove(PublishListener.class, l);
    }
}
