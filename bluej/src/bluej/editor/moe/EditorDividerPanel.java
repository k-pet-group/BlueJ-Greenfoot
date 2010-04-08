/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bluej.Config;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

/**
 * Simple divider between the editor and the naviview to provide expand/collapse functionality
 * @author Marion Zalk  
 *
 */
public class EditorDividerPanel extends JPanel implements MouseListener {
    
    boolean expanded=true;
    protected JLabel expandCollapseButton;
    private final static String EXPAND_COLLAPSE_NAVIVIEW= "expandCollapseNaviview";
    private NaviView nav;
    private ImageIcon openNavArrow;
    private ImageIcon closeNavArrow;
    
    public EditorDividerPanel(NaviView naviview) {
        super();
        //display consists of a label with an image
        nav=naviview;
        openNavArrow=Config.getImageAsIcon("image.replace.open");
        closeNavArrow=Config.getImageAsIcon("image.replace.close");
        
        setPreferredSize(new Dimension(closeNavArrow.getIconWidth()+2, 0));
        setMaximumSize(new Dimension(closeNavArrow.getIconWidth()+2, Integer.MAX_VALUE));
        
        setLayout(new DBoxLayout(DBox.X_AXIS, 0, 0));
        expandCollapseButton=new JLabel();
        expandCollapseButton.setName(EXPAND_COLLAPSE_NAVIVIEW);
        expandCollapseButton.addMouseListener(this);
        expandCollapseButton.setIcon(closeNavArrow);
        add(expandCollapseButton, BorderLayout.CENTER);
    }

    protected boolean isExpanded() {
        return expanded;
    }

    protected void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void mouseClicked(MouseEvent e) {
        JComponent src = (JComponent) e.getSource();
        //if expanded/collapse set the necessary images and flags 
        if (src.getName()==EXPAND_COLLAPSE_NAVIVIEW){  
            if (isExpanded()){
                nav.setVisible(false);
                setExpanded(false);
                expandCollapseButton.setIcon(openNavArrow);
            }           
            else{
                nav.setVisible(true);
                setExpanded(true);
                expandCollapseButton.setIcon(closeNavArrow);
            }
        }       
    }

    public void mouseEntered(MouseEvent e) {
        
    }

    public void mouseExited(MouseEvent e) {
        
    }

    public void mousePressed(MouseEvent e) {
        
    }

    public void mouseReleased(MouseEvent e) {
        
    }
}
