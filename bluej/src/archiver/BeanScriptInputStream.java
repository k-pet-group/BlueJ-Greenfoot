/*
 * @(#)BeanScriptInputStream.java	1.3 99/11/09
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

import java.io.*; 
import java.util.*; 

/**
 * @version 1.3 11/09/99
 * @author Philip Milne
 */

public class BeanScriptInputStream extends AbstractInputStream {

    private Evaluator environment; 
                
    public BeanScriptInputStream(InputStream in) { 
        super(in);  
        environment = new Evaluator();
    } 
                
    public Object readObject() throws IOException { 
        emptyLookAheadQueue();
        Object exp = parse("="); // The operator above ";".
        // System.out.println(ArrayEnumerator.enumerationToString(exp)); 
        Object result = null; 
        try { result = environment.evaluate(exp); } catch (Exception e) { e.printStackTrace(); }; 
        // System.out.println(ArrayEnumerator.enumerationToString(result)); 
        return result; 
    }

    private boolean prefixOperator(Object op1) { 
        return (op1 == "[" || op1 == "{");
    }
        
    /*
    private boolean postfixOperator(Object op1) { 
      return (op1 == ")");}
    }
    */

   /* The "let" and ":" tokens are just here so 
      that we can choose to use the "=" character either for 
      assignment or equality in the future. There is no 
      equality operator in BeansScript now, so "=" actually 
      has the same meaning as the ":=" dipthong anyway. 
      The "let" and ":" tokens do nothing and are 
      discarded by this parser.
    */
    private boolean nullToken(Object token) { 
        return (token == ":" || token == "let");    
    }             
    
    protected Object readToken() { 
        Object token = super.readToken(); 
        return (nullToken(token)) ? super.readToken() : token;
    }

    protected Object peek() { 
        Object token = super.peek(); 
        return (nullToken(token)) ? super.peek(1) : token;
    }
    
    protected Object getCurrentToken() { 
        Object token = super.getCurrentToken(); 
        return (nullToken(token)) ? super.peek() : token;
    }
    
    private Object[] infixOperators = new Object[]{"=", ".", "("}; 
        
    private int precedence(Object op) { 
        if (op == "EOF") { 
            return -2; 
        }
        for(int i = 0; i < infixOperators.length; i++) { 
            if (infixOperators[i] == op) {
                return i; 
            }
        }
        return -1; 
    }
                
    private boolean leftAssociate(Object op1, Object op2) { 
        // Right associating operators.
        if (op1 == op2) { 
            return (op2 != "="); 
        } 
        // Otherwise use precedence. 
        int p1 = precedence(op1); 
        int p2 = precedence(op2); 
        return p1 > p2;
    }
      
    private Object parseList(final Object firstItem, final String separator, final String close) { 
        return ArrayEnumerator.enumerationToArray(new Enumeration() { 
	    boolean readFirstItem = false; 
            public boolean hasMoreElements() { 
            	if (readFirstItem == false) { 
		    return true; 
               	}
                if (peek() == separator) { 
                    readToken();
                } 
                if (peek() == close) { 
                    readToken();
                    return false; 
                } 
                return true;
            }
            
            public Object nextElement() { 
            	if (readFirstItem == false) { 
		    readFirstItem = true; 
		    return firstItem; 
            	}
                return parse("=");
            }
        });
    }    
            
    protected Object parse(Object previousOp) { 
        Object result = readToken(); 
        while(prefixOperator(getCurrentToken()) || !leftAssociate(previousOp, peek())) { 
            if (prefixOperator(getCurrentToken())) { 
                if (getCurrentToken() == "[") { 
                    result = parseList("array", ",", "]"); 
                }
                else if (getCurrentToken() == "{") { 
                    result = parseList("block", ";", "}"); 
                }
            }
            else {
                Object nextOp = readToken(); 
                if (nextOp == "(") { 
                    result = parseList(result, ",", ")"); 
                }
                else { 
                    result = new Object[]{nextOp, result, parse(nextOp)}; 
                }
            }
        }
        return result;
    }
}


