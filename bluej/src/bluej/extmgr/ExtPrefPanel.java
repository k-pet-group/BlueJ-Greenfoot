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

import bluej.extensions.BPrefPanel;
import bluej.extensions.BlueJ;

/**
 * This manages the whole preference pane for Extensions
 * It will be loaded in the appropriate tab when the register() is called
 */
public class ExtPrefPanel implements PrefPanelListener
{
    private final int   DO_panelUpdate=1;
    private final int   DO_loadValues=2;
    private final int   DO_saveValues=3;

    private JPanel      drawPanel;
    private Timer       updateTimer;
  
    public static final ExtPrefPanel INSTANCE = new ExtPrefPanel();
    public  JPanel      rootPanel;

    /**
     * I have no time to figure out how the main system comes here
     * When it comes it adds this same class using the given call...
     * NOTE: Since the static allocation we are not REALLY shure when this class is allocated
     * It may even be USED before it is linked there, any problems ?
     */
    public static void register() 
    {
        PrefMgrDialog.add(INSTANCE.rootPanel, Config.getString("extmgr.extensions"), INSTANCE);
    }


    /**
     * A private constructor, is it so important that this class is a singleton ?
     * What if in some future time we want to destroy it and start again ?
     */
    private ExtPrefPanel() 
    {
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

        /* This timer is used to syncronize with the Swing thread
         * There is a syncronization problem, quite a bing one actually
         * When the extension is instantiated NOT all structures of the manager
         * are in SYNC, basically it means that when the extension calls one of the
         * methods that it should be able to call the data are not always correct
         * Will try to fix this, now let's use the timer, with a LONG delay
         */
        updateTimer = new Timer(1000, new myActionListener());
        updateTimer.setInitialDelay(1000);
        updateTimer.setCoalesce(true);
        updateTimer.setRepeats(false);
    }


    /** 
     * The idea is that every time the component is shown I go around and delete-add all the panels that
     * are here. Since this is not something that is done quite often I have a reasonable
     * way to manage deletion/addition of preferences without becoming mad at it :-)
     * NOTE: This MUST be swing sync, this is the reason is called by the timer.
     */
    private class myActionListener implements ActionListener 
    {
        public void actionPerformed ( ActionEvent anEvent ) 
        {
          doWorkLoop (DO_panelUpdate);
        }
    }

    /**
     * This is the looper, I will use some const to decide at the end
     * what to do. Just to make code simples and cleaner
     * Note that half or more of the code is on Fault managment ...
     */
    private void doWorkLoop( int doAction ) 
    {
        ExtensionsManager extMgr = ExtensionsManager.getExtMgr();
        // This should really never happen, but who knows, some strange state...
        if ( extMgr == null ) return;

        // I need to remove all content, in any case...
        if ( doAction == DO_panelUpdate ) drawPanel.removeAll();
      
        List allExtensions = extMgr.getExtensions();
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
        if ( ! aWrapper.isValid() ) 
            return;
        String extensionName = aWrapper.getName();
    
        BlueJ aBlueJ = aWrapper.getBlueJ();
        // Can a wrapper not have bluej ? ... yes, it happens....
        if ( aBlueJ == null ) 
            return;

        BPrefPanel aPrefPanel = aBlueJ.getBPrefPanel();
        // An extension may not have a preference panel
        if ( aPrefPanel == null ) 
            return;

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

// Will user debug...
//        System.out.println ("ExtPrefPanel: Unknown doAction="+doAction);
        }
  
    /**
     * Being here to make code cleaner. 
     * Given an Extension preference panel add it into the main panel
     */
    private void addUserPanel( BPrefPanel aPrefPanel, String extensionName ) 
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
        updateTimer.restart();  
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

    /**
    * To be deleted or moved into the BPrefPanel, we will see
    */
    String[] getPreferenceNames(ExtensionWrapper ew)
    {
        String [] values = {""};
        return (values);
    }
}
