package greenfoot.gui;

import greenfoot.util.GreenfootUtil;

import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.debugmgr.CallHistory;
import bluej.debugmgr.MethodDialog;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.views.CallableView;

/**
 * MethodDialog for Greenfoot. With Greenfoot in the title instead of BlueJ.
 * 
 * @author Poul Henriksen
 */
public class GreenfootMethodDialog extends MethodDialog
{

    public GreenfootMethodDialog(JFrame parentFrame, ObjectBenchInterface ob, CallHistory callHistory,
            String instanceName, CallableView method, Map typeMap)
    {
        super(parentFrame, ob, callHistory, instanceName, method, typeMap);

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                GreenfootUtil.makeGreenfootTitle(GreenfootMethodDialog.this);
            }
        });
    }

}
