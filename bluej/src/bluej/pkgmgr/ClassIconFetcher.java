/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling 
 
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
package bluej.pkgmgr;

import javafx.beans.binding.ObjectExpression;
import javafx.scene.image.Image;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An interface for fetching the image associated with a given class.  For use in Greenfoot,
 * where a class can have an associated image.
 */
public interface ClassIconFetcher
{
    /**
     * Gets an observable expression with the image for the given class
     * @param name The fully qualified name of the class
     * @return The observable expression with that class's image.  Valid as long
     *         as the class is not removed or renamed.  If no such class can be
     *         found, null will be returned.  If the class is found but has no image,
     *         an observable with null inside will be returned.
     */
    @OnThread(Tag.FXPlatform)
    ObjectExpression<Image> fetchFor(String name);
}
