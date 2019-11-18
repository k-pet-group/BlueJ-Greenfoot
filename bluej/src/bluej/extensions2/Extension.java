/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2014,2019  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.extensions2;

import bluej.Config;

import java.net.URL;

/**
 * Defines the interface between BlueJ and an extension. All extensions must extend this class.
 * A concrete extension class must also have a no-arguments constructor.
 * 
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003,2004
 */
public abstract class Extension
{
    /**
     * Determines whether this extension is compatible with a particular version
     * of the extensions API. This method is called before the startup() method.
     * An extension can use {@link #getExtensionsAPIVersionMajor()} and {@link #getExtensionsAPIVersionMinor()} as an aid to determine
     * whether it is compatible with the current BlueJ release.
     *
     * @return A boolean value indicating if the extension is compatible with the extension API (<code>true</code>).
     */
    public abstract boolean isCompatible();

    /**
     * Obtains the major version of the Extensions API.
     *
     * @return An integer indicating the major version of the Extensions API. Currently 3.
     */
    protected static final int getExtensionsAPIVersionMajor()
    {
        return 3;
    }

    /**
     * Obtains the minor version of the Extensions API.
     *
     * @return An integer indicating the minor version of the Extensions API. Currently 0.
     */
    protected static final int getExtensionsAPIVersionMinor()
    {
        return 0;
    }

    /**
     * Called when the extension can start its activity.
     * This is not called on a separate thread. Extensions should return as quick as 
     * possible from this method after creating their own thread if necessary.
     *
     * @param  bluej  a {@link BlueJ} object which serves as the starting point for interactions with BlueJ.
     */
    public abstract void startup(BlueJ bluej);

    /**
     * Called when the extension should tidy up and terminate.
     * When BlueJ decides that this extension is no longer needed it will call this 
     * method before removing it from the system. Note that an extension may
     * be reloaded after having been terminated.
     *
     * <p>Any attempt by an extension to call methods on its
     * {@link BlueJ} object after this method has been called will
     * result in an (unchecked) {@link ExtensionUnloadedException}
     * being thrown by the {@link BlueJ} object.
     */
    public void terminate()
    {
    }

    /**
     * Should return a name for this extension. This will be displayed in the Help -&gt; Installed Extensions
     * dialog.
     * 
     * <p>Please limit the name to between 5 and 10 characters, and bear in mind the possibility of name
     * conflicts.
     */
    public abstract String getName();

    /**
     * Should return the version of the extension.
     * Please limit the string to between 5 and 10 characters.
     * This will be displayed in the Help -&gt; Installed Extensions dialog
     */
    public abstract String getVersion();

    /**
     * Should return a description of the extension's function.
     * It should be a brief statement of the extension's purpose.
     * This will be displayed in the Help -&gt; Installed Extensions dialog
     */
    public String getDescription()
    {
        return Config.getString("extensions.nodescription");
    }

    /**
     * Should return a URL where more information about the extension is available.
     * This will be displayed in the Help -&gt; Installed Extensions dialog.
     *
     * <p>Ideally the information provided at the URL includes a complete manual, possible
     * upgrades and configuration details.
     *
     * @return A {@link URL} object referring to the URL of the extension, <code>null</code> if no information is available.
     */
    public URL getURL()
    {
        return null;
    }
}
