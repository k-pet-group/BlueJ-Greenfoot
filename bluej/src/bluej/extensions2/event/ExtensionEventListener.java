/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.event;

/**
 * This interface allows an extension to listen for all BlueJ events by using a single listener.
 * Normally, extensions will use the specialised event types and listeners, but 
 * in some cases (e.g.) logging or testing extensions this overarching event type 
 * will be more appropriate.
 */
public interface ExtensionEventListener
{
    /**
     * This method will be called when an event occurs.
     * Note that this method is called from the JavaFX (GUI) thread and therefore the extension must
     * return as quickly as possible. 
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event An {@link ExtensionEvent} object, typically one of the {@link ExtensionEvent}'s subclasses.
     */
    public void eventOccurred(ExtensionEvent event);
}
