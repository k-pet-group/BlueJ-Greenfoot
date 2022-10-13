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
package greenfoot.guifx.images;

import bluej.utility.Debug;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.net.MalformedURLException;

/**
 * An entry in a ListView of image files, which is used in the image lists' frames.
 *
 * @author Amjad Altadmri
 */
public class ImageListEntry
{
    private final File imageFile;
    private final boolean inProjectList;
    private ImageView icon;
    private long lastModified;

    /**
     * Construct an image list entry for a specific file.

     * @param file          The image file; could be null.
     * @param inProjectList {@code true} if the contained list is the project's image files one,
     *                          {@code false} if it is a greenfoot library's list.
     *
     */
    public ImageListEntry(File file, boolean inProjectList)
    {
        this.imageFile = file;
        if (imageFile != null)
        {
            lastModified = file.lastModified();
        }
        this.inProjectList = inProjectList;
    }

    /**
     * Returns the image file name.
     *
     * @return the image's file name.
     */
    public String getImageName()
    {
        return imageFile.getName();
    }

    /**
     * Return a thumbnail icon of the image. It checks its existence
     * first to avoid reconstruction each time.
     *
     * @return an Image view of the image file.
     */
    public ImageView getIcon()
    {
        if (icon == null && imageFile != null)
        {
            icon = getImageView();
        }
        return icon;
    }

    /**
     * Loads the image file and construct an image view of it.
     * This view is returned of the loading succeeded, otherwise
     * an empty view is returned.
     *
     * @return an image view containing a thumbnail of the image.
     */
    private ImageView getImageView()
    {
        try
        {
            Image image = new Image(imageFile.toURI().toURL().toExternalForm());
            ImageView view = new ImageView(image);
            int maxWidth = inProjectList ? 40 : 60;
            if (image.getWidth() > maxWidth)
            {
                view.setFitWidth(maxWidth);
                view.setPreserveRatio(true);
            }
            return view;
        }
        catch (MalformedURLException e)
        {
            Debug.reportError(e);
        }
        return new ImageView();
    }

    /**
     * Returns the image file in this entry.
     *
     * @return the image's file or null if it doesn't exist.
     */
    public File getImageFile()
    {
        return imageFile;
    }

    /**
     * Returns the value of the inProjectList filed.
     *
     * @return {@code true} if the contained list is the project's image files one,
     *            {@code false} if it is a greenfoot library's list.
     */
    public boolean isInProjectList()
    {
        return inProjectList;
    }

    /**
     * Indicates whether some other entry has the same image file and it has not changed,
     * or both entries has no image files.
     *
     * @param other  the reference object with which to compare.
     * @return {@code true} only in two cases:
     *              - both entries have a null image file,
     *              - both entries have the same image file and it has not been modified;
     *         {@code false} otherwise.
     */
    @Override
    public boolean equals(Object other)
    {
        if( !(other instanceof ImageListEntry) )
        {
            return false;
        }

        //other cannot be null here because it passed the instanceof check above.
        ImageListEntry otherEntry = (ImageListEntry) other;
        File otherImageFile = otherEntry.imageFile;

        if (otherImageFile == null && imageFile == null)
        {
            return true;
        }
        else if (otherImageFile == null || imageFile == null)
        {
            return false;
        }

        // We consider them equal entries if they has the same file and it has not been modified.
        return otherImageFile.equals(imageFile) && otherEntry.lastModified == this.lastModified;
    }

    /**
     * Returns a hash code value for the entry. We use the same hash code of the contained image file's object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode()
    {
        return imageFile.hashCode();
    }
}