package bluej.extmgr;

import bluej.Config;
import bluej.prefmgr.PrefPanelListener;
import bluej.prefmgr.PrefMgrDialog;

import java.util.List;
import java.util.Iterator;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * This manages the whole preference pane for Extensions
 * It will be loaded in the appropriate tab when the register() is called
 */
public class PrefManager implements PrefPanelListener
{
    private List        extensionsList;

    private final int   DO_panelUpdate=1;
    private final int   DO_loadValues=2;
    private final int   DO_saveValues=3;

    private JPanel      drawPanel;
    private JPanel      rootPanel;

    /**
     * The manager needs to know the installed extensions
     */
    public PrefManager(List i_extensionsList) 
    {
        extensionsList = i_extensionsList;

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
         * It needs to use all the available space, otherwise the scroll pane does not
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
        if (doAction == DO_panelUpdate) 
            drawPanel.removeAll();
      
        for (Iterator iter=extensionsList.iterator(); iter.hasNext(); ) {
            doWorkItem ((ExtensionWrapper)iter.next(),doAction);
        }
    }

    /**
     * Do some work on one extension wrapper.
     * It is either adding panels, saving or loading...
     */
    private void doWorkItem( ExtensionWrapper aWrapper, int doAction ) 
    {
        // This extension is not valid, let me skip it
        if (! aWrapper.isValid()) 
            return;
        String extensionName = aWrapper.getExtensionClassName();

        switch (doAction) {
            case DO_loadValues:  
                aWrapper.safePrefGenLoadValues();   
                return;
            case DO_saveValues:  
                aWrapper.safePrefGenSaveValues();   
                return;
            case DO_panelUpdate: 
                addUserPanel (aWrapper, extensionName); 
                return;
        }
    }

    /**
     * Utility to make the code nicer. Michael likes it :-)
     */
    private String stripName ( String i_name )
    {
        int dotIndex = i_name.lastIndexOf(".");
        // No dots around, strange but possible...
        if (dotIndex < 0) 
            return i_name;

        // This is also strange... the dot is at the end of the string...
        if (dotIndex+1 >= i_name.length()) 
            return i_name;

        return i_name.substring(dotIndex+1);
    }
  
    /**
     * Being here to make code cleaner. 
     * Given an Extension preference panel add it into the main panel
     */
    private void addUserPanel( ExtensionWrapper aWrapper, String extensionName ) 
    {
        JPanel aPanel = aWrapper.safePrefGenGetPanel();
        if (aPanel == null) 
            return;

        // The panel that the user gives me goes into a container pane
        JPanel framePanel = new JPanel(new BorderLayout());
        framePanel.setBorder(BorderFactory.createTitledBorder(stripName(extensionName)));

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
