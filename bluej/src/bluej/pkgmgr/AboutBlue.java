/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013  Michael Kolling and John Rosenberg
 
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

import bluej.*;
import bluej.Config;
import bluej.utility.EscapeDialog;
import bluej.utility.MultiLineLabel;
import bluej.utility.DialogManager;

import bluej.utility.Utility;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The BlueJ about box.
 *
 * @author  Michael Kolling
 */
class AboutBlue extends EscapeDialog
{
    private static final String BLUEJ_URL = "http://www.bluej.org";
    private static final Color linkColor = new Color(0, 76, 134);

    public AboutBlue(JFrame parent, String version)
    {
        super(parent, Config.getString("menu.help.about"), true);

        // Create About box text
        JPanel aboutPanel = new JPanel();
        aboutPanel.setBorder(BlueJTheme.dialogBorder);
        aboutPanel.setLayout(new BorderLayout(12,0));
        aboutPanel.setBackground(Color.white);

        // insert logo
        Icon icon = Config.getFixedImageAsIcon("about-logo.jpg");
        JLabel logoLabel = new JLabel(icon);
        aboutPanel.add(logoLabel, BorderLayout.WEST);

        // Create Text Panel
        MultiLineLabel text = new MultiLineLabel(LEFT_ALIGNMENT, 6);
        text.setBackground(Color.white);
        text.addText(Config.getString("about.theTeam") + "\n ", false, true);
        text.addText("  Amjad Altadmri\n");
        text.addText("  Neil Brown\n");
        text.addText("  Fabio Hedayioglu\n");
        text.addText("  Michael K\u00F6lling\n");
        text.addText("  Davin McCall\n");
        text.addText("  Ian Utting\n");

        aboutPanel.add(text, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.PAGE_AXIS));
        bottom.setBackground(Color.white);

        // footer text
        MultiLineLabel bottomtext = new MultiLineLabel(LEFT_ALIGNMENT);
        bottomtext.setBackground(Color.white);
        bottomtext.addText(" ");
        bottomtext.addText(Config.getString("about.bluej.version") + " "+ version +
                "  (" + Config.getString("about.java.version") + " " + System.getProperty("java.version") +
                ")", true, false);
        bottomtext.addText(" ");
        bottomtext.addText(Config.getString("about.vm") + " " +
                System.getProperty("java.vm.name") + " " +
                System.getProperty("java.vm.version") +
                " (" + System.getProperty("java.vm.vendor") + ")");
        bottomtext.addText(Config.getString("about.runningOn") + " " + System.getProperty("os.name") +
                " " + System.getProperty("os.version") +
                " (" + System.getProperty("os.arch") + ")");
        bottomtext.addText(Config.getString("about.javahome") + " " + System.getProperty("java.home"));
        bottomtext.addText(" ");
        bottomtext.addText(Config.getString("about.logfile") + " " + Config.getUserConfigFile(Config.debugLogName));
        bottomtext.addText(" ");
        
        bottom.add(bottomtext);

        try {
            final URL bluejURL = new URL(BLUEJ_URL);
            JLabel urlField = new JLabel(BLUEJ_URL);
            urlField.setCursor(new Cursor(Cursor.HAND_CURSOR));
            urlField.setForeground(linkColor);
            urlField.addMouseListener(new MouseAdapter()  {
                public void mouseClicked(MouseEvent e) {
                    Utility.openWebBrowser(bluejURL.toExternalForm());
                }
            });

            JPanel urlPanel = new JPanel();
            urlPanel.setBackground(Color.white);
            urlPanel.setAlignmentX(0.0F);
            urlPanel.add(new JLabel(Config.getString("about.moreInformation")));
            urlPanel.add(urlField);

            bottom.add(urlPanel);
        }
        catch (MalformedURLException exc) {
            // should not happen - URL is constant
        }

        aboutPanel.add(bottom, BorderLayout.SOUTH);


        // Create Button Panel
        JPanel buttonPanel = new JPanel();
        //buttonPanel.setBackground(Color.white);
        buttonPanel.setLayout(new FlowLayout());
        JButton ok = BlueJTheme.getOkButton();
        buttonPanel.add(ok);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(aboutPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Close Action when OK is pressed
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
                {
                    setVisible(false);
                    dispose();
                }
        });

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event)
                {
                    Window win = (Window)event.getSource();
                    win.setVisible(false);
                    win.dispose();
                }
        });

        setResizable(false);
        pack();
        DialogManager.centreDialog(this);
    }
}

