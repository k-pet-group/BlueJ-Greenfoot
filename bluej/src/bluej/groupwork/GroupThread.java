package bluej.groupwork;

import java.lang.*;


public class GroupThread extends Thread
{
    //protected Monitor		monitor;
    protected Runnable		subRunner;
    
    
    /**
     * We severely restrict construction!
     */
    private GroupThread() { }
    private GroupThread( String name ) { }
    private GroupThread( Runnable runner ) { }
    private GroupThread( ThreadGroup group, String name ) { }
    private GroupThread( ThreadGroup group, Runnable runner, String name ) { }


    public GroupThread( String name, Runnable runner)
    {
        super( name );
        //this.monitor = monitor;
        this.subRunner = runner;
    }
    
    public void run()
    {
        //this.monitor.threadStarted();
        
        this.subRunner.run();
        
        //this.monitor.threadFinished();
    }

 //    public interface Monitor
//     {
//         public void threadStarted();

//         public void threadCanceled();

//         public void threadFinished();
//     }
    
}
