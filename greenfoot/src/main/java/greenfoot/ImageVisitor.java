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
package greenfoot;

import java.awt.Graphics2D;
import java.awt.image.ImageObserver;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to Image methods that are package protected. We need some
 * package-protected methods in the Image, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ImageVisitor.java 6256 2009-04-16 11:55:51Z polle $
 */
public class ImageVisitor
{
    public static void drawImage(GreenfootImage image, Graphics2D g, int x, int y, ImageObserver observer, boolean useTranparency)
    {
        image.drawImage(g, x, y, observer, useTranparency);
    }
    
    public static boolean equal(GreenfootImage image1, GreenfootImage image2)
    {
        return GreenfootImage.equal(image1, image2);
    }
}
