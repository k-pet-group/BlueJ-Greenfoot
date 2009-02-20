/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package greenfoot.platforms.ide;

import greenfoot.platforms.GreenfootUtilDelegate;
import greenfoot.util.FileChoosers;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.Hashtable;

import bluej.Config;
import bluej.runtime.ExecServer;
import bluej.utility.BlueJFileReader;
import bluej.utility.FileUtility;

public class GreenfootUtilDelegateIDE implements GreenfootUtilDelegate
{
    /**
     * 
     * Creates the skeleton for a new class
     * 
     */
    public void createSkeleton(String className, String superClassName, File file, String templateFileName) throws IOException   {
        Dictionary<String, String> translations = new Hashtable<String, String>();
        translations.put("CLASSNAME", className);
        if(superClassName != null) {
            translations.put("EXTENDSANDSUPERCLASSNAME", " extends " + superClassName);
        } else {
            translations.put("EXTENDSANDSUPERCLASSNAME", "");
        }
        String baseName = "greenfoot/templates/" +  templateFileName;
        File template = Config.getLanguageFile(baseName);
        
        BlueJFileReader.translateFile(template, file, translations, Charset.forName("UTF-8"));
    }
    
    /**
     * Brings up a file browser that lets the user select an existing Greenfoot scenario.
     * 
     * @return Returns a File pointing to the scenario directory, or null if none selected.
     */
    public File getScenarioFromFileBrowser(Component parent) {
       return FileChoosers.getScenario(parent);
    }    
    
    public String getNewProjectName(Component parent)
    {
        return FileUtility.getFileName(parent, Config.getString("greenfoot.utilDelegate.newScenario"), Config.getString("pkgmgr.newPkg.buttonLabel"), false, null, true);
    }


    public ClassLoader getCurrentClassLoader() 
    {
        return ExecServer.getCurrentClassLoader();
    }
    
    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    public  String getGreenfootLogoPath()
    {        
        File libDir = Config.getGreenfootLibDir();
        return libDir.getAbsolutePath() + "/imagelib/other/greenfoot.png";        
    }


}
