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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import bluej.Config;
import bluej.utility.Debug;

class DataCollectionDialog extends JDialog
{
    private JButton buttonYes;
    private boolean optedIn = false;

    public DataCollectionDialog()
    {
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        setTitle("BlueJ - Data Collection");
        
        JPanel body = new JPanel();
        body.setLayout(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel topHalf = new JPanel();
        topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.Y_AXIS));
        
        JPanel headerPanel = makeHeaderPanel();
            
            //headerPanel.add(Box.createRigidArea(new Dimension(0,10)));
        topHalf.add(headerPanel);
        topHalf.add(Box.createVerticalStrut(10));
        topHalf.add(makeButtonPanel());
        topHalf.add(Box.createVerticalStrut(5));
        topHalf.add(new JSeparator(SwingConstants.HORIZONTAL));
        topHalf.add(Box.createVerticalStrut(5));
        body.add(topHalf, BorderLayout.NORTH);
        
        
        {
            JTextArea text = new JTextArea();

            String content = "If you agree to take part, you will permit us to collect anonymous data about your use of BlueJ, including the features you use and the code you write. This will be used by the BlueJ group and other academic researchers to help understand how students learn to program and to improve BlueJ itself. No personal information will be transmitted to, or stored by, us and any Java class comments you write will be removed before transmission.\n\nIf you change your mind about helping us with this work, you can stop contributing at any time using the checkbox in the Tools/Preferences/Miscellaneous dialog.";
            text.setText(content);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setBackground(getBackground());
            Font labelFont = UIManager.getFont("Label.font");
            Debug.message("UIM Font: " + labelFont.getFontName() + "," + labelFont.getSize2D());
            Debug.message("JLabel Font: " + new JLabel().getFont().getFontName() + "," + new JLabel().getFont().getSize2D());
            Debug.message("JTextArea Font: " + new JTextArea().getFont().getFontName() + "," + new JTextArea().getFont().getSize2D());
            text.setFont(labelFont.deriveFont(labelFont.getSize2D() - 2.0f));
            text.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // To get the control to fit its content, we first have to set it to a size
            // with the right width (we use headerPanel as it's likely the widest component
            // in the dialog:            
            text.setSize(new Dimension(headerPanel.getPreferredSize().width, 300));
            
            // Only after setting its size can we use modelToView to pick
            // out the coordinates of the last character:
            Rectangle r = null;
            try {
                r = text.modelToView(text.getDocument().getLength());
            }
            catch (BadLocationException e) {
                // Shouldn't happen:
                Debug.reportError(e);
            }            

            // Now set the size so it will fit the last character
            // (plus spacing/fudge-factor of 5):
            Dimension d = new Dimension(text.getMinimumSize().width, r.y + r.height + 5);
            text.setPreferredSize(d);
            text.setSize(d);
            
            //I know the above seems crazy, but I spent a long time trying other methods,
            //and this was the only one that worked.
            
            

            body.add(text, BorderLayout.CENTER);
        }
    
        getContentPane().add(body);
        pack();
    }

    private JPanel makeHeaderPanel() {
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

    private JPanel makeButtonPanel() {
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
        
        buttonYes = new JButton();
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
    
    public boolean optedIn()
    {
        return optedIn;
    }
    
}
