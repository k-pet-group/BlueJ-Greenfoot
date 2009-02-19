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

// NOTE: CURRENTLY UNUSED. PENDING FOR REMOVAL

/**
 * The ExitChecker checks whether a System.exit() call has occurred.
 * ATTENTION: The class needs jdk 1.4 to be compiled!
 *
 * @author  Michael Kolling
 * @version $Id: ExitChecker.java 6164 2009-02-19 18:11:32Z polle $
 */
public class ExitChecker
{

    /**
     * We need to recognise System.exit calls. We do this by installing a security
     * manager that traps the checkExit() call. checkExit() gets called from
     * within Runtime.exit(). The problem is that there could be (and are) other
     * places calling checkExit. So: if we detect a checkExit call, we start 
     * checking the stack here to see whether we are coming from Runtime.exit().
     * If so, we have a real exit call and return true, if not it's something else
     * and we return false.
     */
    public static boolean isSystemExit()
    {
        // in order to get a stack trace, we throw and catch an exception
        try {
            throwException();
        }
        catch(IllegalStateException exc) {
            // StackTraceElement[] stack = exc.getStackTrace(); // needs jdk 1.4!
            
            // in a real Runtime.exit(), the stack top looks like this:
            // [0] bluej.runtime.ExitChecker:throwException
            // [1] bluej.runtime.ExitChecker:isSystemExit
            // [2] bluej.runtime.RemoteSecurityManager:checkExit
            // [3] java.lang.Runtime:exit

            //if((stack[3].getClassName().equals("java.lang.Runtime")) &&
              // (stack[3].getMethodName().equals("exit")))
                //return true;
        }
        return false;
    }
    
    private static void throwException()
        throws IllegalStateException
    {
        throw new IllegalStateException();
    }
}
