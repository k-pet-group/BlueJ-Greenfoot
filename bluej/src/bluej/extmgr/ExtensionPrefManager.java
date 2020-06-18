/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgrDialog;
import bluej.prefmgr.PrefPanelListener;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.List;

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

    private VBox drawVBox;
    private ScrollPane drawScrollPane;

    /**
     * The manager needs to know the installed extensions
     */
    public ExtensionPrefManager(List<ExtensionWrapper> i_extensionsList) 
    {
        extensionsList = i_extensionsList;
        // Vertically aligned Box node to contain extensions UI
        drawVBox= new VBox();
        JavaFXUtil.addStyleClass(drawVBox, "prefmgr-ext-innerpanel");
        drawScrollPane = new ScrollPane(drawVBox);
        JavaFXUtil.addStyleClass(drawScrollPane,"prefmgr-pref-panel");
    }

    /**
     * Return the VBox that shows the GUI.
     * If there is nothing to show, we add a label to indicate so.
     */
    public Node getExtensionContent()
    {
        if (drawVBox.getChildren().size() == 0)
        {
            drawVBox.getChildren().add(new Text(Config.getString("prefmgr.extensions.noContent")));
        }
        return drawScrollPane;
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
            drawVBox.getChildren().clear();
      
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
                addUserPane(aWrapper, extensionName);
                return;
        }
    }

  
    /**
     * Being here to make code cleaner. 
     * Given an Extension preference Pane add it into the main panel
     */
    private void addUserPane(ExtensionWrapper aWrapper, String extensionName)
    {
        Pane userPane = aWrapper.safePrefGenGetWindow();
        if (userPane == null) {
            return;
        }

        // The panel that the user gives me goes into a container pane
        // The rendering of the container is consistent with other parts
        // of the Preferences dialog.
        drawVBox.getChildren().add(PrefMgrDialog.headedVBox(extensionName, Arrays.asList(userPane)));
    }

    /**
     * Start the revalidation of the panels associated to the extensions.
     */
    @OnThread(Tag.FXPlatform)
    public void panelRevalidate()
    {
        doWorkLoop(DO_panelUpdate);
    }

     /**
     * Needed only to satisfy the implements
     */
    @OnThread(Tag.FXPlatform)
    public void beginEditing(Project project)  {  }
    
    
    /*
     * Called by the system when it is time to reload the panel values
     */
    @OnThread(Tag.FXPlatform)
    public void revertEditing(Project project)
    {
         doWorkLoop(DO_loadValues);
    }

    /*
     * Called by the system when the user has pressed the OK buton
     */
    @OnThread(Tag.FXPlatform)
    public void commitEditing(Project project)
    {
        doWorkLoop(DO_saveValues);
    }

}
