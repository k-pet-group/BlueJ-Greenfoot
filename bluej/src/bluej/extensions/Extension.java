package bluej.extensions;

import java.net.URL;
import bluej.Config;

/**
 * The Extensions superclass. All extensions must extend this. Use the constructor
 * that takes <CODE>BlueJ</CODE> as a parameter, so that you have a reference to the
 * proxy hierarchy.
 * <p><b>This API is version 1.1</b>
 *
 * @author Clive Miller
 * @version $Id: Extension.java 1459 2002-10-23 12:13:12Z jckm $
 */
public abstract class Extension
{
    /**
     * The major version number of the Extension API
     */
    public static final int VERSION_MAJOR = 1;

    /**
     * The minor version number of the Extension API
     */
    public static final int VERSION_MINOR = 1;
    
    /**
     * Do not override this constructor, since it does not provide a
     * reference to the BlueJ proxy hierarchy. However, it is
     * present so that no <CODE>super</CODE> calls are necessary
     * by subclasses.
     */
    public Extension()
    {
    }

    /**
     * A reference on the relevant BlueJ object is passed in this constructor.
     * This class' constructor does nothing: it does not need to be called
     * by subclasses. Subclasses should take a copy of the reference for
     * access to the BlueJ structure.
     *
     */
    public Extension (BlueJ bluej)
    {
    }
    
    /**
     * Provide the caller with a major version number. This should not
     * change frequently, but only due to a significant change in the extension.
     * @return the major part of the current version.
     */
    public abstract int getVersionMajor();
    
    /**
     * Provide the caller with a minor version number. This should be increased
     * on every release.
     * @return the minor part of the current version.
     */
    public abstract int getVersionMinor();
    
    /**
     * Determine whether this extension is compatible with a particular version of
     * the extensions API. Typically <I>compatible</I> indicates that the major version
     * is the same, and the minor version of the API is the same or greater than the
     * intended version. This has nothing to do with the version of the extension
     * itself, only the API it is expecting to work with. Usually use code like this:
     * <PRE>
     *    private static final int BUILT_FOR_MAJOR = 1;
     *    private static final int BUILD_FOR_MINOR = 0;
     *    public boolean isCompatibleWith (int majorVersion, int minorVersion)
     *    {
     *        return (majorVersion == BUILT_FOR_MAJOR && minorVersion >= BUILD_FOR_MINOR);
     *    }</PRE>
     * However, if you <I>know</I> that it also works with other version, this can be modified
     * accordingly.
     */
    public abstract boolean isCompatibleWith (int majorVersion, int minorVersion);
    
    /**
     * Gets a description of the extension's function. This should include a
     * description of the functionality, any menu items it uses, any
     * Preference Panel items used and any restrictions on it.
     * <BR>Handy hint: you can use <CODE>\n</CODE>, even in language definition files, and they will be interpreted!
     * @return as long a description as you like. It's displayed in a text area, so you can't
     * use HTML or anything fancy, but newlines are observed.
     */
    public String getDescription()
    {
        return Config.getString ("extensions.nodescription");
    }
    
    /**
     * Gets a URL for more information about the extension, including possible upgrades
     * and configuration details.
     * @return a website containing more information
     */
    public URL getURL()
    {
        return null;
    }
}