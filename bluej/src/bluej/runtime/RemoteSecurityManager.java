/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.runtime;

import java.security.Permission;

/**
 * A SecurityManager for the BlueJ runtime
 *
 * @author  Michael Kolling
 * @version $Id: RemoteSecurityManager.java 6164 2009-02-19 18:11:32Z polle $
 */
public class RemoteSecurityManager extends SecurityManager
{
    /**
     * All user threads will be created into this group
     */
    /*
    ThreadGroup threadGroup = null;
    */

    /**
     * This method returns the thread group that any new
     * threads should be constructed in. We rig it so that
     * if threadGroup is null, a new ThreadGroup is created and
     * this will contain all user threads. Then when we simulate
     * System.exit(), we can dispose all these threads (remembering
     * to set threadGroup back to null to reset everything).
     * Currently not implemented - we leave all user created threads
     * alone when simulating System.exit()
     */
    /*
    public ThreadGroup getThreadGroup()
    {
        if(threadGroup == null) {
            threadGroup = new ThreadGroup(super.getThreadGroup(), "BlueJ user threads");
        }
        return threadGroup;
    }
    */

    /**
     * The only thing BlueJ applications are currently not allowed to
     * do is exit normally. We handle this by signalling the exit as
     * a special exception which we catch later in in the debugger.
     *
     * @param status   the exit status.
     */
    public void checkExit(int status)
    {
    }


    public void checkMemberAccess(Class clazz,int which)
    {
    }

    /**
     * With the exception of checkExit(int) we want to
     * behave just as if there were no SecurityManager
     * installed, so we override to do nothing.
     *
     * @param perm The permission object to ignore.
     */
    public void checkPermission(Permission perm)
    {
    }

    /**
     * With the exception of checkExit(int) we want to
     * behave just as if there were no SecurityManager
     * installed, so we override to do nothing.
     *
     * @param perm The permission object to ignore.
     * @param context The context object to ignore.
     */
    public void checkPermission(Permission perm, Object context)
    {
    }
}
