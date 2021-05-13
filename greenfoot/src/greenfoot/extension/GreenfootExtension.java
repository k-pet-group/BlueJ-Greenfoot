/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013,2016,2018,2019  Poul Henriksen and Michael Kolling
 
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
package greenfoot.extension;

import bluej.Boot;
import bluej.collect.DataSubmissionFailedDialog;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;
import greenfoot.guifx.GreenfootGuiHandler;

import java.net.MalformedURLException;
import java.net.URL;

import bluej.Config;
import bluej.Main;
import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.event.ApplicationEvent;
import bluej.extensions2.event.ApplicationListener;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This is the starting point of Greenfoot as a BlueJ Extension.
 * 
 * @author Poul Henriksen
 */
public class GreenfootExtension extends Extension
{
    private BlueJ theBlueJ;

    /**
     * When this method is called, the extension may start its work.
     */
    public void startup(BlueJ bluej)
    {
        theBlueJ = bluej;
        Main.setGuiHandler(new GreenfootGuiHandler());
    }

    /**
     * This method must decide if this Extension is compatible with the current
     * release of the BlueJ Extensions API
     */
    public boolean isCompatible()
    {
        return Config.isGreenfoot();
    }

    /**
     * Returns the version number of this extension
     */
    public String getVersion()
    {
        return ("2003.03");
    }

    /**
     * Returns the user-visible name of this extension
     */
    public String getName()
    {
        return ("greenfoot Extension");
    }

    @Override
    public String getDescription()
    {
        return ("greenfoot extension");
    }

    /**
     * Returns a URL where you can find info on this extension. The real problem
     * is making sure that the link will still be alive in three years...
     */
    @Override
    public URL getURL()
    {
        try {
            return new URL("http://www.greenfoot.org");
        }
        catch (MalformedURLException e) {
            return null;
        }
    }
    
}
