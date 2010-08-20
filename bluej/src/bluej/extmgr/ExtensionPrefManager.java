/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.extmgr;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import bluej.prefmgr.PrefPanelListener;

/**
 * This manages the whole preference pane for Extensions
 * It will be loaded in the appropriate tab when the register() is called
 * 
 * @author  Damiano Bolla: University of Kent at Canterbury
 * @author  Michael Kolling
 */
public class ExtensionPrefManager implements PrefPanelListener
{
    private List<ExtensionWrapper> extensionsList;

    private final int DO_panelUpdate=1;
    private final int DO_loadValues=2;
    private final int DO_saveValues=3;

    private JPanel drawPanel;
    private JPanel rootPanel;

    /**
     * The manager needs to know the installed extensions
     */
    public ExtensionPrefManager(List<ExtensionWrapper> i_extensionsList) 
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
    }

    /**
     * Return the panel that shows the GUI.
     */
    public JPanel getPanel()
    {
        return rootPanel;
    }
    
    /**
     * This is the looper, I will use some const to decide at the end
     * what to do. Just to make code simples and cleaner
     * Note that half or more of the code is on Fault managment ...
     */
    private void doWorkLoop(int doAction) 
    {
        // I need to remove all content, in any case...
        if (doAction == DO_panelUpdate) 
            drawPanel.removeAll();
      
        synchronized (extensionsList) {
            for (ExtensionWrapper wrapper : extensionsList) {
                doWorkItem (wrapper, doAction);
            }            
        }
    }

    /**
     * Do some work on one extension wrapper.
     * It is either adding panels, saving or loading...
     */
    private void doWorkItem(ExtensionWrapper aWrapper, int doAction) 
    {
        // This extension is not valid, let me skip it
        if (! aWrapper.isValid()) return;

        String extensionName = aWrapper.safeGetExtensionName();

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
     * Being here to make code cleaner. 
     * Given an Extension preference panel add it into the main panel
     */
    private void addUserPanel(ExtensionWrapper aWrapper, String extensionName) 
    {
        JPanel aPanel = aWrapper.safePrefGenGetPanel();
        if (aPanel == null) {
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
     * Start the revalidation of the panels associated to the extensions.
     */
    public void panelRevalidate() 
    {
        if (EventQueue.isDispatchThread()) {
            doWorkLoop(DO_panelUpdate);
        }
        else {
            EventQueue.invokeLater(new ExtensionPrefManager.DoPanelUpdate());
        }
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

    /*
     * Needed only to satisfy the implements
     */
    public void beginEditing()  {  }
    
    
    /*
     * Called by the system when it is time to reload the panel values
     */
    public void revertEditing() 
    {
        doWorkLoop(DO_loadValues);
    }

    /*
     * Called by the system when the user has pressed the OK buton
     */
    public void commitEditing()
    {
        doWorkLoop(DO_saveValues);
    }

}
