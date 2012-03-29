/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions.event;

/**
 * <p>
 * This interface allows you to listen for class target events.
 * </p>
 * <p>
 * Currently the only event is the "visibility changed" event which occurs when
 * the visibility of a class target has changed to the opposite of the previous
 * state.
 * </p>
 * 
 * @author Simon Gerlach
 */
public interface ClassTargetListener
{
    /**
     * <p>
     * The visibility of a class target has changed. This means that the class
     * target has either been hidden from the graph or became visible again. The
     * new visibility can be acquired from the passed in
     * {@link ClassTargetEvent} object.
     * </p>
     * <p>
     * This event only occurs if the visibility of the class target has really
     * <em>changed</em>. It is not fired if a class target is set to a
     * visibility in which it already is.
     * </p>
     * 
     * @param event
     *            A {@code ClassTargetEvent} object which describes the event.
     */
    public void classTargetVisibilityChanged(ClassTargetEvent event);
}
