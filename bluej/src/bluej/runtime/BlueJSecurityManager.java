package bluej.runtime;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

/**
 ** A SecurityManager for the BlueJ runtime
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **/
public class BlueJSecurityManager extends SecurityManager
{
    /**
     * The only thing BlueJ applications are currently not allowed to
     * do is exit normally. We handle this by signalling the exit as
     * a special exception which we catch later in in the debugger.
     *
     * @param status   the exit status.
     */
    public void checkExit(int status)
    {
	if(currentClassLoader() != null)
	    throw new SecurityException("BlueJ-Exit:" + status + ":");
    }

    /**
     * This method is called at several differnt placed to check whether 
     * certain actions are legal. Here, we want to allow (contrary to the
     * default) to re-direct the IO stream.
     */
    public void checkPermission(Permission perm)
    {
	if(perm.getName().equals("setIO"))
	    return;	// allow
	else
	    super.checkPermission(perm);
    }

    public void checkAccept(String host, int port) {}
    public void checkAccess(Thread g) {}
    public void checkAccess(ThreadGroup g) {}
    public void checkAwtEventQueueAccess() {}
    public void checkConnect(String host, int port) {}
    public void checkConnect(String host, int port, Object context) {}
    public void checkCreateClassLoader() {}
    public void checkDelete(String file) {}
    public void checkExec(String cmd) {}
    // checkExit is redefined above
    public void checkLink(String lib) {}
    public void checkListen(int port) {}
    public void checkMemberAccess(Class clazz, int which) {}
    public void checkMulticast(InetAddress maddr) {}
    public void checkMulticast(InetAddress maddr, byte ttl) {}
    public void checkPackageAccess(String pkg) {}
    public void checkPackageDefinition(String pkg) {}
    public void checkPrintJobAccess() {}
    public void checkPropertiesAccess() {}
    public void checkPropertyAccess(String key) {}
    public void checkRead(FileDescriptor fd) {}
    public void checkRead(String file) {}
    public void checkRead(String file, Object context) {}
    public void checkSecurityAccess(String provider) {}
    public void checkSetFactory() {}
    public void checkSystemClipboardAccess() {}
    public boolean checkTopLevelWindow(Object window) { return true; }
    public void checkWrite(FileDescriptor fd) {}
    public void checkWrite(String file) {}
	
    ThreadGroup threadGroup;
    public void setThreadGroup(ThreadGroup threadGroup)
    {
	this.threadGroup = threadGroup;
    }
	
    public ThreadGroup getThreadGroup()
    {
	if(threadGroup != null)
	    return threadGroup;
	else
	    return Thread.currentThread().getThreadGroup();
    }
}
