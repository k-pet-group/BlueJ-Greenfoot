package bluej.prefmgr;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.*;

import bluej.Config;
import bluej.utility.Debug;

/**
 * A JDialog subclass to allow the user to interactively edit
 * preferences.
 *
 * Note that this is a singleton class. There can be only one
 * instance of PrefMgrDialog at any time.
 *
 * @author Andrew Patterson
 * @version $Id: PrefMgrDialog.java 304 1999-12-09 23:48:13Z ajp $
 */
public class PrefMgrDialog extends JFrame
{
    private static final Image iconImage = new ImageIcon(Config.getImageFilename("image.icon")).getImage();

    private static PrefMgrDialog dialog = null;

    private static ArrayList listeners = new ArrayList();
    private static ArrayList tabs = new ArrayList();
    private static ArrayList titles = new ArrayList();

    private JTabbedPane tabbedPane = null;

	/**
	 * Setup the UI for the dialog and event handlers for the dialog's buttons.
	 *
	 * @param title the title of the dialog
	 */
    private PrefMgrDialog()
    {
        setIconImage(iconImage);
        setTitle(Config.getString("prefmgr.title"));

        tabbedPane = new JTabbedPane();

        for (ListIterator i = tabs.listIterator(); i.hasNext(); ) {
            int index = i.nextIndex();
            JPanel p = (JPanel)i.next();

            tabbedPane.addTab((String)titles.get(index), null, p);
        }

        JPanel contentPanel = new JPanel();
        {
            // arbitrary dimensions here.. what is the best way of getting these??
            contentPanel.setPreferredSize(new Dimension(512,450));

            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setBorder(Config.generalBorder);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton okButton = new JButton(Config.getString("okay"));
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

                JButton cancelButton = new JButton(Config.getString("cancel"));
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

                // try to make the OK and cancel buttons have equal width
                okButton.setPreferredSize(new Dimension(cancelButton.getPreferredSize().width,
                                okButton.getPreferredSize().height));
            } // buttonPanel

            contentPanel.add(tabbedPane);
            contentPanel.add(Box.createVerticalStrut(Config.generalSpacingWidth));
            contentPanel.add(buttonPanel);
        }

        getContentPane().add(contentPanel);
        pack();
    }

    /**
     * Register a panel to be shown in the preferences dialog
     *
     * @param panel     the panel to add
     * @param title     a string describing the panel
     * @param listener  an object which will be notified of events concerning the
     *                  preferences dialog
     */
    public static void add(JPanel panel, String title, PrefPanelListener listener)
    {
        tabs.add(panel);
        listeners.add(listener);
        titles.add(title);
    }

	/**
	 * Show the preferences dialog.  The first argument should
	 * be null if you want the dialog to come up in the center
	 * of the screen.  Otherwise, the argument should be the
	 * component on top of which the dialog should appear.
	 *
	 * @param comp the parent component for the dialog.
	 */
	public static boolean showDialog(Component comp) {
        if (dialog == null) {
            dialog = new PrefMgrDialog();
        }

        for (Iterator i = dialog.listeners.iterator(); i.hasNext(); ) {
            PrefPanelListener ppl = (PrefPanelListener)i.next();

            ppl.beginEditing();
        }

		dialog.setVisible(true);

		return true;
	}
}

/*
import bluej.editor.moe.MoeEditorPrefPanel;
        MoeEditorPrefPanel p2 = new MoeEditorPrefPanel();
        PrefMgrDialog.add(p2, "Editor", p2);
        */