package greenfoot.gui.classbrowser.role;

import greenfoot.gui.classbrowser.ClassView;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.FileWriter;
import java.rmi.RemoteException;

import javax.swing.JLabel;

import rmiextension.wrappers.RClass;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NormalClassRole.java 3124 2004-11-18 16:08:48Z polle $
 */
public class NormalClassRole extends ClassRole
{

    public void buildUI(ClassView classView, RClass rClass)
    {

        //TODO get this color from the bluej config
        //component.setBackground(new Color(245, 204, 155));
        classView.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;

        String name = "";
        try {
            name = rClass.getQualifiedName();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        JLabel className = new JLabel(name);

        classView.add(className, c);
    }

    public void createSkeleton(String className, String superClassName, FileWriter writer)
    {

    }

}