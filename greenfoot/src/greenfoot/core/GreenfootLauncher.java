package greenfoot.core;

import greenfoot.util.GreenfootLogger;

import java.io.File;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import rmiextension.BlueJRMIClient;
import rmiextension.wrappers.RBlueJ;
import bluej.Config;

/**
 * An object of GreenfootLauncher is the first object that is created in the
 * Debug-VM. The launcher object is created from the BlueJ-VM in the Debug-VM.
 * When a new object of the launcher is created, the constructor looks up the
 * BlueJService in the RMI registry and starts the initialisation of greenfoot.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version 22-05-2003
 * @version $Id$
 */
public class GreenfootLauncher
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    public GreenfootLauncher()
    {
        GreenfootLogger.init();

        BlueJRMIClient client = BlueJRMIClient.instance();

        if (client != null) {
            RBlueJ blueJ = client.getBlueJ();
            logger.info("RMIClient service found!");
            try {
                File libdir = blueJ.getSystemLibDir();
                Config.initializeVMside(libdir, client);
                Greenfoot.initialize(blueJ, client.getPackage());
                logger.info("Greenfoot initialized");
            }
            catch (RemoteException re) {
                // TODO better error handling.
                logger.severe("RemoteException occurred. Cannot initialize greenfoot.");
            }
        }
        else {
            //TODO error message / exception
            logger.severe("Could not find the RMIClient");
        }
    }

}