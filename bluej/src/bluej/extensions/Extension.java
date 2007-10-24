package bluej.extensions;

import java.net.URL;
import bluej.Config;

/**
 * Defines the interface between BlueJ and an extension. All extensions must extend this class.
 * A concrete extension class must also have a no-arguments constructor.
 * 
 * @version    $Id: Extension.java 5344 2007-10-24 16:14:40Z iau $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003,2004
 */
 
public abstract class Extension
{
    /* do NOT make it final otherwise the compiler will cache it and it will seem immutable
     * do NOT make it static, if one want to mess about it will mess its own...
     */
    /**
     * The major version number of the Extension API.
     * Provided so that extensions can check for compatibility.
     */
    public int VERSION_MAJOR = 2;

    /**
     * The minor version number of the Extension API.
     * Provided so that extensions can check for compatibility.
     */
    public int VERSION_MINOR = 6;

    /**
     * Determine whether this extension is compatible with a particular version
     * of the extensions API. This method is called before the startup() method.
     * An extension can use the VERSION_MAJOR and VERSION_MINOR as an aid to determine
     * whether it is compatible with the current BlueJ release.
     */
    public abstract boolean isCompatible();

    /**
     * Called when the extension can start its activity.
     * This is not called on a separate thread. Extensions should return as quick as 
     * possible from this method after creating their own thread if necessary.
     *
     * @param  bluej  The starting point for interactions with BlueJ
     */
    public abstract void startup(BlueJ bluej);


    /**
     * Called when the extension should tidy up and terminate.
     * When BlueJ decides that this extension is no longer needed it will call this 
     * method before removing it from the system. Note that an extension may
     * be reloaded after having been terminated.
     *
     * Any attempt by an extension to call methods on its
     * <code>BlueJ</code> object after this method has been called will
     * reult in an (unchecked) <code>ExtensionUnloadedException</code>
     * being thrown by the <code>BlueJ</code> object.
     */
    public void terminate()
    {
    }

    /**
     * Should return a name for this extension.
     * Please limit the name to five to 10 characters.
     * This will be displayed in the Help->Installed Extensions dialog.
     * Bear in mind of possible name conflicts.
     */
    public abstract String getName();


    /**
     * Should return the version of the extension.
     * Please limit the string to five to 10 characters.
     * This will be displayed in the Help->Installed Extensions dialog
     */
    public abstract String getVersion();

    /**
     * Should return a description of the extension's function.
     * It should be a brief statement of the extension's purpose.
     * This will be displayed in the Help->Installed Extensions dialog
     */
    public String getDescription()
    {
        return Config.getString("extensions.nodescription");
    }

    /**
     * Should return a URL where more information about the extension is available.
     * Ideally this includes complete manual, possible upgrades and configuration details.
     * If no information is available then null can be returned.
     * This will be displayed in the Help->Installed Extensions dialog
     */
    public URL getURL()
    {
        return null;
    }
}
