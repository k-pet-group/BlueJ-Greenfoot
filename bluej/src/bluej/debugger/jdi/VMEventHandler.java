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
package bluej.debugger.jdi;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;

/**
 * Event handler class to handle events coming from the remote VM.
 *
 * @author  Michael Kolling
 * @version $Id: VMEventHandler.java 6163 2009-02-19 18:09:55Z polle $
 */
class VMEventHandler extends Thread
{
    final static String DONT_RESUME = "dontResume";
    
    private VMReference vm;
    private EventQueue queue;
    private boolean queueEmpty;
    
    volatile boolean exiting = false;
    
    VMEventHandler(VMReference vm, VirtualMachine vmm)
    {
        super("vm-event-handler");
        this.vm = vm;
        queue = vmm.eventQueue();
        start();  // will execute our own run method
    }
    
    public void run()
    {
        while (!exiting) {
            try {
                // get the next event
                EventSet eventSet = queue.remove(1);
                
                if (eventSet == null) {
                    // If no event is currently available, signal anyone waiting for
                    // the queue to empty, and then block until an event arrives
                    synchronized (this) {
                        queueEmpty = true;
                        notifyAll();
                    }

                    eventSet = queue.remove();
                    synchronized (this) {
                        queueEmpty = false;
                    }
                }
                
                // From the JDK documentation
                // The events that are grouped in an EventSet are restricted in the following ways:
                //   * Always singleton sets:
                //		 o VMStartEvent
                //		 o VMDisconnectEvent 
                //   * Only with other VMDeathEvents:
                //		 o VMDeathEvent 
                //   * Only with other ThreadStartEvents for the same thread:
                //		 o ThreadStartEvent 
                //   * Only with other ThreadDeathEvents for the same thread:
                //		 o ThreadDeathEvent 
                //   * Only with other ClassPrepareEvents for the same class:
                //		 o ClassPrepareEvent 
                //   * Only with other ClassUnloadEvents for the same class:
                //		 o ClassUnloadEvent 
                //   * Only with other AccessWatchpointEvents for the same field access:
                //		 o AccessWatchpointEvent 
                //   * Only with other ModificationWatchpointEvents for the same field modification:
                //		 o ModificationWatchpointEvent 
                //   * Only with other ExceptionEvents for the same exception occurrance:
                //		 o ExceptionEvent 
                //   * Only with other MethodExitEvents for the same method exit:
                //		 o MethodExitEvent 
                //   * Only with other members of this group, at the same location and in the same thread:
                //		 o BreakpointEvent
                //		 o StepEvent
                //		 o MethodEntryEvent 
                
                boolean addToSuspendCount = true;
                
                // iterate through all events in the set
                EventIterator it = eventSet.eventIterator();
                
                while (it.hasNext()) {
                    Event ev = it.nextEvent();
                    
                    // do some processing with this event
                    // this calls back into VMReference
                    handleEvent(ev);
                    
                    // for breakpoint and step events, we may want
                    // to leave the relevant thread suspended. If the dontResume
                    // property for the event is set, then lets do this.
                    if(ev.request() != null) {
                        if(addToSuspendCount && ev.request().getProperty(DONT_RESUME) != null) {
                            if(ev instanceof LocatableEvent) {
                                LocatableEvent le = (LocatableEvent) ev;
                                le.thread().suspend();
                                addToSuspendCount = false;
                                // a step and breakpoint can be hit at the same
                                // time - make sure to only suspend once
                            }
                        }
                    }
                }
                
                // resume the VM
                eventSet.resume();
            }
            catch (InterruptedException exc) { }
            catch (VMDisconnectedException discExc) { exiting = true; }
        }
    }
    
    /**
     * Wait until the event queue is empty (all pending events have been dispatched).
     */
    public void waitQueueEmpty()
    {
        synchronized (this) {
            try {
                while (! queueEmpty) {
                    wait();
                }
            }
            catch (InterruptedException ie) {}
        }
    }
    
    private void handleEvent(Event event)
    {
        if (event instanceof VMStartEvent) {
            vm.vmStartEvent((VMStartEvent) event);
        } else if (event instanceof VMDeathEvent) {
            // vm.vmExitEvent();
        } else if (event instanceof VMDisconnectEvent) {
            vm.vmDisconnectEvent();
        } else if (event instanceof ExceptionEvent) {
            vm.exceptionEvent((ExceptionEvent)event);
        } else if (event instanceof BreakpointEvent) {
            vm.breakpointEvent((LocatableEvent)event, true);
        } else if (event instanceof StepEvent) {
            vm.breakpointEvent((LocatableEvent)event, false);
        } else if (event instanceof ThreadStartEvent) {
            vm.threadStartEvent((ThreadStartEvent)event);
        } else if (event instanceof ThreadDeathEvent) {
            vm.threadDeathEvent((ThreadDeathEvent)event);
        } else if (event instanceof ClassPrepareEvent) {
            classPrepareEvent(event);
        } else {
            //Debug.message("[VM event] unhandled: " + event);
        }
    }
    
    private boolean classPrepareEvent(Event event)
    {
        ClassPrepareEvent cle = (ClassPrepareEvent)event;
        ReferenceType refType = cle.referenceType();
        
        if(refType.name().equals(VMReference.SERVER_CLASSNAME)) {
            vm.serverClassPrepared();
        }
        return true;
    }
    
}
