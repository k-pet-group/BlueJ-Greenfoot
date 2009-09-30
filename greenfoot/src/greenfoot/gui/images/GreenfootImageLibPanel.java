/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling

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
package greenfoot.gui.images;

import greenfoot.util.GreenfootUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import bluej.BlueJTheme;
import bluej.Config;

/**
 * 
 * Component that shows selectors for images in the Greenfoot library of images.
 * 
 * @author Poul Henriksen
 */
public class GreenfootImageLibPanel extends Box
{
    public GreenfootImageLibPanel(ImageCategorySelector categorySelector, ImageLibList imageList)
    {
        super(BoxLayout.X_AXIS);
        
        // Category panel
        {
            Box piPanel = new Box(BoxLayout.Y_AXIS);        
    
            JLabel piLabel = new JLabel(Config.getString("imagelib.categories"));
            piLabel.setAlignmentX(0.0f);
            piPanel.add(piLabel);
    
    
            JScrollPane jsp = new JScrollPane(categorySelector);
    
            jsp.setBorder(Config.normalBorder);
            jsp.setViewportBorder(BorderFactory.createLineBorder(categorySelector.getBackground(), 4));
            jsp.setAlignmentX(0.0f);
    
            piPanel.add(jsp);
            add(piPanel);             
    
            add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, BlueJTheme.componentSpacingSmall));
        }
         
        // Image panel
        {
            Box piPanel = new Box(BoxLayout.Y_AXIS);
    
            JLabel piLabel = new JLabel(Config.getString("imagelib.images"));
            piLabel.setAlignmentX(0.0f);
            piPanel.add(piLabel);
    
            JScrollPane jsp = new JScrollPane();
    
            jsp.getViewport().setView(imageList);
    
            jsp.setBorder(Config.normalBorder);
            jsp.setViewportBorder(BorderFactory.createLineBorder(imageList.getBackground(), 4));
            jsp.setAlignmentX(0.0f);
    
            piPanel.add(jsp);
            add(piPanel);
        }
    }
}
