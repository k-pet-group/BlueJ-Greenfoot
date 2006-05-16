package rmiextension;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RBlueJImpl;
import bluej.extensions.BlueJ;

/**
 * Starts the registry and BlueJ-service.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: BlueJRMIServer.java 4281 2006-05-16 16:46:42Z polle $
 */
public class BlueJRMIServer
{
	public final static String BLUEJ_SERVICE = "//127.0.0.1/BlueJService";
    public BlueJRMIServer(BlueJ blueJ) throws RemoteException
    {
        try {
            Registry reg = LocateRegistry.createRegistry(1099);
            blueJ.addPackageListener(ProjectManager.instance());
            RBlueJ rBlueJ = new RBlueJImpl(blueJ);
            Naming.rebind(BLUEJ_SERVICE, rBlueJ);
        }
        catch (MalformedURLException e) {
        	e.printStackTrace();
        }
    }
}