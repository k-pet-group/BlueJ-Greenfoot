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
 * This interface allows an extension to listen for class events.
 * 
 * <p>Events type are one of the {@link bluej.extensions2.event.ClassEvent.EventType} values.
 * 
 * @author Davin McCall
 */
public interface ClassListener
{
    /**
     * This method will be called when a class state changed. This means that the class source was
     * changed so that the class is now uncompiled, or the class was
     * compiled, or the class was renamed.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link ClassEvent} object of type {@link bluej.extensions2.event.ClassEvent.EventType#STATE_CHANGED}.
     */
    public void classStateChanged(ClassEvent event);

    /**
     * This method will be called when a class has been renamed.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link ClassEvent} object of type {@link bluej.extensions2.event.ClassEvent.EventType#CHANGED_NAME}.
     */
    public void classNameChanged(ClassEvent event);

    /**
     * This method will be called when a class has been removed. The removed class can be acquired from the
     * passed in {@link ClassEvent} object.
     * If a long operation must be performed the extension should start a Thread.
     *
     * @param event a {@link ClassEvent} object of type {@link bluej.extensions2.event.ClassEvent.EventType#REMOVED}.
     */
    void classRemoved(ClassEvent event);
}
