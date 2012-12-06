/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012  Michael Kolling and John Rosenberg 
 
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
package bluej.compiler;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * This class adapts CompileObserver messages to run on the GUI thread.
 * 
 * @author Davin McCall
 */
final public class EventqueueCompileObserver
    implements CompileObserver, Runnable
{
    private CompileObserver link;
    private int command;
    
    private static final int COMMAND_START = 0;
    private static final int COMMAND_DIAG = 1;
    private static final int COMMAND_END = 2;
    
    // parameters for COMMAND_START/COMMAND_END
    private File [] sources;
    private boolean successful;  // COMMAND_END only
    
    // parameters for COMMAND_DIAG
    private Diagnostic diagnostic;
    // return value for COMMAND_DIAG:
    private boolean wasShown;
    
    /**
     * Constructor for EventqueueCompileObserver. The link parameter is a compiler
     * observer; all messages will be passed on to it, but on the GUI thread.
     */
    public EventqueueCompileObserver(CompileObserver link)
    {
        this.link = link;
    }
    
    /**
     * This method switches execution to the GUI thread.
     */
    private void runOnEventQueue()
    {
        try {
            EventQueue.invokeAndWait(this);
        }
        catch (InterruptedException ie) {}
        catch (InvocationTargetException ite) { throw new RuntimeException(ite); }
    }
    
    // ---------------- CompileObserver interface ---------------------
    
    public synchronized boolean compilerMessage(Diagnostic diagnostic)
    {
        command = COMMAND_DIAG;
        this.diagnostic = diagnostic;
        runOnEventQueue();
        return wasShown;
    }
    
    public synchronized void startCompile(File[] csources)
    {
        command = COMMAND_START;
        this.sources = csources;
        runOnEventQueue();
    }

    public synchronized void endCompile(File[] sources, boolean successful)
    {
        command = COMMAND_END;
        this.sources = sources;
        this.successful = successful;
        runOnEventQueue();

    }
    
    // ------------------ Runnable interface ---------------------
    
    public void run()
    {
        // We're now running on the GUI thread. Call the chained compile observer.
        
        switch (command) {
            case COMMAND_START:
                link.startCompile(sources);
                break;
            case COMMAND_DIAG:
                wasShown = link.compilerMessage(diagnostic);
                break;
            case COMMAND_END:
                link.endCompile(sources, successful);
                break;
        }
    }

}
