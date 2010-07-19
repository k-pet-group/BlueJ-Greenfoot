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
import javax.swing.JLabel;
import javax.swing.JPanel;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

/**
 * Simple divider between the editor and the naviview to provide expand/collapse functionality
 * 
 * @author Marion Zalk  
 */
public class EditorDividerPanel extends JPanel implements MouseListener {

    boolean expanded=true;
    protected JLabel expandCollapseButton;
    private final static String EXPAND_COLLAPSE_NAVIVIEW= "expandCollapseNaviview";
    private NaviView nav;
    private ImageIcon openNavArrow;
    private ImageIcon closeNavArrow;
    private boolean currentlyHidden = false;

    public EditorDividerPanel(NaviView naviview, boolean expanded) 
    {
        super();
        //display consists of a label with an image
        nav=naviview;
        this.expanded=expanded;
        openNavArrow=Config.getImageAsIcon("image.editordivider.open");
        closeNavArrow=Config.getImageAsIcon("image.editordivider.close");

        setPreferredSize(new Dimension(closeNavArrow.getIconWidth()+2, 0));
        setMaximumSize(new Dimension(closeNavArrow.getIconWidth()+2, Integer.MAX_VALUE));

        setLayout(new DBoxLayout(DBox.X_AXIS, 0, 0));
        expandCollapseButton=new JLabel();
        expandCollapseButton.setName(EXPAND_COLLAPSE_NAVIVIEW);
        addMouseListener(this); 
        add(expandCollapseButton, BorderLayout.CENTER);
        if (isExpanded())
            expandCollapseButton.setIcon(closeNavArrow);
        else{
            nav.setVisible(false);
            expandCollapseButton.setIcon(openNavArrow);
        }
    }

    protected boolean isExpanded() 
    {
        return expanded;
    }

    /**
     * Sets the value of expanded/collapsed to the current view and to the prefmgr 
     * @param expanded
     */
    protected void setExpanded(boolean expanded) 
    {
        //saving the value of the naviview (expanded/collapsed) to the prefmgr
        //so it is there when the next editor may be opened
        PrefMgr.setNaviviewExpanded(expanded);
        this.expanded = expanded;
    }

    /**
     * Causes either the naviview to expand/collapse
     */
    public void mouseClicked(MouseEvent e) 
    {
        // If they're viewing the documentation,
        // don't allow the toggle to act:
        if (currentlyHidden)
            return;
        //if expanded/collapse set the necessary images and flags 
        if (isExpanded()){
            nav.setVisible(false);
            setExpanded(false);
            expandCollapseButton.setIcon(openNavArrow);
            mouseExited(e);
        }           
        else{
            nav.setVisible(true);
            setExpanded(true);
            expandCollapseButton.setIcon(closeNavArrow);
            mouseExited(e);
        }     
    }

    public void mouseEntered(MouseEvent e) 
    {
        setOpaque(true);
        setBackground(MoeEditor.lightGrey);
        repaint();
    }

    public void mouseExited(MouseEvent e) {
        setOpaque(false);
        repaint();
    }

    public void mousePressed(MouseEvent e) { }

    public void mouseReleased(MouseEvent e) { }


    /**
     * Temporarily hides the naviview (when switching to documentation view)
     * Also temporarily hides this panel
     */
    public void beginTemporaryHide()
    {
        currentlyHidden = true;
        nav.setVisible(false);
        expandCollapseButton.setIcon(openNavArrow);
        setVisible(false);
    }
    
    /**
     * Stops the effects of a temporary hide (switch back to editor view)
     * and redisplays itself
     * 
     * Can be called without a previous call to beginTemporaryHide, e.g.
     * in the case where the editor is opened in documentation view
     */
    public void endTemporaryHide()
    {
        currentlyHidden = false;
        if (isExpanded()) {
            nav.setVisible(true);
            expandCollapseButton.setIcon(closeNavArrow);
        }
        setVisible(true);
    }
}
