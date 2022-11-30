/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2016,2019,2022  Poul Henriksen and Michael Kolling
 
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

import bluej.Config;
import bluej.extensions2.SourceType;
import bluej.pkgmgr.Package;
import bluej.utility.JavaNames;

import java.util.Properties;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;

/**
 * Class that verifies a class name typed into a TextField. It checks that the
 * class name is a legal name of a Java class and that the class does not
 * already exist. It is also possible to listen for changes in the validity of
 * the TextField.
 * 
 * @author Poul Henriksen
 * @author Amjad Altadmri
 */
public class ClassNameVerifier
{
    /** The package the class will be added to */
    private Package pkg;

    private String message;
    private String illegalClassName;
    private Properties localProperties = new Properties();
    private final String classExists = Config.getString("newclass.dialog.err.classExists");

    /** The text field to listen for DocumentEvents on */
    private final StringProperty textProperty;
    private final ReadOnlyObjectProperty<SourceType> sourceTypeProperty;

    /**
     * Create new class name verifier. This will add the appropriate listeners to
     * the textField.
     * 
     * @param pkg Package containing the user classes for which to check against.
     */
    public ClassNameVerifier(Package pkg, StringProperty textProperty, ReadOnlyObjectProperty<SourceType> sourceTypeProperty)
    {
        this.pkg = pkg;
        this.textProperty = textProperty;
        this.sourceTypeProperty = sourceTypeProperty;
        checkValidity();
    }

    public String getMessage()
    {
        return message;
    }

    private void buildTheErrorMessage()
    {
        SourceType sourceType = sourceTypeProperty.get();
        localProperties.put("LANGUAGE", sourceType !=null ? sourceType.toString() : "");
        illegalClassName = Config.getString("newclass.dialog.err.classNameIllegal", null,  localProperties, false);
    }

    /**
     * Checks whether the text field contains a valid class name. It will send
     * the relevant events to notify ValidityListeners.
     * Verifies that the TextField doesn't contain the name of an existing class.
     *
     * @return True if the class name is valid, false otherwise.
     */
    public boolean checkValidity()
    {
        buildTheErrorMessage();
        String className = textProperty.get();
        boolean valid = !className.isEmpty() && JavaNames.isIdentifier(className) && !classNameExist(className) && !isGreenfootClassName(className);
        message = valid || className.isEmpty() ? "" :
                (classNameExist(className) || isGreenfootClassName(className) ? classExists : illegalClassName);
        return valid;
    }

    private boolean isGreenfootClassName(String className)
    {
        return className.equals("Actor") || className.equals("World");
    }
    
    /**
     * Returns true if a class with the given name already exists.
     * 
     * @param className
     * @return
     */
    private boolean classNameExist(String className)
    {
        return pkg.getTarget(className) != null;
    }
}
