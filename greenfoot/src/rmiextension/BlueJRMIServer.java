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
 * @version $Id: BlueJRMIServer.java 3124 2004-11-18 16:08:48Z polle $
 */
public class BlueJRMIServer
{
    public BlueJRMIServer(BlueJ blueJ)
    {
        try {
            Registry reg = LocateRegistry.createRegistry(1099);
            blueJ.addPackageListener(ProjectManager.instance());
            blueJ.addCompileListener(ProjectManager.instance());
            RBlueJ rBlueJ = new RBlueJImpl(blueJ);
            Naming.rebind("//127.0.0.1/BlueJService", rBlueJ);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}