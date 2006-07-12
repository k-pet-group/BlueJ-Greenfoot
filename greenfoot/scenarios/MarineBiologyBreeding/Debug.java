// AP(r) Computer Science Marine Biology Simulation:
// The Debug class is copyright(c) 2002 College Entrance
// Examination Board (www.collegeboard.com).
//
// This class is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation.
//
// This class is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

import java.util.Stack;

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  The <code>Debug</code> class supports conditional printing of
 *  debugging messages.
 *
 *  <p>
 *  The <code>turnOn</code> and <code>turnOff</code> methods cause the
 *  previous debugging state to be saved on a stack so that it can be
 *  restored later using the <code>restoreState</code> method.
 *
 *  <p>
 *  For example, consider the following scenario.  Method 1 turns on
 *  debugging and calls Method 2.  Method 2, not knowing that debugging
 *  is already on, turns debugging on and calls Method 3.  Method 3
 *  turns debugging off.  Before returning, Method 3 restores the
 *  previous state, which turns debugging back on (as set by Method 2).
 *  Before Method 2 returns, it also restores the state, which will
 *  leave debugging on (as set by Method 1).  This is illustrated in
 *  the pseudo-code below.
 *  <pre>
 *      in Method 1:
 *          (A: in Method 1; we don't know debugging state)
 *          turn on debugging
 *          (debugging state is now on; Debug.println will print)
 *          call Method 2
 *              (B: now in Method 2; debugging state is unchanged (on))
 *              turn on debugging
 *              (debugging state is on (still))
 *              call Method 3
 *                  (C: now in Method 3; debugging state is unchanged (on))
 *                  turn off debugging
 *                  (debugging state is now off; Debug.println will not print)
 *                  restore state
 *                  (debugging state is restored to what it was at point C (on))
 *                  return to Method 2
 *               (D: now in Method 2; debugging state is unchanged (still on))
 *               restore state
 *               (debugging state is still on because it was on at point B)
 *               return to Method 1
 *           (E: now in Method 1; debugging state unchanged (still on))
 *           restore state
 *           (debugging state is whatever it was at point A (unknown))
 *  </pre>
 *  Note that when Method 2 restores the debugging state, it does not go
 *  back to its most recent state, which would be off (as set by Method 3).
 *  State restoration is controlled by a stack, not by a toggle.
 *  
 *  <p>
 *  The <code>Debug</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author Alyce Brady
 *  @version 1 July 2002
 **/
public class Debug
{
    private static boolean debugOn = false;       // debugging is on or off?
    private static Stack oldStates = new Stack(); // to restore previous states


    /** Checks whether debugging is on.  The <code>Debug.print</code>
     *  and <code>Debug.println</code>) methods use <code>isOn</code>
     *  to decide whether or not to print.
     **/
    public static boolean isOn ()
    {
        return debugOn;
    }


    /** Checks whether debugging is off.
     **/
    public static boolean isOff()
    {
        return ! isOn();
    }


    /** Turns debugging on.
     **/
    public static void turnOn()
    {
        // Push current state on stack.
        oldStates.push (new Boolean(debugOn));

        debugOn = true;
    }

    /** Turns debugging off.
     **/
    public static void turnOff()
    {
        // Push current state on stack.
        oldStates.push (new Boolean(debugOn));

        debugOn = false;
    }

    /** Restores the previous debugging state.  If there is
     *  no previous state to restore, <code>restoreState</code> turns
     *  debugging off.
     **/
    public static void restoreState()
    {
        // Is there a previous state to restore to?
        if ( oldStates.empty() )
            debugOn = false;
        else
            debugOn = ((Boolean) oldStates.pop()).booleanValue();
    }

    /** Prints debugging message without appending a newline character at
     *  the end.
     *  If debugging is turned on, <code>message</code> is
     *      printed to <code>System.out</code> without a newline.
     *  @param    message    debugging message to print
     **/
    public static void print(String message)
    {
        if ( debugOn )
            System.out.print(message);
    }

    /** Prints debugging message, appending a newline character at the end.
     *  If debugging is turned on, <code>message</code> is
     *      printed to <code>System.out</code> followed by a newline.
     *  @param    message    debugging message to print
     **/
    public static void println(String message)
    {
        if ( debugOn )
            System.out.println(message);
    }

}

