/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JComponent;


/**
 * Super class for splash images.
 *
 * @author Poul Henriksen
 * @version $Id$
 */
public abstract class SplashLabel extends JComponent
{
    private BufferedImage image;
    
    public SplashLabel(String imageName)
    {
        loadImage(imageName);
        //setBorder(BorderFactory.createLineBorder(Color.black, 1));
    }
    
    public BufferedImage getImage() {
        return image;
    }
   
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        Dimension prefSize = new Dimension();
        if(image != null) {
            prefSize.setSize(image.getWidth(), image.getHeight());
        }
        return prefSize;
    }
    
    private void loadImage(String imageName) {
        URL splashURL = getClass().getResource(imageName); 
      
        if (splashURL == null) {
            System.out.println("cannot find splash image: " + imageName);
            return;
        }
        try {
            image = ImageIO.read(splashURL);
        }
        catch (IOException exc) { // ignore
        }
    }

}
