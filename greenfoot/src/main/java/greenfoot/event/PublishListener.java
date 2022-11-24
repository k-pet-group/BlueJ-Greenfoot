/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.event;

import java.util.EventListener;

/**
 * Listener for recieving events when publishing a scenario to a website.
 * 
 * @author Poul Henriksen
 */
public interface PublishListener extends EventListener
{
    /**
     * The upload completed successfully.
     */
    public void uploadComplete(PublishEvent event);
    
    /**
     * An error occurred. The upload will not complete.
     */
    public void errorRecieved(PublishEvent event);
    
    /**
     * Some upload progress has been made.
     */
    public void progressMade(PublishEvent event);
    
    /**
     * Proxy authentication details are required.
     * @return a 2-element array containing a username/password pair, or null if the user cancels.
     */
    public String[] needProxyAuth();
}
