package bluej.extensions;

import java.net.URL;
import bluej.Config;

/**
 * <pre>The Extensions superclass. All extensions must extend this.
 *
 *  Your class MUST have an empty parameters constructor 
 *  and it must implement all the abstract methods.
 * </pre>
 * 
 * @version    $Id: Extension.java 1712 2003-03-20 10:39:46Z damiano $
 */
public abstract class Extension
{
    /* do NOT make it final othervise the compiler will cache it and it will seem immutable
     * do NOT make it static, if one want to mess about it will mess its own...
     */
    /**
     * The major version number of the Extension API.
     */
    public int VERSION_MAJOR = 2;

    /**
     * The minor version number of the Extension API.
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
     * After your class is created this method is called, a reference on the
     * relevant BlueJ object is passed so you can interact with BlueJ This is
     * NOT a thread. You MUST return as quick as possible from this method. If
     * you start doing something you should create your own thread.
     *
     * @param  bluej  The statring point to interact with BlueJ
     */
    public abstract void startup(BlueJ bluej);


    /**
     *  When bluej decides that this extension is not longer need it will call this 
     *  method before detaching it from the system. This is needed since if an extension
     *  is reloaded I REALLY would like it to come back in the exact
     *  way it was the first time and this extension may have threads going on
     *  that it wants to shut... What you Extension writer should do here is to
     *  SHUT down everything you created. The extension may give me a not
     *  null message string that will be written to the console.
     *  In ANY case the extension will be disconnected.
     *
     * @return    A possible not null string that will be sent to the console
     */
    public abstract String terminate();


    /**
     * BlueJ will call this method to display the version of the loaded extension.
     * Please limit the string to five or 10 chars.
     * NOTE: This is NOT the verion of the Extension API, it is the Verions of the 
     * Extension itself !
     *
     * @return    The version of this extensions
     */
    public abstract String getVersion();


    /**
     *  Gets a description of the extension's function.
     *
     * @return    A description of the extension.
     */
    public String getDescription()
    {
        return Config.getString("extensions.nodescription");
    }


    /**
     *  Gets a URL for more information about the extension, including possible
     *  upgrades and configuration details.
     *
     * @return    a website containing more information
     */
    public URL getURL()
    {
        return null;
    }
}
