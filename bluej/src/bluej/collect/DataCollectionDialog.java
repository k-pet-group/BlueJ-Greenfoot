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

public class DataCollectionDialog extends JDialog
{
    public DataCollectionDialog()
    {
        setModal(true);
        
        setTitle("BlueJ - Data Collection");
        
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        {
            JPanel headerPanel = new JPanel();
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            headerPanel.add(new JLabel(new ImageIcon("/home/neil/work/bluej/lib/images/bluej-icon-48.png")));
            headerPanel.add(Box.createRigidArea(new Dimension(10,0)));
            
            JLabel label = new JLabel();
            label.setText("<html><body><b>The BlueJ team are running a data collection project to help researchers understand how students learn to program.<br></b></body></html>");
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            headerPanel.add(label, BorderLayout.CENTER);
            
            body.add(headerPanel);
            body.add(Box.createRigidArea(new Dimension(0,10)));
        }
        
        {
            JTextArea text = new JTextArea();
            text.setText("If you click Yes below, you will permit us to collect anonymous data about your use of BlueJ, including the features you use and the code you write. This will be used by the BlueJ group and other academic researchers to help understand how students learn to program and to improve BlueJ itself. No personal information will be transmitted to, or stored by, us and any Java class comments you write will be blanked out before transmission.\n\nIf you change your mind about helping us with this work, you can stop contributing at any time using the checkbox in the Tools/Preferences/Miscellaneous dialog.");
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(text);
        }
        
        {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setText("I agree to take part, and I certify that I am 16 or older.");
            checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
            
            JPanel checkPanel = new JPanel();
            checkPanel.setLayout(new BorderLayout());
            checkPanel.add(checkBox, BorderLayout.EAST);
            checkPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            body.add(checkPanel);
        }
        
        {
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BorderLayout());
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JButton buttonNo = new JButton();
            buttonNo.setText("No");
            buttonPanel.add(buttonNo, BorderLayout.WEST);
            
            JButton buttonYes = new JButton();
            buttonYes.setText("Yes");
            buttonYes.setEnabled(false);
            buttonPanel.add(buttonYes, BorderLayout.EAST);
    
            body.add(buttonPanel);
        }
        
        getContentPane().add(body);
        pack();
    }
    
    
}
