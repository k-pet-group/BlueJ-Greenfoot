/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2014,2016,2019  Michael Kolling and John Rosenberg
 
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

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.pkgmgr.Package;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class encapsulates events which occur on BlueJ classes.<br/><br/>
 * 
 * The following events can occur:<br/>
 * 
 * STATE_CHANGED    -   the compile state changed (either from uncompiled to compiled,
 *                or from compiled to uncompiled)<br/>
 * CHANGED_NAME -   the class has changed name.<br/>
 * REMOVED  -   the class has been removed.<br/><br/>
 * 
 * In the case of STATE_CHANGED there are three possible sitations:<br/>
 * <ul>
 * <li> isClassCompiled() returns true: the class has been compiled successfully.  Editing
 * the class will switch to one of the next two states.</li>
 * <li> isClassCompiled() returns false and hasError() returns true: the class
 * has been compiled and was found to have an error.  If the user edits the class,
 * it will either switch directly to the first state (if they fix it and the compile occurs quickly)
 * or the next state (if we are awaiting the compile).</li>
 * <li> isClassCompiled() returns false, and hasError() returns false: the class
 * has been edited and is awaiting the next compile.  When the next compile occurs,
 * the state will be changed to one of the above two states.</li>
 * </ul>
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class ClassEvent implements ExtensionEvent
{
    /**
     * Types of class events.
     */
    public static enum EventType
    {
        /**
         * Event generated when the compile state of a class changed (either from uncompiled to compiled, or from compiled to uncompiled).
         */
        STATE_CHANGED,

        /**
         * Event generated when a class has changed name.
         */
        CHANGED_NAME,

        /**
         * Event generated when a class has been removed.
         */
        REMOVED
    }

    private EventType eventType;
    private Package bluejPackage;
    private BClass bClass;
    private boolean isCompiled;
    private boolean hasError;
    private String oldName;

    /**
     * Constructs a new ClassEvent object for a {@link ClassEvent.EventType#STATE_CHANGED} event.
     * @param bluejPackage a {@link Package} object that contains the compiled class.
     * @param bClass a {@link BClass} object wrapping the compiled class.
     * @param isCompiled  boolean indicating whether the class is compiled (<code>true</code>) or not.
     * @param hasError boolean indicating whether the last compilation gave an error and the class not has been edited since (<code>true</code>).
     */
    public ClassEvent(Package bluejPackage, BClass bClass, boolean isCompiled, boolean hasError)
    {
        this.eventType = EventType.STATE_CHANGED;
        this.bluejPackage = bluejPackage;
        this.isCompiled = isCompiled;
        this.bClass = bClass;
        this.hasError = hasError;
    }
    
    /**
     * Constructs a new ClassEvent object for a {@link ClassEvent.EventType#CHANGED_NAME} event.
     * @param bluejPackage a {@link Package} object that contains the renamed class.
     * @param bClass   a {@link BClass} object wrapping the class which was renamed (refers to the new name).
     */
    public ClassEvent(Package bluejPackage, BClass bClass, String oldName)
    {
        this.eventType = EventType.CHANGED_NAME;
        this.bluejPackage = bluejPackage;
        this.bClass = bClass;
        this.oldName = oldName;
    }

    /**
     * Constructs a new ClassEvent object for a {@link ClassEvent.EventType#REMOVED} event.
     * @param bluejPackage a {@link Package} object that contained the removed class.
     * @param bClass   a {@link BClass} object wrapping the class which was removed.
     */
    public ClassEvent(Package bluejPackage, BClass bClass)
    {
        this.eventType = EventType.REMOVED;
        this.bluejPackage = bluejPackage;
        this.bClass = bClass;
    }
    
    /**
     * Gets the event type.
     *
     * @return The {@link EventType} value associated with this ClassEvent.
     */
    public EventType getEventType()
    {
        return eventType;
    }
    
    /**
     * Checks whether the class for which the event occurred is compiled.
     * Valid for {@link EventType#STATE_CHANGED} event.
     *
     * @return <code>True</code> if the class is compiled, <code>false</code> otherwise.
     */
    public boolean isClassCompiled()
    {
        return isCompiled;
    }
    
    /**
     * Checks whether the class for which the event occurred has compilation errors.
     * Valid for {@link EventType#STATE_CHANGED} event if isClassCompiled() returns false.
     *
     * @return <code>True</code> if the class has compilation errors, <code>false</code> otherwise.
     */
    public boolean hasError()
    {
        return hasError;
    }
    
    /**
     * Returns the package to which the class that caused this event belongs.
     * 
     * @return A {@link BPackage} object wrapping the package to which the class that caused this event belongs.
     */
    public BPackage getPackage()
    {
        return bluejPackage.getBPackage();
    }

    /**
     * Gets the class on which the event occurred.
     *
     * @return A {@link BClass} object wrapping the class on which this event occurred.
     */
    public BClass getBClass()
    {
        return bClass;
    }
    
    /**
     * Gets the old class name. Valid for {@link EventType#CHANGED_NAME} event.
     */
    public String getOldName()
    {
        return oldName;
    }
}
