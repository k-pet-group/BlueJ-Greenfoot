package greenfoot;

import greenfoot.util.GreenfootLogger;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import rmiextension.BlueJRMIClient;
import rmiextension.wrappers.RBlueJ;

/**
 * An object of GreenfootLauncher is the first object that is created in the
 * Debug-VM. The launcher object is created from the BlueJ-VM in the Debug-VM.
 * When a new object of the launcher is created, the constructor looks up the
 * BlueJService in the RMI registry and starts the initialisation of greenfoot.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version 22-05-2003
 * @version $Id: GreenfootLauncher.java 3124 2004-11-18 16:08:48Z polle $
 */
public class GreenfootLauncher
{
    RBlueJ blueJ = null;
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    public GreenfootLauncher()
        throws RemoteException
    {
        GreenfootLogger.init();

        BlueJRMIClient client = BlueJRMIClient.instance();

        if (client != null) {
            RBlueJ blueJ = client.getBlueJ();
            logger.info("RMIClient service found!");
            Greenfoot.initialize(blueJ, client.getProject(), client.getPackage());
            logger.info("Greenfoot initialized");
        }
        else {
            //TODO error message / exception
            logger.severe("Could not find the RMIClient");
        }
    }

}