package bluej.prefmgr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;

import bluej.Config;

/**
 * A JDialog subclass to allow the user to interactively edit
 * preferences.
 *
 * Note that this is a singleton class. There can be only one
 * instance of PrefMgrDialog at any time.
 *
 * @author  Andrew Patterson
 * @version $Id: PrefMgrDialog.java 1819 2003-04-10 13:47:50Z fisker $
 */
public class PrefMgrDialog extends JFrame
{
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
        setIconImage(Config.frameImage);
        setTitle(Config.getString("prefmgr.title"));

        tabbedPane = new JTabbedPane();

        for (ListIterator i = tabs.listIterator(); i.hasNext(); ) {
            int index = i.nextIndex();
            JPanel p = (JPanel)i.next();

            tabbedPane.addTab((String)titles.get(index), null, p);
        }

        JPanel contentPanel = (JPanel)getContentPane();
        {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.setBorder(Config.dialogBorder);

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

        for (Iterator i = PrefMgrDialog.listeners.iterator(); i.hasNext(); ) {
            PrefPanelListener ppl = (PrefPanelListener)i.next();

            ppl.beginEditing();
        }

        dialog.setVisible(true);

        return true;
    }
}
