package bluej.extensions;

import java.net.URL;
import bluej.Config;

/**
 * The Extensions superclass, all extensions must extend this class.
 * Your class must have an empty constructor.
 * 
 * @version    $Id: Extension.java 1800 2003-04-10 09:36:34Z damiano $
 */
public abstract class Extension
{
    /* do NOT make it final othervise the compiler will cache it and it will seem immutable
     * do NOT make it static, if one want to mess about it will mess its own...
     */
    /**
     * The major version number of the Extension API.
     * Provided so that Extensions can check for compatibility.
     */
    public int VERSION_MAJOR = 2;

    /**
     * The minor version number of the Extension API.
     * Provided so that Extensions can check for compatibility.
     */
    public int VERSION_MINOR = 1;

    /**
     * Determine whether this extension is compatible with a particular version
     * of the extensions API. This method is called BEFORE the startup() method.
     * The extension writer can use the VERSION_MAJOR and VERSION_MINOR as an aid to determine
     * if his extension is compatible with the current BlueJ release.
     *
     * @return true or false
     */
    public abstract boolean isCompatible();

    /**
     * After the Extensions is created this method is called.
     * A reference on the relevant BlueJ object is passed so you can interact with BlueJ.
     * This is NOT a thread. You MUST return as quick as possible from this method. If
     * you start doing something a thread should be created.
     *
     * @param  bluej  The statring point to interact with BlueJ
     */
    public abstract void startup(BlueJ bluej);


    /**
     * Terminate this extension.
     * When bluej decides that this extension is not longer needed it will call this 
     * method before detaching it from the system. Note that an extension may
     * be reloaded after having been unloaded. What you Extension writer should do here is to
     * shut down everything you created. The extension may return a not
     * null message string that will be written to the console.
     * In any case the extension will be disconnected from BlueJ.
     *
     * @return    A possible not null string that will be sent to the console
     */
    public String terminate()
    {
        return null;
    }

    /**
     * Return to BlueJ the version of the loaded extension.
     * Please limit the string to five or 10 chars.
     */
    public abstract String getVersion();

    /**
     * Return to BlueJ a description of the extension's function.
     * It should be a brief statement of the Extension purpose.
     */
    public String getDescription()
    {
        return Config.getString("extensions.nodescription");
    }

    /**
     * Return to BlueJ a URL for more information about the extension.
     * Ideally this includes complete manual, possible upgrades and configuration details.
     */
    public URL getURL()
    {
        return null;
    }
}
