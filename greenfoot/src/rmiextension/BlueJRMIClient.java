package rmiextension;

import greenfoot.util.GreenfootLogger;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: BlueJRMIClient.java 3124 2004-11-18 16:08:48Z polle $
 */
public class BlueJRMIClient
{

    RBlueJ blueJ = null;
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private static BlueJRMIClient instance;

    private RProject project;
    private RPackage pkg;

    public BlueJRMIClient(String prjDir, String pkgName)
        throws RemoteException
    {
        GreenfootLogger.init();

        instance = this;

        try {
            logger.info("Looking for BlueJRMIClient RMI service ");
            blueJ = (RBlueJ) Naming.lookup("rmi://localhost/BlueJService");
            logger.info("BlueJRMIClient RMI service found");

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (NotBoundException e) {
            e.printStackTrace();
        }

        if (blueJ != null) {
            try {
                RProject[] openProjects = blueJ.getOpenProjects();
                for (int i = 0; i < openProjects.length; i++) {
                    RProject prj = openProjects[i];
                    File passedDir = new File(prjDir);
                    if (prj.getDir().equals(passedDir)) {
                        project = prj;
                        break;
                    }
                }
                pkg = project.getPackage(pkgName);

            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
        }

    }

    public static BlueJRMIClient instance()
    {
        return instance;
    }

    /**
     * @return
     */
    public RBlueJ getBlueJ()
    {
        return blueJ;
    }

    public RProject getProject()
    {
        return project;
    }

    public RPackage getPackage()
    {
        return pkg;
    }
}