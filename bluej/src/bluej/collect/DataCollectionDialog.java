/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013  Michael Kolling and John Rosenberg 
 
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
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
        
        setTitle("BlueJ - " + Config.getString("collect.dialog.title"));
        
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

        String content = "<html><body><p>" + Config.getString("collect.dialog.ethics1") + 
                "  <a href='http://www.bluej.org/blackbox.html'>" + Config.getString("collect.dialog.ethics.seemore") + "</a>.</p>" +
                "<p>" + Config.getString("collect.dialog.ethics2") + "</p></body></html>";
        text.setText(content);
        text.setEditable(false);
        text.setAlignmentX(Component.LEFT_ALIGNMENT);

        // We want to make it same background colour as the window.  However,
        // setBackground doesn't work reliably with Nimbus (at least on Linux),
        // so making the background transparent is a more reliable way to have
        // the background the same colour as the underlying window
        // (although there is an old bug that may cause problems on early JDK 6:
        //  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6687960 )
        text.setOpaque(false);
        text.setBackground(new Color(0,0,0,0));
        
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
        headerLabel.setText("<html><body><b><p>"
          + Config.getString("collect.dialog.header1") + "<br>"
          + Config.getString("collect.dialog.header2") + "</p><br>"
          + "<p>" + Config.getString("collect.dialog.header3") + "</p></b></body></html>");
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
        buttonNo.setText(Config.getString("collect.dialog.no"));
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
        buttonYes.setText(Config.getString("collect.dialog.yes"));
        buttonYes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                optedIn = true;
                DataCollectionDialog.this.setVisible(false);                    
            }
        });
        buttonPanel.add(buttonYes, BorderLayout.EAST);
        getRootPane().setDefaultButton(buttonYes);
        buttonYes.requestFocusInWindow();
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
