/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
package bluej.collect;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;

/**
 * Package-visible dialog, displayed to ask the user whether they want to opt-in or opt-out
 * of the data collection project.
 *
 */
class DataCollectionDialog extends JDialog
{
    private boolean optedIn = false;

    public DataCollectionDialog()
    {
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        setTitle("BlueJ - Data Collection");
        
        JPanel body = new JPanel();
        body.setLayout(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel topHalf = new JPanel();
        topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.Y_AXIS));
        
        JPanel headerPanel = makeHeaderPanel();
            
        topHalf.add(headerPanel);
        topHalf.add(Box.createVerticalStrut(10));
        topHalf.add(makeButtonPanel());
        topHalf.add(Box.createVerticalStrut(5));
        topHalf.add(new JSeparator(SwingConstants.HORIZONTAL));
        body.add(topHalf, BorderLayout.NORTH);

        // To make the sizing work right, it's best that the text is the center panel in the layout:
        body.add(makeExplanationText(headerPanel.getPreferredSize().width), BorderLayout.CENTER);
    
        getContentPane().add(body);
        pack();
    }

    /**
     * Make a JEditorPane with the ethics text inside
     * @param width
     * @return
     */
    private JEditorPane makeExplanationText(int width) {
        JEditorPane text = new JEditorPane();
        text.setContentType("text/html");

        String content = "<p>If you agree to take part, you will permit us to collect anonymous data about your use of BlueJ, including the features you use and the code you write. This will be used by the BlueJ group and other academic researchers to help understand how students learn to program and to improve BlueJ itself. No personal information will be transmitted to, or stored by, us and any Java class comments you write will be removed before transmission.  <a href='http://www.bluej.org/blackbox.html'>See more information</a></p><p>If you change your mind about helping us with this work, you can stop contributing at any time using the checkbox in the Tools/Preferences/Miscellaneous dialog.</p>";
        text.setText(content);
        text.setEditable(false);
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Make it same background colour as the window:
        text.setBackground(getBackground());
        // Make it same font as the top label:
        Font labelFont = UIManager.getFont("Label.font");
        // but slightly smaller, to de-emphasise the large chunk of text.
        // Must use style-sheet to set the font, not setFont
        String styleRule = "a { color: black; } body { font-family: " + labelFont.getFamily() + "; " +
                "font-size: " + (labelFont.getSize() - 2) + "pt; } ";
        ((HTMLDocument)text.getDocument()).getStyleSheet().addRule(styleRule);
        
        setEditorPaneSize(width, text);
        
        addLinkListener(text);
        return text;
    }

    /**
     * Adds a HyperlinkListener to the pane that opens the given link in
     * an external browser
     */
    private void addLinkListener(JEditorPane text) {
        text.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hev)
            {
                if (hev.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                {
                    Utility.openWebBrowser(hev.getURL());
                }
            }
        });
    }

    /**
     * Sets the editor pane's size by using a given fixed width, and
     * letting the height vary.
     */
    private void setEditorPaneSize(int width, JEditorPane text)
    {
        //I know this code seems crazy, but I spent a long time trying other methods,
        //and this was the only one that worked at the time.        
        
        // To get the control to fit its content, we first have to set it to a size
        // with the right width (we use headerPanel as it's likely the widest component
        // in the dialog:            
        text.setSize(new Dimension(width, 300));
        
        // Only after setting its size can we use modelToView to pick
        // out the coordinates of the last character:
        Rectangle r = null;
        try
        {
            r = text.modelToView(text.getDocument().getLength());
        }
        catch (BadLocationException e)
        {
            // Shouldn't happen:
            Debug.reportError(e);
        }            

        // Now set the size so it will fit the last character
        // (plus spacing/fudge-factor of 5):
        Dimension d = new Dimension(text.getMinimumSize().width, r.y + r.height + 5);
        text.setPreferredSize(d);
        text.setSize(d);
    }

    /**
     * Makes the header panel, which contains an icon on the left, and header text on the right
     */
    private JPanel makeHeaderPanel()
    {
        JPanel headerPanel = new JPanel();
    
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        headerPanel.add(new JLabel(Config.getFixedImageAsIcon("bluej-icon-48.png")));
        headerPanel.add(Box.createHorizontalStrut(20));
        
        JLabel headerLabel = new JLabel();
        headerLabel.setText("<html><body><b>The BlueJ team are running a data collection project to help researchers<br>understand how students learn to program.<br><br>Please help us by participating.</b></body></html>");
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        headerPanel.add(headerLabel);
        
        return headerPanel;
    }

    /**
     * Makes the button panel, which has Opt-In/Opt-Out buttons
     */
    private JPanel makeButtonPanel()
    {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton buttonNo = new JButton();
        buttonNo.setText("No thanks");
        buttonNo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                optedIn = false;
                DataCollectionDialog.this.setVisible(false);                    
            }
        });
        buttonPanel.add(buttonNo, BorderLayout.WEST);
        
        JButton buttonYes = new JButton();
        buttonYes.setText("I agree to take part, and I certify that I am 16 or older.");
        buttonYes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                optedIn = true;
                DataCollectionDialog.this.setVisible(false);                    
            }
        });
        buttonPanel.add(buttonYes, BorderLayout.EAST);
        return buttonPanel;
    }
    
    /**
     * After the dialog has closed, tells you whether they opted-in
     */
    public boolean optedIn()
    {
        return optedIn;
    }
    
}
