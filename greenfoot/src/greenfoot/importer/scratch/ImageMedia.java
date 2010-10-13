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

import greenfoot.core.GProject;

import java.io.IOException;
import java.util.List;

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
    
    ScratchImage getImage()
    {
        return (ScratchImage)scratchObjects.get(super.fields() + 0);
    }

    @Override public String saveInto(GProject project) throws IOException
    {
        // Save the image that we are wrapping:
        String imageFileName = getImage().saveInto(project);
        
        return imageFileName;
        //TODO use mediaName from getImage() and do the saving here rather than in ScratchImage
    }
}
