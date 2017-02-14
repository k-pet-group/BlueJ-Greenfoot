/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2017  Michael Kolling and John Rosenberg 
 
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
package bluej;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;


/**
 * Super class for splash images.
 *
 * @author Poul Henriksen
 * @version $Id$
 */
public abstract class SplashLabel extends JComponent
{
    // We use ImageIcon because it loads the HiDPI version of 
    // the image when on a Retina machine.
    private ImageIcon image;

    public SplashLabel(String imageName)
    {
        loadImage(imageName);
        //setBorder(BorderFactory.createLineBorder(Color.black, 1));
    }
    
    public Image getImage() {
        return image.getImage();
    }
   
    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension prefSize = new Dimension();
        if(image != null) {
            prefSize.setSize(image.getIconWidth(), image.getIconHeight());
        }
        return prefSize;
    }
    
    private void loadImage(String imageName) {
        URL splashURL = getClass().getResource(imageName); 
      
        if (splashURL == null) {
            System.out.println("cannot find splash image: " + imageName);
            return;
        }

        image = new ImageIcon(splashURL);
    }

}
