/*
 * @(#)SwingEditorSupport.java	1.2 99/10/27
 *
 * Copyright 1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
package javax.swing.beaninfo;

import java.awt.Component;
import java.awt.Dimension;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditorSupport;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Base class of all Swing based property editors.
 *
 * @version 1.2 10/27/99
 * @author  Tom Santos
 * @author  Mark Davidson
 */
public class SwingEditorSupport extends PropertyEditorSupport {

    /** 
     * Component which holds the editor. Subclasses are responsible for
     * instantiating this panel.
     */
    protected JPanel panel;
    
    protected static final Dimension MEDIUM_DIMENSION = new Dimension(120,20);
    protected static final Dimension SMALL_DIMENSION = new Dimension(50,20);

    /** 
     * Returns the panel responsible for rendering the PropertyEditor.
     */
    public Component getCustomEditor() {
        return panel;
    }

    public boolean supportsCustomEditor() {
        return true;
    }
    
    // layout stuff
    protected final void setAlignment(JComponent c){
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.setAlignmentY(Component.CENTER_ALIGNMENT);
    }
    
    /** 
     * For property editors that must be initialized with values from
     * the property descriptor.
     */
    public void init(PropertyDescriptor descriptor)  {  
    }
}
