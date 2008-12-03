package rmiextension;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import bluej.BlueJPropStringSource;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;

/**
 * The RMI client that establishes the initial connection to the BlueJ RMI server
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: BlueJRMIClient.java 6006 2008-12-03 19:08:29Z polle $
 */
public class BlueJRMIClient implements BlueJPropStringSource
{

    RBlueJ blueJ = null;
    private static BlueJRMIClient instance;

    private RPackage pkg;

    public BlueJRMIClient(String prjDir, String rmiServiceName)
    {
        BlueJRMIServer.forceHostForServer();
        instance = this;

        try {
        	blueJ = (RBlueJ) Naming.lookup(rmiServiceName);
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
                RProject prj = null;
                for (int i = 0; i < openProjects.length; i++) {
                    prj = openProjects[i];
                    File passedDir = new File(prjDir);
                    if (prj.getDir().equals(passedDir)) {
                        break;
                    }
                }
                pkg = prj.getPackage("");

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
     * Returns the remote BlueJ instance.
     */
    public RBlueJ getBlueJ()
    {
        return blueJ;
    }
    
    /**
     * Returns the remote BlueJ package.
     */
    public RPackage getPackage()
    {
        return pkg;
    }
    
    // Implementation for "BlueJPropStringSource" interface
    
    public String getBlueJPropertyString(String property, String def)
    {
        try {
            String val = blueJ.getExtensionPropertyString(property, null);
            if (val == null)
                val = blueJ.getBlueJPropertyString(property, def);
            return val;
        }
        catch (RemoteException re) {
            return def;
        }
    }

    public String getLabel(String key)
    {
        return Config.getString(key, key);
    }
    
    public void setUserProperty(String property, String val)
    {
        try {
            blueJ.setExtensionPropertyString(property, val);
        }
        catch (RemoteException re) {}
    }
    
    public Properties getInitialCommandLineProperties()
    {
        try {
            return blueJ.getInitialCommandLineProperties();
        }
        catch (RemoteException e) {
            return null;
        }
    }
}