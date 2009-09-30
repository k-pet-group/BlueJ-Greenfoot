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

import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;


/**
 * A filter used to just display image files understood by all the currently
 * registered readers.
 * @author Michael Berry (mjrb4)
 * @version 09/07/09
 */
public class ImageFilter extends FileFilter
{

    /**
     * Decide whether or not the file should be accepted by the ImageFilter.
     * All currently registered image files as well as directories should be
     * accepted.
     * @param f the file or directory to check for acceptance
     * @return true if the file is accepted in the filter, false otherwise
     */
    public boolean accept(File f)
    {
        if(f.isDirectory()) {
            return true;
        }
        for(String s : ImageIO.getReaderFileSuffixes()) {
            if(getExtension(f).equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    /**
     * Get the description of this ImageFilter.
     */
    public String getDescription()
    {
        return "Images";
    }

    /**
     * Get the extension of a file.
     * @see http://java.sun.com/docs/books/tutorial/uiswing/examples/components/FileChooserDemo2Project/src/components/Utils.java
     */
    private String getExtension(File f) {
        String ext = "";
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

}
