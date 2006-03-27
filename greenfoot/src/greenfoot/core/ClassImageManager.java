package greenfoot.core;

import greenfoot.GreenfootImage;

/**
 * Interface for a mechanism to get the default image for greenfoot classes
 * 
 * @author Davin McCall
 * @version $Id: ClassImageManager.java 3882 2006-03-27 03:44:41Z davmac $
 */
public interface ClassImageManager
{
    /**
     * Return the image for a particular class. May return null if the image
     * has not been set for the specified class.
     * 
     * @param className  The class whose image to get
     * @return           The name of the image file for the class
     */
    public GreenfootImage getClassImage(String className);
}
