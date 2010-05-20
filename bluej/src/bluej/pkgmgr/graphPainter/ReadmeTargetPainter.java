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
package bluej.pkgmgr.graphPainter;

import java.awt.*;

import bluej.Config;
import bluej.pkgmgr.target.*;

/**
 * Paints a ReadmeTarget
 * @author fisker
 * @version $Id: ReadmeTargetPainter.java 7642 2010-05-20 09:36:36Z nccb $
 */
public class ReadmeTargetPainter
{
    // Images
    private static final Image readmeImage = Config.getImageAsIcon("image.readme").getImage();
    private static final Image selectedReadmeImage = Config.getImageAsIcon("image.readme-selected").getImage();

    /**
     * Create the painter.
     */
    public ReadmeTargetPainter()
    {
    }
    
    /**
     * Paint the given target on the specified graphics context.
     * @param g  The graphics context to paint on.
     * @param target  The target to paint.
     */
    public void paint(Graphics2D g, Target target, boolean hasFocus)
    {
        boolean isSelected = target.isSelected() && hasFocus;
        g.drawImage(isSelected ? selectedReadmeImage : readmeImage, target.getX(), target.getY(), null);
    }
    
    public static int getMaxImageWidth()
    {
        return Math.max(readmeImage.getWidth(null), selectedReadmeImage.getWidth(null));
    }
    
    public static int getMaxImageHeight()
    {
        return Math.max(readmeImage.getHeight(null), selectedReadmeImage.getHeight(null));
    }
}
