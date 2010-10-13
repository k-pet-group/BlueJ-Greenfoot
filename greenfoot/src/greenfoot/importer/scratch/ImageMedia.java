package greenfoot.importer.scratch;

import greenfoot.core.GProject;

import java.io.IOException;
import java.util.List;

import bluej.utility.Debug;

public class ImageMedia extends ScratchMedia
{

    public ImageMedia(int version, List<ScratchObject> scratchObjects)
    {
        super(ScratchUserObject.IMAGE_MEDIA, version, scratchObjects);
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
