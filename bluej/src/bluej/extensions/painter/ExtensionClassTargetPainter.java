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
package bluej.extensions.painter;

import java.awt.Graphics2D;

import bluej.extensions.BClassTarget;

/**
 * <p>
 * This interface allows extensions to provide custom implementations for
 * painting a class target. Although, extensions are not allowed to define a
 * completely new visual representation of a class target. A class target is
 * always rectangular and the name of the class is printed in the top area. Only
 * the area below the name may be customized by extensions.
 * </p>
 * <p>
 * To prevent extensions from painting over each others representation of a
 * class target, the process is split into two methods. One is responsible for
 * the background while the other prints the foreground. Extensions are
 * encouraged to decide wisely where their painting should go. However, there is
 * no guarantee that your drawing will not be painted over by another extension.
 * </p>
 * <p>
 * NOTE: The warnings of BlueJ (e.g. the stripped image, if the class is
 * uncompiled and the "broken" image if the source code could not be parsed)
 * will always be drawn <em>after</em> the extensions have drawn their images.
 * Therefore, extensions are unable to paint over the warnings or prevent them
 * from appearing.
 * </p>
 * 
 * @author Simon Gerlach
 */
public interface ExtensionClassTargetPainter
{
    /**
     * Ask the extension to draw the background of its class target
     * representation.
     * 
     * @param bClassTarget
     *            The class target that will be painted.
     * @param graphics
     *            The {@link Graphics2D} instance to draw on.
     * @param width
     *            The width of the area to paint.
     * @param height
     *            The height of the area to paint.
     */
    void drawClassTargetBackground(BClassTarget bClassTarget, Graphics2D graphics, int width, int height);

    /**
     * Ask the extension to draw the foreground of its class target
     * representation.
     * 
     * @param bClassTarget
     *            The class target that will be painted.
     * @param graphics
     *            The {@link Graphics2D} instance to draw on.
     * @param width
     *            The width of the area to paint.
     * @param height
     *            The height of the area to paint.
     */
    void drawClassTargetForeground(BClassTarget bClassTarget, Graphics2D graphics, int width, int height);
}
