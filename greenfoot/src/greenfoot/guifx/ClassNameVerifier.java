/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2016  Poul Henriksen and Michael Kolling
 
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
import bluej.extensions.SourceType;
import bluej.pkgmgr.Package;
import bluej.utility.JavaNames;

import java.util.Properties;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;

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
    private SourceType sourceType;
    private String classExists = Config.getString("newclass.dialog.err.classExists");
    private String illegalClassName;
    private Properties localProperties = new Properties();
    
    /** Is the text field currently in a valid state? */
    private boolean valid = false;

    /**
     * Flag used to indicate whether it is the first check. If it is, we should
     * fire an event no matter what.
     */
    private boolean firstCheck = true;

    /** The package the class will be added to */
    private Package pkg;

    /** The text field to listen for DocumentEvents on */
    private TextField textField;

    private BooleanProperty validity = new SimpleBooleanProperty(false);
    private String message;

    /**
     * Create new class name verifier. This will add the appropate listeners to
     * the textField.
     * <p>
     * If using a Cancel button it might be useful to set
     * {@link JComponent.#setVerifyInputWhenFocusTarget(boolean)} to false on
     * the cancel button.
     * 
     * @param pkg Package containing the user classes for which to check against.
     */
    public ClassNameVerifier(TextField textField, Package pkg, SourceType sourceType)
    {
        this.pkg = pkg;
        this.textField = textField;
        this.sourceType = sourceType;
        buildTheErrorMessage();
    }

    public void change(SourceType sourceType)
    {
        firstCheck = true;
        this.sourceType = sourceType;
        checkValidity();
    }

    public BooleanProperty validityProperty()
    {
        return validity;
    }

    public String getMessage()
    {
        return message;
    }


    private void buildTheErrorMessage()
    {
        localProperties.put("LANGUAGE", sourceType!= null ? sourceType.toString() : "");
        illegalClassName = Config.getString("newclass.dialog.err.classNameIllegal", null,  localProperties);
    }

    /**
     * Checks whether the text field contains a valid class name. It will send
     * the relevant events to notify ValidityListeners.
     * Verifies that the TextField doesn't contain the name of an existing class.
     */
    private void checkValidity()
    {
        String className = textField.getText();
        boolean inputOK = JavaNames.isIdentifier(className) && !classNameExist(className) && !isGreenfootClassName(className);
        if (inputOK != valid || firstCheck) {
            firstCheck = false;
            valid = inputOK;
            if (valid) {
                validity.set(true);
                message = "All OK";
            }
            else
            {
                validity.set(false);
                if (classNameExist(className) || isGreenfootClassName(className))
                {
                    message = classExists;
                }
                else
                {
                    message = illegalClassName;
                }
            }
        }
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
