package bluej.groupwork;

import java.lang.*;

/**
** This Class represents a group job that is executed by the runner
** and monitored by the monitor  
**
** @author Markus Ostman, the structure of this class is similar to 
** CVSThread from jCVS, written by Tim Endres. This is not a thread though.
**
**/
public class GroupJob
{
    protected Monitor	 monitor;
    protected Runnable	 subRunner;
    private String       name;
    
    /**
     * Construction
     */
    public GroupJob( String name, Runnable runner, Monitor monitor )
    {
        this.name = name;
        this.monitor = monitor;
        this.subRunner = runner;
    }
    
    public void process()
    {
        this.monitor.threadStarted();
        
        this.subRunner.run();
        
        this.monitor.threadFinished();
    }
    
    public interface Monitor
    {
        public void threadStarted();
        
        public void threadCanceled();

        public void threadFinished();
    }
    
}
