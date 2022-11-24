/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.importer.scratch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

/**
 * Equivalent of the Scratch ImageMedia class.
 * 
 * This is primarily a container that holds a ScratchImage inside it (which you can get at with getImage()).
 * 
 * It also has support for things like holding an original copy of a JPEG
 * and compositing text with images, but we don't support that at the moment.
 * 
 * @author neil
 *
 */
public class ImageMedia extends ScratchMedia
{
    // If non-null, the File that the image has been saved into
    private File imageFile;

    public ImageMedia(int version, List<ScratchObject> scratchObjects)
    {
        super(ScratchUserObject.IMAGE_MEDIA, version, scratchObjects);
    }

    // Fields:
    //  form (ScratchImage), rotationCenter (?), textBox (?), jpegBytes (?), compositeForm (?) 
    
    public int fields()
    {
        return super.fields() + 5;
    }    
    
    private ScratchImage getImage()
    {
        if (scratchObjects.get(super.fields() + 4) != null) {
            return (ScratchImage) scratchObjects.get(super.fields() + 4);
        } else {
            return (ScratchImage)scratchObjects.get(super.fields() + 0);
        }
    }
    
    private byte[] getJpegBytes()
    {
        ScratchObject obj = scratchObjects.get(super.fields() + 3);
        if (obj == null) {
            return null;
        } else {
            return (byte[]) obj.getValue();
        }
    }
    
    public ScratchPoint getRotationCentre()
    {
        return (ScratchPoint) scratchObjects.get(super.fields() + 1);
    }
    
    public int getWidth()
    {
        byte[] jpegBytes = getJpegBytes();
        if (jpegBytes != null) {
            try {
                return ImageIO.read(new ByteArrayInputStream(jpegBytes)).getWidth();
            } catch (IOException e) {
                return -1;
            }
        } else {
            return getImage().getWidth();
        }        
    }
    
    public int getHeight()
    {
        byte[] jpegBytes = getJpegBytes();
        if (jpegBytes != null) {
            try {
                return ImageIO.read(new ByteArrayInputStream(jpegBytes)).getHeight();
            } catch (IOException e) {
                return -1;
            }
        } else {
            return getImage().getHeight();
        }        
    }

    @Override public File saveInto(File destDir, Properties props, String prefix) throws IOException
    {       
        if (imageFile == null) {
            byte[] jpegBytes = getJpegBytes();
            
            String extension = jpegBytes == null ? "png" : "jpg";
            
            File imageDir = new File(destDir, "images");
            imageDir.mkdirs();
            for (int i = -1;;i++) {
                // First try without addition, then append numbers until we find a free file:
                imageFile = new File(imageDir, prefix + mungeChars(getMediaName()) + (i < 0 ? "" : "_" + i) + "." + extension);
                if (false == imageFile.exists())
                    break;
            }
            
            if (jpegBytes != null) {
                FileOutputStream fos = new FileOutputStream(imageFile);
                fos.write(jpegBytes);
                fos.close();
            } else {
                ImageIO.write(getImage().getBufferedImage(), "png", imageFile);
            }
        }
        
        return imageFile;
    }
    
    private static String mungeChars(String name)
    {
        // Replace special characters (colons and slashes) with underscore:
        return name.replaceAll("[:/\\\\]", "_");
    }
}
