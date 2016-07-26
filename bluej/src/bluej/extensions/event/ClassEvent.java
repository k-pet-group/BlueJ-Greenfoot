/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2014,2016  Michael Kolling and John Rosenberg
 
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

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.pkgmgr.Package;

/**
 * This class encapsulates events which occur on BlueJ classes.<p>
 * 
 * The following events can occur:<p>
 * 
 * STATE_CHANGED: The compile state changed (either from uncompiled to compiled,
 *                or from compiled to uncompiled)<p>
 * CHANGED_NAME:  The class has changed name.<p>
 * REMOVED:       The class has been removed.
 * 
 * In the case of STATE_CHANGED there are three cases:<p>
 * 
 * isClassCompiled() returns true: The class has been compiled successfully.  Editing
 * the class will switch to one of the next two states.<p>
 * isClassCompiled() returns false and hasError() returns true: The class
 * has been compiled and was found to have an error.  If the user edits the class,
 * it will either switch directly to the first state (if they fix it and the compile occurs quickly)
 * or the next state (if we are awaiting the compile). 
 * isClassCompiled() returns false, and hasError() returns false: The class
 * has been edited and is awaiting the next compile.  When the next compile occurs,
 * the state will be changed to one of the above two states.<p> 
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class ClassEvent implements ExtensionEvent
{
    public static final int STATE_CHANGED = 0;
    public static final int CHANGED_NAME = 1;
    public static final int REMOVED = 2;
    
    private int eventId;
    private Package bluejPackage;
    private BClass bClass;
    private boolean isCompiled;
    private boolean hasError;
    private String oldName;

    /**
     * Construct a new ClassEvent object for a STATE_CHANGED event.
     * @param eventId    The event identifier (STATE_CHANGED)
     * @param isCompiled  Whether the class is compiled or not
     */
    public ClassEvent(int eventId, Package bluejPackage, BClass bClass, boolean isCompiled)
    {
        this(eventId, bluejPackage, bClass, isCompiled, isCompiled);
    }


    /**
     * Construct a new ClassEvent object for a STATE_CHANGED event.
     * @param eventId    The event identifier (STATE_CHANGED)
     * @param isCompiled  Whether the class is compiled or not
     * @param hasError True if the last compilation gave an error, and the class not has been edited since. 
     */
    public ClassEvent(int eventId, Package bluejPackage, BClass bClass, boolean isCompiled, boolean hasError)
    {
        this.eventId = eventId;
        this.bluejPackage = bluejPackage;
        this.isCompiled = isCompiled;
        this.bClass = bClass;
        this.hasError = hasError;
    }
    
    /**
     * Construct a new ClassEvent object for a CHANGED_NAME event.
     * @param eventId  The event identifier (CHANGED_NAME)
     * @param bClass   The class which was renamed (refers to the new name)
     */
    public ClassEvent(int eventId, Package bluejPackage, BClass bClass, String oldName)
    {
        this.eventId = eventId;
        this.bluejPackage = bluejPackage;
        this.bClass = bClass;
        this.oldName = oldName;
    }
    
    /**
     * Get the event Id (one of STATE_CHANGED, CHANGED_NAME).
     */
    public int getEventId()
    {
        return eventId;
    }
    
    /**
     * Check whether the class for which the event occurred is compiled.
     * Valid for STATE_CHANGED event.
     */
    public boolean isClassCompiled()
    {
        return isCompiled;
    }
    
    /**
     * Check whether the class for which the event occurred is compiled.
     * Valid for STATE_CHANGED event if isClassCompiled() returns false.
     */
    public boolean hasError()
    {
        return hasError;
    }
    
    /**
     * Returns the package to which the class that caused this event belongs.
     * 
     * @return The package to which the class that caused this event belongs.
     */
    public BPackage getPackage()
    {
        return bluejPackage.getBPackage();
    }

    /**
     * Get the BClass object identifying the class on which the event
     * occurred.
     */
    public BClass getBClass()
    {
        return bClass;
    }
    
    /**
     * Get the new class name. Valid for CHANGED_NAME event.
     */
    public String getOldName()
    {
        return oldName;
    }
}
