package bluej.extmgr;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefPanelListener;
import bluej.prefmgr.PrefMgrDialog;

import java.util.List;
import java.util.Iterator;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;

import bluej.extensions.PrefGen;
import bluej.extensions.BlueJ;

/**
 * This manages the whole preference pane for Extensions
 * It will be loaded in the appropriate tab when the register() is called
 */
public class PrefManager implements PrefPanelListener
{
    private ExtensionsManager extensionsManager;

    private final int   DO_panelUpdate=1;
    private final int   DO_loadValues=2;
    private final int   DO_saveValues=3;

    private JPanel      drawPanel;
    private JPanel      rootPanel;

    /**
     * The manager needs to know who is the extension manager.
     * Well, this is just to have the list of extensions...
     */
    public PrefManager(ExtensionsManager extensionsManager) 
    {
        this.extensionsManager = extensionsManager;

        // I need a draw panel Components in here should be laid on the top - down
        drawPanel = new JPanel();
        drawPanel.setLayout(new BoxLayout(drawPanel,BoxLayout.Y_AXIS));

        // I need a middle panel just to pack everything up
        JPanel middlePanel = new JPanel (new BorderLayout());
        middlePanel.add(drawPanel,BorderLayout.NORTH);

        // And I need to put this panel into a scroll pane
        JScrollPane drawScroll = new JScrollPane (middlePanel);
        drawScroll.setBorder(new EmptyBorder(0,0,0,0));
        drawScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        /* Add the scroll pane to the root panel
         * It needs to use all the available space, othervise the scroll pane does not
         * understand when to draw its scrollbars
         */
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(drawScroll,BorderLayout.CENTER);

        // After all of this I may join the club in the main BlueJ
        PrefMgrDialog.add(rootPanel, Config.getString("extmgr.extensions"), this);
    }


    /**
     * This is the looper, I will use some const to decide at the end
     * what to do. Just to make code simples and cleaner
     * Note that half or more of the code is on Fault managment ...
     */
    private void doWorkLoop( int doAction ) 
    {
        // I need to remove all content, in any case...
        if ( doAction == DO_panelUpdate ) drawPanel.removeAll();
      
        List allExtensions = extensionsManager.getExtensions();
        // In theory I should never get null, in theory...
        if ( allExtensions == null ) return;

        for ( Iterator iter=allExtensions.iterator(); iter.hasNext(); )
          doWorkItem ((ExtensionWrapper)iter.next(),doAction);
    }

    /**
     * Do some work on one extension wrapper.
     * It is either adding panels, saving or loading...
     */
    private void doWorkItem( ExtensionWrapper aWrapper, int doAction ) 
    {
        // This extension is not valid, let me skip it
        if ( ! aWrapper.isValid() ) return;
        String extensionName = aWrapper.getExtensionClassName();
    
        BlueJ aBluej = aWrapper.getBluej();
        // Can a wrapper not have bluej ? ... yes, it happens....
        if ( aBluej == null ) 
            return;

        PrefGen aPrefPanel = aBluej.getPrefGen();
        // An extension may not have a preference panel
        if ( aPrefPanel == null ) return;

        switch (doAction) 
        {
        case DO_loadValues:  
            aPrefPanel.loadValues();   
            return;
        case DO_saveValues:  
            aPrefPanel.saveValues();   
            return;
        case DO_panelUpdate: 
            addUserPanel (aPrefPanel, extensionName); 
            return;
          }
        }
  
    /**
     * Being here to make code cleaner. 
     * Given an Extension preference panel add it into the main panel
     */
    private void addUserPanel( PrefGen aPrefPanel, String extensionName ) 
    {
        JPanel aPanel = aPrefPanel.getPanel(); 
        if ( aPanel == null ) {
          // The extension coder has a PrefPanel but not a JPanel, BAD, better tell it
          System.out.println ("BPrefPanel: addUserPanel: getPanel should return a JPanel");
          return;
          }

        // The panel that the user gives me goes into a container pane
        JPanel framePanel = new JPanel(new BorderLayout());
        framePanel.setBorder(BorderFactory.createTitledBorder(extensionName));

        // The panel that the user gives me goes into the north, packed
        framePanel.add (aPanel,BorderLayout.NORTH);

        // Finally put this panel into the drawing panel, it will be stacket Y axis
        drawPanel.add (framePanel);
    }


    /**
     * To start the revalidation of the panels associated to the extensions you
     * use this one. NOTE that swing will be accessing the extensions generated objects
     * in a NON syncronized way, there is a sort of risk here.
     */
    public void panelRevalidate() 
    {
        EventQueue.invokeLater(new PrefManager.DoPanelUpdate());
    }
    
    /**
     * Nothing much to do, this is to satisfy the invokeLater
     */
    private class DoPanelUpdate implements Runnable
      {
      public void run()
        {
        doWorkLoop (DO_panelUpdate);
        }
      }

    /**
     * Needed only to satisfy the implements
     */
    public void beginEditing()  {  }
    
    
    /**
     * Called by the system when it is time to reload the panel values
     */
    public void revertEditing() 
    {
        doWorkLoop (DO_loadValues);
     }

    /**
     * Called by the system when the user has pressed the OK buton
     */
    public void commitEditing()
    {
        doWorkLoop (DO_saveValues);
    }

}
