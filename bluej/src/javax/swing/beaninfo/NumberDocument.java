/*
 * @(#)NumberDocument.java	1.1 99/09/23
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

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * A text document which will reject any characters that are not
 * digits.
 *
 * @version 1.1 09/23/99
 * @author  Mark Davidson
 */
public class NumberDocument extends PlainDocument  {
    public void insertString(int offs, String str, AttributeSet atts) 
            throws BadLocationException  {
        if (!Character.isDigit(str.charAt(0)))  {
            return;
        }
        super.insertString(offs, str, atts);
    }
}

