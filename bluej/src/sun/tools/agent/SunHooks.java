package sun.tools.agent;

public class SunHooks 

    implements AgentConstants
{
    public static void die()
    {
	try {
	    Agent.the_Agent.asyncOutputStream.write(CMD_THREADDEATH_NOTIFY);
	    Agent.the_Agent.writeObject(Thread.currentThread(), Agent.the_Agent.asyncOutputStream);
	    Agent.the_Agent.asyncOutputStream.flush();
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	}
    }
}
