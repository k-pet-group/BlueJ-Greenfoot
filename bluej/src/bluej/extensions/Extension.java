package bluej.extensions;

import java.net.URL;
import bluej.Config;

/**
 *  The Extensions superclass. All extensions must extend this. <p>
 *
 *  Your class MUST have an empty parameters constructor 
 *  and it must implement all the abstract methods.
 *
 * @version    $Id: Extension.java 1662 2003-03-07 09:57:11Z damiano $
 */
public abstract class Extension
{
    /**
     * The major version number of the Extension API.
     * do NOT make it final othervise the compiler will cache it and it will seem immutable
     * do NOT make it static, if one want to mess about it will mess its own...
     */
    public int VERSION_MAJOR = 2;

    /**
     * The minor version number of the Extension API.
     * do NOT make it final othervise the compiler will cache it and it will seem immutable
     * do NOT make it static, if one want to mess about it will mess its own...
     */
    public int VERSION_MINOR = 1;

    /**
     * Determine whether this extension is compatible with a particular version
     * of the extensions API. This method is called BEFORE the startup method.
     * The extension writer can use the VERSION_MAJOR and MINOR as an aid to determine
     * if his extension is compatible with the current BlueJ release.
     *
     * @return true or false
     */
    public abstract boolean isCompatible();

    /**
     * After your class is created this method is called A reference on the
     * relevant BlueJ object is passed so you can interact with BlueJ This is
     * NOT a thread. You MUST return as quick as possible from this method. If
     * you start doing something you should create your own thread.
     *
     * @param  bluej  The statring point to interact with BlueJ
     */
    public abstract void startup(BlueJ bluej);


    /**
     *  I need a way to tell to the extension to go away, something it needs to
     *  do before being disconnected from the system. This is needed since if I
     *  "reload" an extension I REALLY would like it to come back in the exact
     *  way it was the first time and this extension may have threads going on
     *  that I want to shut... What you Extension writer should do here is to
     *  SHUT down everything you created I try to be nice, you may give me a not
     *  null message string that I may display to the user :-) In ANY case I am
     *  going to disconnect
     *
     * @return    A possible not null string that will be sent to the console
     */
    public abstract String terminate();


    /**
     * BlueJ will call this method to display the version of the extension that is implementing
     * this method. Please limit the string to five or 10 chars.
     * NOTE: This is NOT the verion of the Extension API, it is the Verions of the 
     * Extension itself !
     *
     * @return    The version of this extensions
     */
    public abstract String getVersion();


    /**
     *  Gets a description of the extension's function. This should include a
     *  description of the functionality, any menu items it uses, any Preference
     *  Panel items used and any restrictions on it. <BR>
     *  Handy hint: you can use <CODE>\n</CODE>, even in language definition
     *  files, and they will be interpreted!
     *
     * @return    as long a description as you like. It's displayed in a text
     *      area, so you can't use HTML or anything fancy, but newlines are
     *      observed.
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
