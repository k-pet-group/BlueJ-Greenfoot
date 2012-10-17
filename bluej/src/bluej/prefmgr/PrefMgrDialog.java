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
package bluej.prefmgr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.classmgr.ClassMgrPrefPanel;
import bluej.editor.moe.EditorPrefPanel;
import bluej.editor.moe.KeyBindingsPanel;
import bluej.extmgr.ExtensionPrefManager;
import bluej.extmgr.ExtensionsManager;
import bluej.utility.DialogManager;

/**
 * A JDialog subclass to allow the user to interactively edit
 * preferences.
 *
 * <p>A singleton.
 *
 * @author  Andrew Patterson
 * @author  Michael Kolling
 */
public class PrefMgrDialog extends JFrame
{
    private static PrefMgrDialog dialog = null;
    
    /** Indicates whether the dialog has been prepared for display. */
    private boolean prepared = false;
    
    private KeyBindingsPanel kbPanel;

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
     * @param actiontable 
     * @param categories 
     * @param categoryIndex 
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


    private ArrayList<PrefPanelListener> listeners = new ArrayList<PrefPanelListener>();
    private ArrayList<JPanel> tabs = new ArrayList<JPanel>();
    private ArrayList<String> titles = new ArrayList<String>();

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
        kbPanel=new KeyBindingsPanel();
        add(kbPanel.makePanel(), Config.getString("prefmgr.edit.keybindingstitle"), kbPanel);
        MiscPrefPanel panel2 = new MiscPrefPanel();
        add(panel2, Config.getString("prefmgr.misc.prefpaneltitle"), panel2);
        InterfacePanel panel3 = new InterfacePanel();
        add(panel3, Config.getString("prefmgr.interface.title"), panel3);
        userConfigLibPanel = new ClassMgrPrefPanel();
        add(userConfigLibPanel, Config.getString("classmgr.prefpaneltitle"), userConfigLibPanel);
        if(!Config.isGreenfoot()) {
            ExtensionPrefManager mgr = ExtensionsManager.getInstance().getPrefManager();
            add(mgr.getPanel(), Config.getString("extmgr.extensions"), mgr);
        }

    }

    /**
     * Returns the instance of the UserConfigLibPanel.
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
        for (Iterator<PrefPanelListener> i = listeners.iterator(); i.hasNext(); ) {
            PrefPanelListener ppl = i.next();
            ppl.beginEditing();
        }        
    }

    private void selectTab(int tabNumber)
    {
        tabbedPane.setSelectedIndex(tabNumber);
    }

    private void makeDialog()
    {
        Image icon = BlueJTheme.getIconImage();
        if (icon != null) {
            setIconImage(icon);
        }
        setTitle(Config.getApplicationName() + ": " + Config.getString("prefmgr.title"));

        tabbedPane = new JTabbedPane();

        for (ListIterator<JPanel> i = tabs.listIterator(); i.hasNext(); ) {
            int index = i.nextIndex();
            JPanel p = i.next();
            tabbedPane.addTab(titles.get(index), null, p);
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
                            for (Iterator<PrefPanelListener> i = listeners.iterator(); i.hasNext(); ) {
                                PrefPanelListener ppl = i.next();
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
                            for (Iterator<PrefPanelListener> i = listeners.iterator(); i.hasNext(); ) {
                                PrefPanelListener ppl = i.next();
                                ppl.revertEditing();
                            }
                            setVisible(false);
                        }
                    });
                }

                DialogManager.addOKCancelButtons(buttonPanel, okButton, cancelButton);
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
