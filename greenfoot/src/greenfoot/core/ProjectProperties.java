package greenfoot.core;

import greenfoot.GreenfootImage;
import greenfoot.WorldVisitor;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Represents the persistent properties associated with a greenfoot project. It
 * represents both the file that holds the properties and the actual properties.
 * 
 * @author Poul Henriksen
 * 
 */
public class ProjectProperties
{
    /** String printed in the top of the properties file. */
    private static final String FILE_HEADER = "Greenfoot properties";

    /**
     * Name of the greenfoot package file that holds information specific to a
     * package/project
     */
    public static final String GREENFOOT_PKG_NAME = "project.greenfoot";

    /** Holds the actual properties */
    private Properties properties;

    /** Reference to the file that holds the properties */
    private File propsFile;
    
    /** Holds images for classes. Avoids loading the same image twice */
    public Map<String, GreenfootImage> classImages = new HashMap<String, GreenfootImage>();

    /**
     * Creates a new properties instance for the project in the given directory.
     * The directory has to exist.
     * 
     * @param projectDir
     */
    public ProjectProperties(File projectDir)
    {
        properties = new Properties();
        load(projectDir);
    }
    
    /**
     * Creates a new properties instance with the file loaded from the root of this class loader.
     * 
     * @param projectDir
     */
    public ProjectProperties()
    {
        properties = new Properties();
        load();
    }

    /**
     * Tries to load the project-file with the default class loader.
     */
    private void load()
    {
        URL probsFile = this.getClass().getResource("/" + GREENFOOT_PKG_NAME);
        InputStream is = null;
        try {
            is = probsFile.openStream();
            properties.load(is);
        }
        catch (IOException ioe) {
            // if it does not exist, we will create it later if something needs
            // to be written to it. This makes it work with scenarios created
            // with earlier versions of greenfoot that does not contain a
            // greenfoot project properties file.
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {}
            }
        }
    }

    /**
     * Loads the properties from the greenfoot properties file in the given
     * directory.
     * 
     * @param projectDir The project dir.
     * @throws IllegalArgumentException If directory can't be read or written.
     */
    private void load(File projectDir)
    {
        propsFile = new File(projectDir, GREENFOOT_PKG_NAME);

        InputStream is = null;
        try {
            is = new FileInputStream(propsFile);
            properties.load(is);
        }
        catch (IOException ioe) {
            // if it does not exist, we will create it later if something needs
            // to be written to it. This makes it work with scenarios created
            // with earlier versions of greenfoot that does not contain a
            // greenfoot project properties file.
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {}
            }
        }
    }
    
    
    /**
     * Stores these properties to the file.
     * 
     * @throws IOException If the properties can't be written to the properties
     *             file.
     */
    public void save()
    {
        OutputStream os = null;
        try {
            os = new FileOutputStream(propsFile);
            properties.store(os, FILE_HEADER);
        }
        catch (FileNotFoundException e) {}
        catch (IOException e) {}
        finally {
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException e) {}
            }
        }
    }


    /**
     * Sets a property as in Java's Properties class. 
     */
    public void setString(String key, String value)
    {
        properties.setProperty(key, value);
    }


    /**
     * Gets a property as in Java's Properties class.
     */
    public String getString(String key)
    {
        return properties.getProperty(key);
    }


    /**
     * Sets an int property as in Java's Properties class. 
     */
    public void setInt(String key, int value)
    {
        properties.setProperty(key, Integer.toString(value));
    }


    /**
     * Gets an int property as in Java's Properties class.
     */
    public int getInt(String key) throws NumberFormatException
    {
        String number = properties.getProperty(key);
        return Integer.parseInt(number);
    }

    /**
     * Remove a property; return its old value.
     * @param key  The property name
     */
    public String removeProperty(String key)
    {
        return (String) properties.remove(key);
    }

    /**
     * Gets an image for the given class. The images are cached to avoid loading
     * images several times.
     * 
     * @param className If it is a qualified name, the package is ignored.
     *            Returns null, if there is no entry for this class in the
     *            properties.
     * @return The image.
     */
    public GreenfootImage getImage(String className)
    {
        className = GreenfootUtil.extractClassName(className);
        
        synchronized (classImages) {
            GreenfootImage image = classImages.get(className);

            if (image == null) {
                // If it is the Actor class the image is always the same:
                if (className.equals("Actor")) {
                    image = new GreenfootImage(GreenfootUtil.getGreenfootLogoPath().toString());
                }
                else {
                    String imageName = getString("class." + className + ".image");
                    if (imageName != null) {
                        try {
                            image = new GreenfootImage("images/" + imageName);
                        }
                        catch (IllegalArgumentException iae) {
                            // This occurs if the image file doesn't exist anymore
                        }
                    }
                }

                if (image != null) {
                    classImages.put(className, image);
                }
            }
            return image;
        }
    }


    /**
     * Remove the cached version of an image for a particular class. This should be
     * called when the image for the class is changed.
     */
    public void removeCachedImage(String className)
    {
        synchronized (classImages) {
            classImages.remove(className);
        }
    }


    /**
     * Stores the API version.
     */
    public void setApiVersion()
    {
        properties.setProperty("version", WorldVisitor.getApiVersion().toString());
    }


    /**
     * Attempts to find the version number the greenfoot API that a greenfoot
     * project was created with. If it can not find a version number, it will
     * return Version.NO_VERSION.
     * 
     * @return API version or Version.NO_VERSION
     */
    public Version getAPIVersion()
    {
        Version version = Version.NO_VERSION;
        String versionString = properties.getProperty("version");
        if(versionString != null) {
            version = new Version(versionString);
        }
        return version;
    }
}
