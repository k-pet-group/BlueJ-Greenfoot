/*
 * @(#)DescriptorComparator.java	1.1 99/09/23
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
package beantest.util;

import java.beans.FeatureDescriptor;

import java.util.Comparator;

/**
 * Comparator used to compare java.beans.FeatureDescriptor objects.
 * The Strings returned from getDisplayName are used in the comparison.
 *
 * @version 1.1 09/23/99
 * @author  Mark Davidson
 */
public class DescriptorComparator implements Comparator  {

    /**
     * Compares two FeatureDescriptor objects
     */
    public int compare(Object o1, Object o2)  {
        FeatureDescriptor f1 = (FeatureDescriptor)o1;
        FeatureDescriptor f2 = (FeatureDescriptor)o2;

        return f1.getDisplayName().compareTo(f2.getDisplayName());
    }
}
