package greenfoot.core;

import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    /** Constant that indicates that no version number could be found. */
    public static final double NO_VERSION = -1;
    /**
     * Name of the greenfoot package file that holds information specific to a
     * package/project
     */
    public static final String GREENFOOT_PKG_NAME = "greenfoot.project";

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
        loadProperties(projectDir);
    }

    /**
     * Loads the properties from the greenfoot properties file in the given
     * directory.
     * 
     * @param projectDir The project dir.
     * @throws IllegalArgumentException If directory can't be read or written.
     */
    private void loadProperties(File projectDir)
    {
        if (!projectDir.canWrite() || !projectDir.canRead()) {
            throw new IllegalArgumentException(
                    "Project directory must exist and be readable and writable. Project directory: " + projectDir);
        }
        initialiseFile(projectDir);
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
     * Creates the file object for this projects properties.
     * 
     * @param projectDir
     */
    private void initialiseFile(File projectDir)
    {
        propsFile = new File(projectDir, GREENFOOT_PKG_NAME);
    }

    /**
     * Stores these properties to the file.
     * 
     * @throws IOException If the properties can't be written to the properties
     *             file.
     */
    private void storeProperties()
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
     * Writes the API version into the file.
     */
    public void storeApiVersion()
    {
        System.out.println("Writing API version: " + ActorVisitor.getApiVersion());
        properties.setProperty("version", "" + ActorVisitor.getApiVersion());
        storeProperties();
    }

    /**
     * Attempts to find the version number the greenfoot API that a greenfoot
     * project was created with. If it can not find a version number, it will
     * return ProjectProperties.NO_VERSION.
     * 
     * @return API version or ProjectProperties.NO_VERSION
     */
    public double getAPIVersion()
    {
        double version = NO_VERSION;
        try {
            String versionString = properties.getProperty("version");
            if (versionString != null) {
                version = Double.parseDouble(versionString);
            }
        }
        catch (NumberFormatException e) {}
        return version;
    }

    /**
     * Sets a property as in Java's Properties class. It also immediately writes
     * the property to the file.
     * 
     * @param key
     * @param value
     * @see java.util.Properties
     */
    public void setProperty(String key, String value)
    {
        properties.setProperty(key, value);
        storeProperties();
    }

    /**
     * Gets a property as in Java's Properties class.
     * 
     * @param key
     * @see java.util.Properties
     */
    public String getProperty(String key)
    {
        return properties.getProperty(key);
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
        int lastDot = className.lastIndexOf('.');
        if (lastDot != -1) {
            className = className.substring(lastDot + 1);
        }

        GreenfootImage image = classImages.get(className);

        if (image == null) {
            // If it is the Actor class the image is always the same:
            if (className.equals("Actor")) {
                image = new GreenfootImage("images/greenfoot-logo.png");
            }
            else {
                String imageName = getProperty("class." + className + ".image");
                if (imageName != null) {
                    image = new GreenfootImage("images/" + imageName);
                }
            }
            
            if (image != null) {
                classImages.put(className, image);
            }
        }
        return image;
    }

    /**
     * Remove the cached version of an image for a particular class. This should be
     * called when the image for the class is set to something different.
     */
    public void removeCachedImage(String className)
    {
        classImages.remove(className);
    }
}
