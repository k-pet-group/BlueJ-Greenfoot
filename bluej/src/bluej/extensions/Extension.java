package bluej.extensions;

import java.net.URL;
import bluej.Config;

/**
 * The Extensions superclass, all extensions must extend this class.
 * Your class must have an empty constructor.
 * 
 * @version    $Id: Extension.java 1849 2003-04-14 14:05:01Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
 
public abstract class Extension
{
    /* do NOT make it final otherwise the compiler will cache it and it will seem immutable
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
     * of the extensions API. This method is called before the startup() method.
     * The extension writer can use the VERSION_MAJOR and VERSION_MINOR as an aid to determine
     * if his extension is compatible with the current BlueJ release.
     */
    public abstract boolean isCompatible();

    /**
     * After the Extensions is created this method is called.
     * A reference on the relevant BlueJ object is passed so you can interact with BlueJ.
     * This is not a thread. You must return as quick as possible from this method. If
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
     * Should return the version of the loaded extension.
     * Please limit the string to five or 10 chars.
     * This will be displayed into the Help->Installed Extensions
     */
    public abstract String getVersion();

    /**
     * Should return a description of the extension's function.
     * It should be a brief statement of the Extension purpose.
     */
    public String getDescription()
    {
        return Config.getString("extensions.nodescription");
    }

    /**
     * Should return a URL for more information about the extension.
     * Ideally this includes complete manual, possible upgrades and configuration details.
     * If no url is available then null can be returned.
     */
    public URL getURL()
    {
        return null;
    }
}
