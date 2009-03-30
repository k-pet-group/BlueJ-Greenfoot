/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import greenfoot.core.GPackage;
import greenfoot.event.ValidityEvent;
import greenfoot.event.ValidityListener;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;

import bluej.Config;
import bluej.utility.JavaNames;

/**
 * Class that verifies a class name typed into a TextField. It checks that the
 * class name is a legal name of a Java class and that the class does not
 * already exist. It is also possible to listen for changes in the validity of
 * the TextField.
 * 
 * @author Poul Henriksen
 */
public class ClassNameVerifier extends InputVerifier
    implements DocumentListener
{
    private EventListenerList listenerList = new EventListenerList();
    private String classExists = Config.getString("newclass.dialog.err.classExists");
    private String illegalClassName = Config.getString("newclass.dialog.err.classNameIllegal");
    
    /** Is the text field currently in a valid state? */
    private boolean valid = false;

    /**
     * Flag used to indicate whether it is the first check. If it is, we should
     * fire an event no matter what.
     */
    private boolean firstCheck = true;

    /** The package the class will be added to */
    private GPackage pkg;

    /** The text field to listen for DocumentEvents on */
    private JTextField textField;

    /**
     * Create new class name verifier. This will add the appropate listeners to
     * the textField.
     * <p>
     * If using a Cancel button it might be useful to set
     * {@link JComponent.#setVerifyInputWhenFocusTarget(boolean)} to false on
     * the cancel button.
     * 
     * @param pkg
     *            Package containing the user classes for which to check
     *            against.
     */
    public ClassNameVerifier(JTextField textField, GPackage pkg)
    {
        this.pkg = pkg;
        this.textField = textField;
        textField.getDocument().addDocumentListener(this);
        textField.setInputVerifier(this);
        textField.setVerifyInputWhenFocusTarget(true);
    }

    /**
     * Verifies that the TextField doesn't contain the name of an existing
     * class.
     * 
     * @param jTextField
     *            The component must be of the type JTextField.
     */
    @Override
    public boolean verify(JComponent textField)
    {
        if(this.textField != textField) {
            System.err.println("Illegal textfield verified.");
            return false;
        }
        String className = this.textField.getText();
        return JavaNames.isIdentifier(className) && !classNameExist(className) && !isGreenfootClassName(className);
    }

    @Override
    public boolean shouldYieldFocus(JComponent jTextField)
    {
        return checkValidity();
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
        return pkg.getClass(className) != null;
    }

    /**
     * Checks whether the text field contains a valid class name. It will send
     * the relevant events to notify ValidityListeners.
     * 
     * @return true, if it is valid.
     */
    private boolean checkValidity()
    {
        boolean inputOK = verify(textField);
        if (inputOK != valid || firstCheck) {
            firstCheck = false;
            valid = inputOK;
            if (valid) {
                fireValidEvent(new ValidityEvent(textField, "All OK"));
            }
            else {
                String className = textField.getText();
                ValidityEvent validityEvent = null;
                if (classNameExist(className) || isGreenfootClassName(className)) {
                    validityEvent = new ValidityEvent(textField, classExists);
                }
                else {
                    validityEvent = new ValidityEvent(textField, illegalClassName);
                }
                fireInvalidEvent(validityEvent);
            }
        }
        return inputOK;
    }

    /**
     * Adds a listener to receive events when the validity of the component
     * changes.
     */
    public void addValidityListener(ValidityListener l)
    {
        listenerList.add(ValidityListener.class, l);
    }

    public void removeValidityListner(ValidityListener l)
    {
        listenerList.remove(ValidityListener.class, l);
    }

    private void fireValidEvent(ValidityEvent event)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ValidityListener.class) {
                ((ValidityListener) listeners[i + 1]).changedToValid(event);
            }
        }
    }

    private void fireInvalidEvent(ValidityEvent event)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ValidityListener.class) {
                ((ValidityListener) listeners[i + 1]).changedToInvalid(event);
            }
        }
    }

    private void change()
    {
        checkValidity();
    }

    public void changedUpdate(DocumentEvent e)
    {
        // Nothing to do
    }

    public void insertUpdate(DocumentEvent e)
    {
        change();
    }

    public void removeUpdate(DocumentEvent e)
    {
        change();
    }
}
