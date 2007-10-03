package rmiextension;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.Permission;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RBlueJImpl;
import bluej.extensions.BlueJ;
import bluej.utility.Debug;

/**
 * Starts the registry and BlueJ-service.
 * <p>
 * 
 * To support multiple instances of greenfoot running at the same time the
 * registry will be started on a random free port.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: BlueJRMIServer.java 5268 2007-10-03 18:32:44Z polle $
 */
public class BlueJRMIServer
{

    private final static String HOST = "127.0.0.1";
    private final static String BLUEJ_SERVICE = "BlueJService";
    private static int port;

    /** How many unsuccessful attempts that have been made to start the registry */
    private int registryStartAttempts = 0;
    /** How many time to we retry before giving up */
    private static final int MAX_REGISTRY_START_ATTEMPTS = 10;

    /**
     * Returns the BlueJ service.
     * <p>
     * Can only be called after the registry has been started.
     * 
     * @return URL containing host, port and service name.
     */
    public static String getBlueJService()
    {
        if (port == 0) {
            throw new IllegalStateException("Registry not started.");
        }
        return "//" + HOST + ":" + port + "/" + BLUEJ_SERVICE;
    }
    
    /**
     * Make sure that the localhost is used for the server. If this is not done,
     * RMI will use the outgoing IP address if there is one, and loosing this IP
     * address will make RMI calls impossible and hence lock up Greenfoot.
     */
    public static void forceHostForServer()
    {
        System.setProperty("java.rmi.server.hostname", HOST);
    }

    /**
     * Creates a new RMI server and exports the argument as a BlueJService. The
     * registry will be started on a free port, NOT the default RMI port.
     * 
     * @throws IOException If for some reason the server could not be created.
     */
    public BlueJRMIServer(BlueJ blueJ)
        throws IOException
    {
    	if (System.getSecurityManager() == null) {
    		// If there's no security manager, the registry will
    		// (stupidly) refuse to load classes from the class path
    		// that aren't visible to the system class loader.
    		System.setSecurityManager(new SecurityManager() {
    			public void checkPermission(Permission perm) {
    				// super.checkPermission(perm);
    				return;
    			}

    			public void checkPermission(Permission perm, Object context) {
    				// super.checkPermission(perm, context);
    				return;
    			}
    		});
    	}
    	
        startRegistry();
        blueJ.addPackageListener(ProjectManager.instance());
        RBlueJ rBlueJ = new RBlueJImpl(blueJ);
        Naming.rebind(getBlueJService(), rBlueJ);
    }

    /**
     * Starts the registry.
     * 
     * @throws IOException If the registry could not be started - even after
     *             several retries.
     */
    private void startRegistry()
        throws IOException
    {
        forceHostForServer();
        boolean success = false;
        while (!success && registryStartAttempts < MAX_REGISTRY_START_ATTEMPTS) {
            try {
                port = getFreePort();
            }
            catch (IOException e1) {
                //
                Debug.reportError("Could not obtain a free port number. Attempt number: " + registryStartAttempts);
                e1.printStackTrace();
                registryStartAttempts++;
                continue;
            }

            try {
                LocateRegistry.createRegistry(port);
                success = true;
            }
            catch (RemoteException re) {
                // One reason could be that the port that was reported as free
                // is no longer free.
                Debug.reportError("Could not start registry. Attempt number: " + registryStartAttempts);
                re.printStackTrace();
                registryStartAttempts++;
                continue;
            }
        }
        if (!success) {
            throw new IOException("Could not start the registry.");
        }

    }

    /**
     * Gets a port that is currently unused. When using the returned port, it
     * might no longer be free so appropiate exception handling should be in
     * place.
     * 
     * @return A free port number.
     * @throws IOException If something goes wrong when trying to obtain a free
     *             port number.
     */
    private int getFreePort()
        throws IOException
    {
        ServerSocket s = new ServerSocket(0);
        int freePort = s.getLocalPort();
        s.close();
        return freePort;
    }
}