package bluej.extensions;

import java.net.URL;
import bluej.Config;

/**
 *  The Extensions superclass. All extensions must extend this. <p>
 *
 *  <b>This API is version 1.2</b> Your class MUST have an empty parameters
 *  constructor AND implement the startup and teminate methods.
 *
 * @version    $Id: Extension.java 1504 2002-11-18 08:29:39Z damiano $
 */
public abstract class Extension
{
    /**
     *  The major version number of the Extension API
     */
    public final static int VERSION_MAJOR = 1;

    /**
     *  The minor version number of the Extension API
     */
    public final static int VERSION_MINOR = 2;


    /**
     *  After your class is created this method is called A reference on the
     *  relevant BlueJ object is passed so you can interact with BlueJ This is
     *  NOT a thread. You MUST return as quick as possible from this method. If
     *  you start doing something you should create your own thread.
     *
     * @param  bluej  Description of the Parameter
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
     * @return    Description of the Return Value
     */
    public abstract String terminate();


    /**
     *  Provide the caller with a major version number. This should not change
     *  frequently, but only due to a significant change in the extension.
     *
     * @return    the major part of the current version.
     */
    public abstract int getVersionMajor();


    /**
     *  Provide the caller with a minor version number. This should be increased
     *  on every release.
     *
     * @return    the minor part of the current version.
     */
    public abstract int getVersionMinor();


    /**
     *  Determine whether this extension is compatible with a particular version
     *  of the extensions API. Typically <I>compatible</I> indicates that the
     *  major version is the same, and the minor version of the API is the same
     *  or greater than the intended version. This has nothing to do with the
     *  version of the extension itself, only the API it is expecting to work
     *  with. Usually use code like this: <PRE>
     *    private static final int BUILT_FOR_MAJOR = 1;
     *    private static final int BUILD_FOR_MINOR = 0;
     *    public boolean isCompatibleWith (int majorVersion, int minorVersion)
     *    {
     *        return (majorVersion == BUILT_FOR_MAJOR && minorVersion >= BUILD_FOR_MINOR);
     *    }</PRE> However, if you <I>know</I> that it also works with other
     *  version, this can be modified accordingly.
     *
     * @param  majorVersion  Description of the Parameter
     * @param  minorVersion  Description of the Parameter
     * @return               The compatibleWith value
     */
    public abstract boolean isCompatibleWith(int majorVersion, int minorVersion);


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
