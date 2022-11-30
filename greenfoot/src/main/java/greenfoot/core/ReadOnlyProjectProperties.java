package greenfoot.core;

import greenfoot.GreenfootImage;
import greenfoot.util.GreenfootUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A read-only interface to access properties of a project.
 * 
 * Implementing classes only need implement one method: getString(String key, String defaultValue).
 */
public interface ReadOnlyProjectProperties
{

    /**
     * Gets a property as in Java's Properties class. Thread-safe.
     */
    public String getString(String key, String defaultValue);

    /**
     * Gets a String property.  Returns null if property not present.
     */
    public default String getString(String key)
    {
        return getString(key, null);
    }

    /**
     * Gets an integer property with the given key.
     */
    public default int getInt(String key) throws NumberFormatException
    {
        String number = getString(key);
        return Integer.parseInt(number);
    }

    /**
     * Gets a boolean property as in Java's Properties class. 
     * Allows the specification of a default value. Thread-safe.
     */
    public default boolean getBoolean(String key, boolean defaultValue)
    {
        String bool = getString(key, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(bool);
    }

    /**
     * Gets an image for the given class. The images are cached to avoid loading
     * images several times. This method is thread-safe.
     *
     * @param className If it is a qualified name, the package is ignored.
     *            Returns null, if there is no entry for this class in the
     *            properties.
     * @return The image.
     */
    @OnThread(Tag.Simulation)
    public default GreenfootImage getImage(String className)
    {
        return GreenfootUtil.getGreenfootImage(className, getString("class." + className + ".image"));
    }
}
