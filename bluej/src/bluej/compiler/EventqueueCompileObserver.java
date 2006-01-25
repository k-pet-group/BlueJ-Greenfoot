package bluej.compiler;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * This class adapts CompileObserver messages to run on the GUI thread.
 * 
 * @author Davin McCall
 * @version $Id$
 */
final public class EventqueueCompileObserver
    implements CompileObserver, Runnable
{
    private CompileObserver link;
    private int command;
    
    private static final int COMMAND_START = 0;
    private static final int COMMAND_ERROR = 1;
    private static final int COMMAND_WARNING = 2;
    private static final int COMMAND_END = 3;
    
    // parameters for COMMAND_START/COMMAND_END
    private File [] sources;
    private boolean successful;  // COMMAND_END only
    
    // parameters for COMMAND_ERROR/COMMAND_WARNING
    private String filename;
    private int lineNo;
    private String message;
    
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
    
    public synchronized void startCompile(File[] csources)
    {
        command = COMMAND_START;
        this.sources = csources;
        runOnEventQueue();
    }

    public synchronized void errorMessage(String filename, int lineNo, String message)
    {
        command = COMMAND_ERROR;
        this.filename = filename;
        this.lineNo = lineNo;
        this.message = message;
        runOnEventQueue();
    }

    public synchronized void warningMessage(String filename, int lineNo, String message)
    {
        command = COMMAND_WARNING;
        this.filename = filename;
        this.lineNo = lineNo;
        this.message = message;
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
            case COMMAND_ERROR:
                link.errorMessage(filename, lineNo, message);
                break;
            case COMMAND_WARNING:
                link.warningMessage(filename, lineNo, message);
                break;
            case COMMAND_END:
                link.endCompile(sources, successful);
        }
    }

}
