/*
 * @(#)BeanInfoFactory.java	1.1 99/11/09
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

package bluej.guibuilder;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;

import java.util.Hashtable;

/**
 * A wrapper for the Introspector to generate and return instances
 * of BeanInfos. Creating a BeanInfo with the Introspector is a
 * really expensive operation. Since a BeanInfo is immutable for
 * each class, it makes sense to share instances of BeanInfo's
 * throughout the application.
 *
 * @version 1.1 11/09/99
 * @author  Mark Davidson
 */
public class BeanInfoFactory  {

    private static Hashtable infos = new Hashtable();

    /**
     * Retrieves the BeanInfo for a Class
     */
    public static BeanInfo getBeanInfo(Class cls)  {
        BeanInfo beanInfo = (BeanInfo)infos.get(cls);

        if (beanInfo == null)  {
            try {
                beanInfo = Introspector.getBeanInfo(cls);
                infos.put(cls, beanInfo);
            } catch (IntrospectionException ex) {
                // XXX - should handle this better.
                ex.printStackTrace();
            }
        }
        return beanInfo;
    }
}
