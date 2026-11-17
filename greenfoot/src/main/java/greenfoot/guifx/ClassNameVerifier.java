/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2016,2019,2022,2026  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import bluej.Config;
import bluej.extensions2.SourceType;
import bluej.parser.kotlin.KotlinParserUtils;
import bluej.pkgmgr.Package;
import bluej.utility.JavaNames;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Validates a new class name for the selected language and checks for existing
 * targets or files. Creation checks deliberately do not govern source discovery.
 */
@OnThread(Tag.FXPlatform)
public class ClassNameVerifier
{
    private final Package pkg;
    private String message;
    private final String classExists = Config.getString("newclass.dialog.err.classExists");
    private final StringProperty textProperty;
    private final ReadOnlyObjectProperty<SourceType> sourceTypeProperty;

    /**
     * Create a verifier for the dialog's current name and language.
     *
     * @param pkg package to receive the new class
     * @param textProperty proposed class name
     * @param sourceTypeProperty selected source language
     */
    public ClassNameVerifier(Package pkg, StringProperty textProperty, ReadOnlyObjectProperty<SourceType> sourceTypeProperty)
    {
        this.pkg = pkg;
        this.textProperty = textProperty;
        this.sourceTypeProperty = sourceTypeProperty;
        checkValidity();
    }

    /**
     * Get the message from the most recent validity check.
     * @return an error message, or empty text for a valid or blank name
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Check the name and language currently selected by the dialog.
     * @return true if the new class name is valid and available
     */
    public boolean checkValidity()
    {
        String className = textProperty.get();
        SourceType sourceType = sourceTypeProperty.get();
        boolean legal = isLegalClassName(className, sourceType) && !isWindowsRestrictedWord(className);
        boolean exists = legal && classNameExists(pkg, className);
        boolean valid = legal && !exists;
        Properties localProperties = new Properties();
        localProperties.put("LANGUAGE", sourceType == null ? "" : sourceType.toString());
        message = valid || className.isEmpty() ? "" : exists ? classExists
            : Config.getString("newclass.dialog.err.classNameIllegal", null, localProperties, false);
        return valid;
    }

    @OnThread(Tag.Any)
    static boolean isLegalClassName(String name, SourceType language)
    {
        if (name == null || language == null || language == SourceType.NONE
            || name.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        // Keep existing JVM-facing class-name restrictions as well as Kotlin's lexical rules.
        return JavaNames.isIdentifier(name)
            && (language != SourceType.Kotlin || KotlinParserUtils.isCreationIdentifier(name));
    }

    @OnThread(Tag.Any)
    static boolean isWindowsRestrictedWord(String name)
    {
        return List.of("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5",
                "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9")
            .contains(name.toUpperCase(Locale.ROOT));
    }

    static boolean classNameExists(Package pkg, String name)
    {
        return hasNameCollision(pkg.getPath(), name, pkg.getTarget(name) != null);
    }

    // Include untracked files, not just targets: creating a source must not overwrite
    // another language or make an existing class disappear on the next project load.
    @OnThread(Tag.Any)
    static boolean hasNameCollision(File directory, String name, boolean targetExists)
    {
        if (targetExists || name.equals("Actor") || name.equals("World")) {
            return true;
        }
        for (String extension : List.of("java", "stride", "kt", "class")) {
            if (Files.exists(new File(directory, name + "." + extension).toPath(), LinkOption.NOFOLLOW_LINKS)) {
                return true;
            }
        }
        return false;
    }
}
