package org.bluej.extensions.submitter;

import bluej.extensions.*;
import java.awt.event.*;
import javax.swing.*;

public class MenuBuilder extends MenuGenerator 
  {
  private Stat stat;  
  private MenuAction anAction;

  /**
   * I need to put stat in a safe place, I need it later
   */
  MenuBuilder ( Stat i_stat )
    {
    stat = i_stat;
    
    String aLabel = stat.bluej.getLabel("menu.submit");
    anAction = new MenuAction(aLabel);
    }

  /**
   */
  public JMenuItem getToolsMenuItem(BPackage aPackage)
    {
//    System.out.println ("Submitter.MenuBuilder.getToolsMenuItem aPackage="+aPackage);
    anAction.setEnabled(aPackage!=null);
    return new JMenuItem(anAction);
    }

 /**
   * This is the action that has to be performed when the given menu is selected
   * It is fairly flexible to use and the parameters are just an example...
   */
  class MenuAction extends AbstractAction
    {

    /**
     * Constructor for the MenuAction object
     */
    public MenuAction(String menuName)
      {
      putValue(AbstractAction.NAME, menuName);
      }

    /**
     *  Called when a menu is selected
     */
    public void actionPerformed(ActionEvent anEvent)
      {
      /*
       * If we can't get the details of the current package, just return
       * The package could still go away later, but we'll cope with that
       * through "file not found" when we go looking.
       * It's more likely to have been closed than deleted anyway.
       */
      try {
          BPackage bpkg = stat.bluej.getCurrentPackage();
          if (bpkg == null) return;     // package has already gone away
              
          BProject bproj = bpkg.getProject();
          bproj.save();
      
          // Try to submit this project
          stat.submitDialog.submitThis ( bproj.getDir(), bproj.getName() );
      } catch (ExtensionException e ) {}
      }
    }
  
}