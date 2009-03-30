/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.prefmgr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;

import bluej.*;
import bluej.Config;

import bluej.classmgr.ClassMgrPrefPanel;
import bluej.editor.moe.EditorPrefPanel;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.ExtensionPrefManager;

/**
 * A JDialog subclass to allow the user to interactively edit
 * preferences.
 *
 * A singleton.
 *
 * @author  Andrew Patterson
 * @author  Michael Kolling
 * @version $Id: PrefMgrDialog.java 6215 2009-03-30 13:28:25Z polle $
 */
public class PrefMgrDialog extends JFrame
{
    private static PrefMgrDialog dialog = null;
    
    /** Indicates whether the dialog has been prepared for display. */
    private boolean prepared = false;

    /**
     * Show the preferences dialog.  The first argument should
     * be null if you want the dialog to come up in the center
     * of the screen.  Otherwise, the argument should be the
     * component on top of which the dialog should appear.
     *
     * @param comp the parent component for the dialog.
     */
    public static void showDialog() {
        getInstance().prepareDialog();
        dialog.setVisible(true);
    }
    
    /**
     * Show the preferences dialog.  The first argument should
     * be null if you want the dialog to come up in the center
     * of the screen.  Otherwise, the argument should be the
     * component on top of which the dialog should appear.
     *
     * @param comp the parent component for the dialog.
     * @param comp the parent component for the dialog.
     */
    public static void showDialog(int paneNumber) {
        dialog.prepareDialog();
        dialog.selectTab(paneNumber);
        dialog.setVisible(true);
    }

    /**
     * Prepare this dialog for display.
     */
    private synchronized void prepareDialog() {
        if (!prepared) {
            makeDialog();
            prepared = true;
        }
        dialog.startEditing();
    }
    
    /**
     * Returns the current instance of the dialog, can be null.
     * @return the current instance of the dialog, can be null.
     */
    public static final PrefMgrDialog getInstance ()
    {
        if (dialog == null) {
            dialog = new PrefMgrDialog();
        }

        return dialog;
    }
    
    
    private ArrayList listeners = new ArrayList();
    private ArrayList tabs = new ArrayList();
    private ArrayList titles = new ArrayList();

    private JTabbedPane tabbedPane = null;

    private ClassMgrPrefPanel userConfigLibPanel;
    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     *
     * @param title the title of the dialog
     */
    private PrefMgrDialog()
    {
        createPrefPanes();
    }

    /**
     * Create all known preference panes.
     */
    private void createPrefPanes()
    {
        EditorPrefPanel panel = new EditorPrefPanel();
        add(panel, Config.getString("prefmgr.edit.prefpaneltitle"), panel);    
        MiscPrefPanel panel2 = new MiscPrefPanel();
        add(panel2, Config.getString("prefmgr.misc.prefpaneltitle"), panel2);
        userConfigLibPanel = new ClassMgrPrefPanel();
        add(userConfigLibPanel, Config.getString("classmgr.prefpaneltitle"), userConfigLibPanel);
        if(!Config.isGreenfoot()) {
            ExtensionPrefManager mgr = ExtensionsManager.getInstance().getPrefManager();
            add(mgr.getPanel(), Config.getString("extmgr.extensions"), mgr);
        }
    }
    
    /**
     * Returns the istance of the UserConfigLibPanel.
     * It is possible to retrieve the current list of libraries from it.
     * @return a user config lib panel.
     */
    public ClassMgrPrefPanel getUserConfigLibPanel ()
    {
        return userConfigLibPanel;
    }
    
    /**
     * Register a panel to be shown in the preferences dialog
     *
     * @param panel     the panel to add
     * @param title     a string describing the panel
     * @param listener  an object which will be notified of events concerning the
     *                  preferences dialog
     */
    public void add(JPanel panel, String title, PrefPanelListener listener)
    {
        tabs.add(panel);
        listeners.add(listener);
        titles.add(title);
    }

    private void startEditing()
    {
        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            PrefPanelListener ppl = (PrefPanelListener)i.next();
            ppl.beginEditing();
        }        
    }
    
    private void selectTab(int tabNumber)
    {
        tabbedPane.setSelectedIndex(tabNumber);
    }
    
    private void makeDialog()
    {
        setIconImage(BlueJTheme.getIconImage());
        setTitle(Config.getApplicationName() + ": " + Config.getString("prefmgr.title"));

        tabbedPane = new JTabbedPane();

        for (ListIterator i = tabs.listIterator(); i.hasNext(); ) {
            int index = i.nextIndex();
            JPanel p = (JPanel)i.next();
            tabbedPane.addTab((String)titles.get(index), null, p);
        }

        JPanel contentPanel = (JPanel)getContentPane();
        {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.setBorder(BlueJTheme.dialogBorder);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton okButton = BlueJTheme.getOkButton();
                {
                    okButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                for (Iterator i = listeners.iterator(); i.hasNext(); ) {
                                    PrefPanelListener ppl = (PrefPanelListener)i.next();
                                    ppl.commitEditing();
                                }
                                setVisible(false);
                            }
                        });
                }

                getRootPane().setDefaultButton(okButton);

                JButton cancelButton = BlueJTheme.getCancelButton();
                {
                    cancelButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                for (Iterator i = listeners.iterator(); i.hasNext(); ) {
                                    PrefPanelListener ppl = (PrefPanelListener)i.next();
                                    ppl.revertEditing();
                                }
                                setVisible(false);
                            }
                        });
                }

                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);
            }

            contentPanel.add(tabbedPane, BorderLayout.CENTER);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        }

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.preferences", getLocation());
                }
            });

        setLocation(Config.getLocation("bluej.preferences"));
        pack();        
    }
}
