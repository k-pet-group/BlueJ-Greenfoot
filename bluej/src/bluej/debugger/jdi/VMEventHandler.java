package bluej.debugger.jdi;

import bluej.utility.Debug;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.EventRequest;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

/**
 ** Event handler class to handle events coming from the remote VM.
 **
 ** @author Michael Kolling
 **/

public class VMEventHandler implements Runnable {

    JdiDebugger debugger;
    Thread thread;
    VirtualMachine vm;
    EventQueue queue;
    volatile boolean exiting = false;
    boolean exited = false;

    VMEventHandler(JdiDebugger debugger, VirtualMachine vm) {
        this.debugger = debugger;
	this.vm = vm;
	queue = vm.eventQueue();
        thread = new Thread(this, "vm-event-handler"); 
        thread.start();  // will execute our own run method
    }

    public void run() {
        while (!exiting) {
            try {
		EventSet eventSet = queue.remove();

                boolean resumeStoppedApp = false;
		// ** I am not so sure about this - it seams I should only
		// handle one event of every group...?! **

                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    resumeStoppedApp |= handleEvent(it.nextEvent());
                }

                // Note: we never request events to suspend just the event
                // thread, so there's no code here to handle that case.
                if (eventSet.suspendPolicy() == EventRequest.SUSPEND_ALL) {
                    if (resumeStoppedApp) {
                        vm.resume();
                    } else {
                        //notifier.vmInterrupted();
                    }
                }
            } catch (InterruptedException exc) {
                // Do nothing. Any changes will be seen at top of loop.
            } catch (VMDisconnectedException discExc) {
                handleDisconnectedException();
                break;
            }
        }
        synchronized (this) {
            exited = true;
            notifyAll();
        }
    }

    private boolean handleEvent(Event event) {
        if (event instanceof ExceptionEvent) {
            return exceptionEvent(event);
        } else if (event instanceof BreakpointEvent) {
            return breakpointEvent(event);
        } else if (event instanceof StepEvent) {
            return stepEvent(event);
        } else if (event instanceof ClassPrepareEvent) {
            return classPrepareEvent(event);
        } else if (event instanceof VMDeathEvent) {
            return handleExitEvent(event);
        } else if (event instanceof VMDisconnectEvent) {
            return handleExitEvent(event);
        } else {
	    //Debug.message("[VM event] unhandled: " + event);
            return true;
        }
    }

    private boolean vmDied = false;
    private boolean handleExitEvent(Event event) {
        if (event instanceof VMDeathEvent) {
            vmDied = true;
            return vmDeathEvent(event);
        } else if (event instanceof VMDisconnectEvent) {
            exiting = true;
            if (!vmDied) {
                vmDisconnectEvent(event);
            }
            Debug.message("[VM Event] exiting..!?");
            return false;
        } 
	else {
            return false;
        }
    }

    synchronized void handleDisconnectedException() {
        /*
         * A VMDisconnectedException has happened while dealing with
         * another event. We need to flush the event queue, dealing only
         * with exit events (VMDeath, VMDisconnect) so that we terminate
         * correctly.
         */
            Debug.message("[VM Event] unexpected disconnection");

//          while (!exiting) {
//              try {
//                  EventSet eventSet = queue.remove();
//                  EventIterator iter = eventSet.eventIterator();
//                  while (iter.hasNext()) {
//                      handleExitEvent((Event)iter.next());
//                  }
//              } catch (InterruptedException exc) {
//                  // ignore
//              }
//          }
    }

    private boolean breakpointEvent(Event event)
    {
        debugger.breakEvent((LocatableEvent)event, true);
        return false;
    }

    private boolean stepEvent(Event event)
    {
        debugger.breakEvent((LocatableEvent)event, false);
        return false;
    }

    private boolean classPrepareEvent(Event event)
    {
	ClassPrepareEvent cle = (ClassPrepareEvent)event;
	ReferenceType refType = cle.referenceType();

	if(refType.name().equals(JdiDebugger.SERVER_CLASSNAME)) {
	    debugger.serverClassPrepared();
	}
	return true;
    }

    /**
     * Called when an exception was thrown in the remote VM.
     */
    private boolean exceptionEvent(Event event)
    {
        debugger.exceptionEvent((ExceptionEvent)event);
	return true;
    }

    private boolean threadDeathEvent(Event event)
    {
	Debug.message("[VM Event] threadDeathEvent");
//          ThreadDeathEvent tee = (ThreadDeathEvent)event;
//          ThreadReference thread = tee.thread();
//          if (ThreadInfo.current != null && 
//  	    ThreadInfo.current.equals(thread)) {

//              String currentThreadName;
            
//              // Be careful getting the thread name. If this event happens
//              // as part of VM termination, it may be too late to get the 
//              // information, and an exception will be thrown.
//              try {
//  		currentThreadName = " \"" + thread.name() + "\"";
//              } catch (Exception e) {
//  		currentThreadName = "";
//              }
                 
//              setCurrentThread(null);

//              Env.errorln("");
//              Env.errorln("Current thread" + currentThreadName + 
//                          " died. Execution continuing...");
//          }
        return false;
    }

    public boolean vmDeathEvent(Event event)
    {
	Debug.message("[VM Event] vmDeathEvent");
//          String app = null;
//          if (Env.lastArgs() != null) {
//              StringTokenizer t = new StringTokenizer(Env.lastArgs());
//              if (t.hasMoreTokens()) {
//                  app = t.nextToken();
//              }
//          }
//          if (app == null) {
//              app = "The application";
//          }
//          shutdownMessage = "\n" + app + " exited";
        return false;
    }

    public boolean vmDisconnectEvent(Event event) 
    {
	Debug.message("[VM Event] vmDisconnectEvent");
//          String app = null;
//          if (Env.lastArgs() != null) {
//              StringTokenizer t = new StringTokenizer(Env.lastArgs());
//              if (t.hasMoreTokens()) {
//                  app = t.nextToken();
//              }
//          }
//          if (app == null) {
//              app = "The application";
//          }
//          shutdownMessage = "\n" + app + " has been disconnected";
        return false;
    }

    /**
     * The debugger is about to exit. Finish this thread.
     */
    synchronized void finish() {
        exiting = true;
        thread.interrupt();
        while (!exited) {
            try {wait();} catch (InterruptedException exc) {}
        }
    }

}
