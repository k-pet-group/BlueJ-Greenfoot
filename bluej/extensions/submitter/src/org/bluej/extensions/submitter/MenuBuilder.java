package org.bluej.extensions.submitter;

import bluej.extensions.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  Description of the Class
 */
public class MenuBuilder extends MenuGenerator implements ActionListener
{
    private Stat stat;
    private String aLabel;


    /**
     * I need to put stat in a safe place, I need it later
     *
     * @param  i_stat  Description of the Parameter
     */
    MenuBuilder(Stat i_stat)
    {
        stat = i_stat;
        aLabel = stat.bluej.getLabel("menu.submit");
    }


    /**
     * @param  aPackage  Description of the Parameter
     * @return           The toolsMenuItem value
     */
//    public JMenuItem getToolsMenuItem(BPackage aPackage)
    public JMenuItem getMenuItem()
    {
//    System.out.println ("Submitter.MenuBuilder.getToolsMenuItem aPackage="+aPackage);
        JMenuItem anItem = new JMenuItem(aLabel);
        anItem.setEnabled(isMenuEnabled(stat.bluej.getCurrentPackage()));
        anItem.addActionListener(this);
        return anItem;
    }

    private boolean isMenuEnabled ( BPackage aPackage )
    {
        // Menu is not enabled if there is no package !
        if ( aPackage == null ) return false;

        try {
            BProject bproj = aPackage.getProject();

            return stat.treeData.haveConfiguration(bproj.getDir());
        } catch ( Exception exc ) {
            return false;
        }
       
    }


    /**
     *  Called when a menu is selected
     *
     * @param  anEvent  Description of the Parameter
     */
    public void actionPerformed(ActionEvent anEvent)
    {
      /* If we can't get the details of the current package, just return
       * The package could still go away later, but we'll cope with that
       * through "file not found" when we go looking.
       * It's more likely to have been closed than deleted anyway.
       */
        try {
            BPackage bpkg = stat.bluej.getCurrentPackage();
            // If package has already gone away just return...
            if (bpkg == null) return;

            BProject bproj = bpkg.getProject();
            bproj.save();

            // Try to submit this project
            stat.submitDialog.submitThis(bproj.getDir(), bproj.getName());
        } catch (ExtensionException e) {}
    }
}

