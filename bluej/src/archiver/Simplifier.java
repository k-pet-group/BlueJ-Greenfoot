/*
 * @(#)Simplifier.java	1.1 99/09/27
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
package archiver;

import java.util.*; 

/**
 * @version 1.1 09/27/99
 * @author Philip Milne
 */

public class Simplifier {     
    private Hashtable references; 
    private boolean changed;  

    private void addReference(Object exp, int direction) { 
        Integer N = (Integer)references.get(exp); 
        int n = (N == null) ? 0 : N.intValue(); 
        references.put(exp, new Integer(n + direction));
    }
    
    private void mark(Object exp) { 
        changed = true; 
        mark(exp, true); 
    }
    
    private void unmark(Object exp) { 
        changed = true; 
        mark(exp, false); 
    }
    
    private void mark(Object exp, boolean mark) { 
        if (!(exp instanceof Object[])) { 
            if (exp instanceof String) { 
                addReference(exp, mark ? 1 : -1); 
            }
            return; 
        } 
        Object[] a = (Object[])exp;
        for(int i = 0; i < a.length; i++) { 
            mark(a[i], mark); 
        }
    }
        
    public Object simplify(Object exp) { 
        //System.out.println("Pruning ..."); 
        //System.out.println(ArrayEnumerator.enumerationToString2(exp));
        references = new Hashtable(); 
        mark(exp); 
        while(changed == true) { 
            changed = false; 
            exp = prune(exp); 
        }
        return exp; 
    }
    
    private int refCount(Object exp) { 
        Object refs = references.get(exp); 
        return (refs instanceof Integer) ? ((Integer)refs).intValue() : 0; 
    }
    
    private Object prune(Object exp) { 
        if (!(exp instanceof Object[])) { 
            return exp; 
        } 
        Object[] a = (Object[])exp;
        Object f = (a.length < 1) ? null : a[0]; 
        if (f == "=") { 
            if (!a[1].getClass().isArray()) { 
                int refs = refCount(a[1]); 
                if (refs <= 1) { 
                    // System.out.println("Pruning: " + a[1]); 
                    unmark(a); 
                    mark(a[2]); 
                    return a[2]; 
                } 
            }
        }
        if (f == "block") {
            if (a.length == 3) { 
                unmark(a); 
                mark(a[1]); 
                return prune(a[1]); 
            } 
            Vector v = new Vector(); 
            v.addElement(a[0]); 
            for(int i = 1; i < a.length - 1; i++) { 
                Object child = prune(a[i]); 
                if (child != null && child.getClass().isArray() && 
                        (((Object[])child)[0] == "=" || 
                         ((Object[])child)[0] == "block" || 
                         (((Object[])child)[0] == "." && 
                          (((Object[])child)[1].getClass().isArray() || 
                           ((Object[])child)[2].getClass().isArray())))) {
                    v.addElement(child); 
                } 
                else { 
                    // System.out.println("Pruned: " + ArrayEnumerator.enumerationToString(child));
                    unmark(child); 
                }
            } 
            v.addElement(a[a.length - 1]); 
            return ArrayEnumerator.enumerationToArray(v.elements());
        }
        for(int i = 0; i < a.length; i++) { 
            a[i] = prune(a[i]); 
        } 
        return a; 
    }
}

