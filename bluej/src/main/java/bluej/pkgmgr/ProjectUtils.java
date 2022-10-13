/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import bluej.Config;
import bluej.debugger.Debugger;
import bluej.extensions2.SourceType;
import bluej.utility.BlueJFileReader;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Utilities for working with projects.
 * 
 * @author Davin McCall
 */
public class ProjectUtils
{
    /**
     * Check the debugger state is suitable for execution: that is, it is not already
     * executing anything or stuck at a breakpoint.
     * 
     * <P>Returns true if the debugger is currently idle, or false if it is already
     * executing, in which case an error dialog is also displayed.
     * 
     * @param project the project to check execution state for.
     * @param msgWindow the parent window for any error dialogs.
     * @return true if the debugger is currently idle.
     */
    public static boolean checkDebuggerState(Project project, Stage msgWindow)
    {
        Debugger debugger = project.getDebugger();
        if (debugger.getStatus() == Debugger.SUSPENDED)
        {
            DialogManager.showErrorFX(msgWindow, "stuck-at-breakpoint");
            return false;
        }
        else if (debugger.getStatus() == Debugger.RUNNING)
        {
            DialogManager.showErrorFX(msgWindow, "already-executing");
            return false;
        }
        
        return true;
    }
    
    /**
     * Given a project and a path, save a copy of the project to the new path. If there are any
     * errors, display a dialog to the user (and return false).
     * 
     * @param project  The project to save
     * @param newName  The path to save the copy to
     * @param window   The parent window for any error dialogs
     * @return     true if the save was successful, or false otherwise
     */
    public static boolean saveProjectCopy(Project project, File newName, Stage window)
    {
        Path newPath = newName.toPath().toAbsolutePath();
        Path existingPath = project.getProjectDir().toPath().toAbsolutePath();
        // We need to block the case where new project is subdirectory of old path, as that will cause
        // infinite copy.  It's odd but fine if new project is parent of old path, as it won't cause
        // new files in old path, so copy will complete.
        if (newPath.startsWith(existingPath))
        {
            DialogManager.showErrorFX(window, "cannot-save-inside-self");
            return false;
        }

        int result = FileUtility.copyDirectory(project.getProjectDir(),
                newName);

        switch (result)
        {
            case FileUtility.NO_ERROR:
                break;

            case FileUtility.DEST_EXISTS_NOT_DIR:
                DialogManager.showErrorFX(window, "directory-exists-file");
                return false;
            case FileUtility.DEST_EXISTS_NON_EMPTY:
                DialogManager.showErrorFX(window, "directory-exists-non-empty");
                return false;

            case FileUtility.SRC_NOT_DIRECTORY:
            case FileUtility.COPY_ERROR:
                DialogManager.showErrorFX(window, "cannot-save-project");
                return false;
        }
        
        return true;
    }

    /**
     * Creates the skeleton for a new class
     */
    public static void createSkeleton(String className, String superClassName, File file, String templateFileName, String projCharsetName)
        throws IOException
    {
        Dictionary<String, String> translations = new Hashtable<String, String>();
        translations.put("CLASSNAME", className);
        if (superClassName != null)
        {
            translations.put("EXTENDSANDSUPERCLASSNAME", "extends " + superClassName);
            translations.put("EXTENDSSUPERCLASSNAME",  "extends=\"" + superClassName + "\"");
        } 
        else
        {
            translations.put("EXTENDSANDSUPERCLASSNAME", "");
            translations.put("EXTENDSSUPERCLASSNAME",  "");
        }
        String baseName = "greenfoot/templates/" +  templateFileName;
        File template = Config.getLanguageFile(baseName);
        
        if (!template.canRead())
        {
            template = Config.getDefaultLanguageFile(baseName);
        }
        BlueJFileReader.translateFile(template, file, translations, StandardCharsets.UTF_8, selectCharset(projCharsetName));
    }

    /**
     * Creates the duplicate for a class
     */
    public static void duplicate(String originalClassName, String destinationClassName, File originalFile, File destination, SourceType type)
        throws IOException
    {
        Dictionary<String, String> translations = new Hashtable<String, String>();
        translations.put(originalClassName, destinationClassName);
        BlueJFileReader.duplicateFile(originalFile, destination, translations);

        //TODO if the previous line doesn't work properly for Java & Frame files, replace it with the next mechanism
//      if (type.equals(SourceType.Java)) {
//          file = createJavaCopy(destinationClassName, originalClassName, originalFile);
//      }
//      else if (type.equals(SourceType.Frame)) {
//          file = createFrameCopy(destinationClassName, originalClassName, originalFile);
//      }
    }

    private static Charset selectCharset(String projCharsetName)
    {
        Charset projCharset;
        try
        {
            projCharset = Charset.forName(projCharsetName);
        }
        catch (UnsupportedCharsetException uce)
        {
            projCharset = StandardCharsets.UTF_8;
        }
        catch (IllegalCharsetNameException icne)
        {
            projCharset = StandardCharsets.UTF_8;
        }
        return projCharset;
    }
}
