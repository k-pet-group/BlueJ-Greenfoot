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

import bluej.*;
import bluej.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;

/**
 * This class can display info on a particular extension. It is not really
 * bound to the HelpDialog and may be useful in the future.
 *  
 * @author Damiano Bolla, University of Kent at Canterbury, 2002,2003,2004
 */
class HelpDetailDialog extends EscapeDialog implements ActionListener
{
    private final String detailsTag = Config.getString("extmgr.details");
    private final String systemString = Config.getString("extmgr.systemExtensionLong");
    private final String projectString = Config.getString("extmgr.projectExtensionLong");
    private final String locationTag = Config.getString("extmgr.details.location");
    private final String versionTag = Config.getString("extmgr.details.version");
    private final String urlTag = Config.getString("extmgr.details.url");

    private JLabel nameField, locationField, typeField;
    private JLabel urlField;
    private JTextArea descriptionField;
    private URL url;
    private JButton closeButton;

    /**
     * Constructor
     */
    HelpDetailDialog(Dialog owner)
    {
        super(owner);
        setTitle(detailsTag);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        nameField = new JLabel();
        typeField = new JLabel();
        locationField = new JLabel();

        urlField = new JLabel();
        urlField.setCursor(new Cursor(Cursor.HAND_CURSOR));
        urlField.setForeground(Color.blue);
        urlField.addMouseListener(new MouseAdapter()  {
                public void mouseClicked(MouseEvent e) {
                    openURL();
                }
            });

        descriptionField = new JTextArea(4, 20);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        descriptionField.setEnabled(false);
        descriptionField.setDisabledTextColor(Color.black);
        descriptionField.setBackground(urlField.getBackground());

        JScrollPane descriptionScroller = new JScrollPane(descriptionField);
        descriptionScroller.setBorder(null);
        descriptionScroller.setAlignmentX(0.0F);

        mainPanel.add(nameField);
        mainPanel.add(typeField);
        mainPanel.add(Box.createVerticalStrut(12));
        
        mainPanel.add(locationField);
        mainPanel.add(Box.createVerticalStrut(12));
        
        mainPanel.add(descriptionScroller);
        mainPanel.add(Box.createVerticalStrut(12));

        JPanel urlPanel = new JPanel();
        urlPanel.setAlignmentX(0.0F);
        urlPanel.add(new JLabel(urlTag));
        urlPanel.add(urlField);
        mainPanel.add(urlPanel);
        
        // The close button goes into a panel, to make it nice...
        JPanel buttonPanel = new JPanel();
        closeButton = new JButton(Config.getString("close"));
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);

        // TIme to put the two main panels into the root pane
        JPanel rootPane = (JPanel) getContentPane();
        rootPane.setLayout(new BorderLayout());
        rootPane.setBorder(BlueJTheme.dialogBorder);

        rootPane.add(mainPanel, BorderLayout.CENTER);
        rootPane.add(buttonPanel, BorderLayout.SOUTH);

        // save position when window is moved
        addComponentListener(
            new ComponentAdapter()
            {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.extmgr.helpdialog.details", getLocation());
                }
            });

        setLocation(Config.getLocation("bluej.extmgr.helpdialog.details"));
        pack();
        // Do not call the setVisible
    }


    /**
     *  Called when the button is pressed
     *
     * @param  evt  Description of the Parameter
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object src = evt.getSource();
        if (src == null) return;

        if (src == closeButton) setVisible(false);
    }

    /**
     * When a different extension is shown you call this one.
     */
    void updateInfo(ExtensionWrapper wrapper)
    {
        if (wrapper == null) return;

        nameField.setText(wrapper.safeGetExtensionName() + " " + versionTag + " " 
                          + wrapper.safeGetExtensionVersion());
        typeField.setText((wrapper.getProject() != null) ? projectString : systemString);
        locationField.setText(locationTag + " " + wrapper.getExtensionFileName() +
                              " (" + wrapper.getExtensionStatus() +')');

        url = wrapper.safeGetURL();
        if (url == null)
            urlField.setText(null);
        else
            urlField.setText(url.toExternalForm());

        descriptionField.setText(wrapper.safeGetExtensionDescription());
        descriptionField.setCaretPosition(0);

        validate();
        pack();
        setVisible(true);
    }


    /**
     * Description of the Method
     */
    private void openURL()
    {
        if (url == null)
            return;
        Utility.openWebBrowser(url.toExternalForm());
    }
}

