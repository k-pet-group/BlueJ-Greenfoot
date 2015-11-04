/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.BlueJTheme;
import bluej.Boot;
import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * Dialog implementing version check functionality.
 *
 * @author  Michael Kolling
 * @version $Id: VersionCheckDialog.java 12517 2014-10-09 11:46:47Z nccb $
 */

final public class VersionCheckDialog extends EscapeDialog
     implements ActionListener
{
    // Internationalisation
    private static final String close = Config.getString("close");
    private static final String check = Config.getString("pkgmgr.versionDlg.check");
    private static final String dialogTitle = Config.getString("pkgmgr.versionDlg.title");
    private static final String helpLine1 = Config.getString("pkgmgr.versionDlg.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.versionDlg.helpLine2");

    private static final String versionURL = Config.getPropString("bluej.url.versionCheck");

    private JTextArea textArea;
    private JButton closeButton;

    private String newVersion = null;
    private Thread versionThread = null;
    private boolean isClosed = false;

    /**
     * Create a new version check dialogue and make it visible.
     */
    public VersionCheckDialog(PkgMgrFrame parent)
    {
        super(parent, dialogTitle, true);
        makeDialog();
        setVisible(true);
    }

    /**
     * A button was pressed. Check which one and do the right thing.
     */
    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();
        if(check.equals(cmd)) {
            doVersionCheck();
            getRootPane().setDefaultButton(closeButton);
        }
        else if(close.equals(cmd))
            doClose();
    }

    /**
     * Action when Close is pressed.
     */
    private void doClose()
    {
        isClosed = true;
        setVisible(false);
    }

    /**
     * Perform a version check. 
     */
    private void doVersionCheck()
    {
        textArea.setText(Config.getString("pkgmgr.checkingVersion"));
        versionThread = new VersionChecker();
        //versionThread.setPriority(Thread.MIN_PRIORITY);
        versionThread.start();
    }



    /**
     * Create the dialog interface.
     */
    private void makeDialog()
    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            JLabel helpText1 = new JLabel(helpLine1);
            mainPanel.add(helpText1);

            JLabel helpText2 = new JLabel(helpLine2);
            mainPanel.add(helpText2);

            Font smallFont = helpText1.getFont().deriveFont(10);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            textArea = new JTextArea(14, 46);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);

            mainPanel.add(scrollPane);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton checkButton = new JButton(check);
                checkButton.addActionListener(this);

                closeButton = new JButton(close);
                closeButton.addActionListener(this);

                DialogManager.addOKCancelButtons(buttonPanel, checkButton, closeButton);

                getRootPane().setDefaultButton(checkButton);
                checkButton.requestFocus();

                // try to make the OK and cancel buttons have equal width
                closeButton.setPreferredSize(
                                 new Dimension(checkButton.getPreferredSize().width,
                                 closeButton.getPreferredSize().height));
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    @OnThread(Tag.Any)
    private void setTextLater(String txt)
    {
        SwingUtilities.invokeLater(() -> textArea.setText(txt));
    }

    /**
     * Private class to run the actual version checking in separate thread.
     */
    private class VersionChecker extends Thread
    {
        /**
         * Do a version check. That is: open a URL connection to the remote 
         * version file and read it. Display version info as appropriate.
         */
        @OnThread(value = Tag.Unique, ignoreParent = true)
        public void run()
        {
            try {
                InputStream is = new URL(versionURL).openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                
                if(isOutOfDate(reader)) {
                    if(!isClosed)
                        displayNewVersionInfo(reader);
                }
                else {
                    if(!isClosed)
                        setTextLater(Config.getString("pkgmgr.versionDlg.upToDate"));
                }
            }
            catch(IOException exc) {
                if(!isClosed)
                    setTextLater("Error: could not access remote version information");
                Debug.reportError("IO error when trying to access URL\n" + exc);
            }
        }

        /**
         * Given a reader for the (remote) version file, check whether this
         * version is out of date. We know that the first line of the version
         * file contains the up-to-date version number.
         */
        @OnThread(Tag.Unique)
        private boolean isOutOfDate(BufferedReader versionReader)
        {
            try {
                newVersion = versionReader.readLine();
                if(newVersion != null)
                    newVersion = newVersion.trim();
            }
            catch(IOException exc) {
                setTextLater("Error: could not read remote version information");
                Debug.reportError("IO error when reading remote version info\n" + exc);
            }
            return ! Boot.BLUEJ_VERSION.equals(newVersion);
        }

        /**
         * Given a reader for the (remote) version file, read the version
         * info text out of it and display it in the text area.
         */
        @OnThread(Tag.Unique)
        private void displayNewVersionInfo(BufferedReader versionReader)
        {
            if(newVersion == null)
                setTextLater("Error: could not read remote version info");
            else {
                StringBuffer text = new StringBuffer(Config.getString("pkgmgr.versionDlg.currentVersion"));
                text.append(" ");
                text.append(Boot.BLUEJ_VERSION);
                text.append("\n");
                text.append(Config.getString("pkgmgr.versionDlg.newVersion"));
                text.append(" ");
                text.append(newVersion);
                text.append("\n");

                try {
                    String line = versionReader.readLine();
                    while(line != null) {
                        text.append(line);
                        text.append("\n");
                        line = versionReader.readLine();
                    }
                }
                catch(IOException exc) {
                    Debug.reportError("IO error when reading from version file");
                }
                SwingUtilities.invokeLater(() -> {
                    textArea.setText(text.toString());
                    textArea.setCaretPosition(0);
                });
            }
        }

    } // end class VersionChecker

}
