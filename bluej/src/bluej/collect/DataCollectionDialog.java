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
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;

import bluej.utility.Debug;

public class DataCollectionDialog extends JDialog
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
        
        JPanel headerPanel = new JPanel();
        {
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            headerPanel.add(new JLabel(new ImageIcon("/home/neil/work/bluej/lib/images/bluej-icon-48.png")));
            headerPanel.add(Box.createRigidArea(new Dimension(10,0)));
            
            JLabel headerLabel = new JLabel();
            headerLabel.setText("<html><body><b>The BlueJ team are running a data collection project to help researchers<br>understand how students learn to program.<br></b></body></html>");
            headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            headerPanel.add(headerLabel);
            
            //headerPanel.add(Box.createRigidArea(new Dimension(0,10)));
            
            body.add(headerPanel, BorderLayout.NORTH);
        }
        
        {
            JTextArea text = new JTextArea();
            text.setText("If you click Yes below, you will permit us to collect anonymous data about your use of BlueJ, including the features you use and the code you write. This will be used by the BlueJ group and other academic researchers to help understand how students learn to program and to improve BlueJ itself. No personal information will be transmitted to, or stored by, us and any Java class comments you write will be blanked out before transmission.\n\nIf you change your mind about helping us with this work, you can stop contributing at any time using the checkbox in the Tools/Preferences/Miscellaneous dialog.");
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // To get the JTextArea to fit its content, we first have to set it to a size
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
            // (plus spacing/fudge-factor of 15):
            Dimension d = new Dimension(text.getMinimumSize().width, r.y + r.height + 15);
            text.setPreferredSize(d);
            text.setSize(d);
            
            //I know the above seems crazy, but I spent a long time trying other methods,
            //and this was the only one that worked.

            body.add(text, BorderLayout.CENTER);
        }
        
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        
        {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setText("I agree to take part, and I certify that I am 16 or older.");
            checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
            checkBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e)
                {
                    buttonYes.setEnabled(e.getStateChange() == ItemEvent.SELECTED);                    
                }
            });
            
            JPanel checkPanel = new JPanel();
            checkPanel.setLayout(new BorderLayout());
            checkPanel.add(checkBox, BorderLayout.EAST);
            checkPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            controls.add(checkPanel);
        }
        
        {
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BorderLayout());
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JButton buttonNo = new JButton();
            buttonNo.setText("No");
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
            buttonYes.setText("Yes");
            buttonYes.setEnabled(false);
            buttonYes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    optedIn = true;
                    DataCollectionDialog.this.setVisible(false);                    
                }
            });
            buttonPanel.add(buttonYes, BorderLayout.EAST);
    
            controls.add(buttonPanel);
        }
        
        body.add(controls, BorderLayout.SOUTH);
        
        getContentPane().add(body);
        pack();
    }
    
    public boolean optedIn()
    {
        return optedIn;
    }
    
}
