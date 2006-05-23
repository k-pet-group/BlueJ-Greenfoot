package greenfoot.core;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;

import javax.swing.ImageIcon;

import rmiextension.BlueJRMIClient;
import rmiextension.wrappers.RBlueJ;
import bluej.BlueJTheme;
import bluej.Config;

/**
 * An object of GreenfootLauncherDebugVM is the first object that is created in the
 * Debug-VM. The launcher object is created from the BlueJ-VM in the Debug-VM.
 * When a new object of the launcher is created, the constructor looks up the
 * BlueJService in the RMI registry and starts the initialisation of greenfoot.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version 22-05-2003
 * @version $Id$
 */
public class GreenfootLauncherDebugVM
{
    public GreenfootLauncherDebugVM(String prjDir, String pkgName)
    {
        BlueJRMIClient client = new BlueJRMIClient(prjDir, pkgName);
        
        RBlueJ blueJ = client.getBlueJ();
        try {
            File libdir = blueJ.getSystemLibDir();
            Config.initializeVMside(libdir, client);
            
            URL iconFile = this.getClass().getClassLoader().getResource("greenfoot.png");
            ImageIcon icon = new ImageIcon(iconFile);
            BlueJTheme.setIconImage(icon.getImage());

            GreenfootMain.initialize(blueJ, client.getPackage());
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
    }
}
