/*
 * @(#)AbstractInputStream.java	1.2 99/11/09
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
 * @version 1.2 11/09/99
 * @author Philip Milne
 */

public abstract class AbstractInputStream extends DataInputStream 
implements ObjectInput {

    private Object currentToken;         
    private Vector nextTokens; 
    private StreamTokenizer tokenizer; 
                
    public AbstractInputStream(InputStream in) { 
        super(in); 
        nextTokens = new Vector(); 
        Reader r = new InputStreamReader(in);
        Reader br = new BufferedReader(r); 
        tokenizer = new StreamTokenizer(br);
        // tokenizer.eolIsSignificant(true); 
        tokenizer.ordinaryChar('.'); 
        tokenizer.ordinaryChar(','); 
        tokenizer.ordinaryChar(';'); 
        tokenizer.ordinaryChar(':'); 
        tokenizer.ordinaryChar('['); 
        tokenizer.ordinaryChar(']'); 
        tokenizer.ordinaryChar('{'); 
        tokenizer.ordinaryChar('}'); 
        tokenizer.ordinaryChar('<'); 
        tokenizer.ordinaryChar('>'); 
        tokenizer.ordinaryChar('/'); 
        //tokenizer.whitespaceChars(',', ','); 
        tokenizer.wordChars('_', '_'); 
        tokenizer.wordChars('$', '$');         
    } 
                
    // Find the least general type which can represent this 
    // number without loss of precision.
    private Object leastGeneralRepresentation(double d) { 
        byte byteValue = (byte)d; 
        if (byteValue == d) { 
            return new Byte(byteValue); 
        }
        short shortValue = (short)d; 
        if (shortValue == d) { 
            return new Short(shortValue); 
        }
        /*
        char charValue = (char)d; 
        if (charValue == d) { 
            return new Character(charValue); 
        }
        */
        int intValue = (int)d; 
        if (intValue == d) { 
            return new Integer(intValue); 
        }
        long longValue = (long)d; 
        if (longValue == d) { 
            return new Long(longValue); 
        }
        float floatValue = (float)d; 
        if (floatValue == d) { 
            return new Float(floatValue); 
        }
        return new Double(d);
    }

    private Object reallyReadToken() { 
        int tok = StreamTokenizer.TT_EOF; 
        try {
            tok = tokenizer.nextToken(); 
        }
        catch (Throwable e) {};
        // System.out.println("Read: " + nextToken + "("+(char)nextToken+")"+": " + tokenizer.sval); 
        int tokenType = tokenizer.ttype; 
        switch(tokenType) {
        case StreamTokenizer.TT_NUMBER: 
            return leastGeneralRepresentation(tokenizer.nval); 
        case StreamTokenizer.TT_WORD:
            return tokenizer.sval.intern();
        case '"': 
            return new Object[]{"\"", new String(tokenizer.sval)}; 
        case StreamTokenizer.TT_EOF: 
            return "EOF";
        default: 
            return String.valueOf((char)tok).intern();
        }
    } 

    protected Object readToken() { 
        if (nextTokens.size() == 0) { 
            currentToken = reallyReadToken(); 
        }
        else { 
            currentToken = nextTokens.elementAt(0); 
            nextTokens.removeElementAt(0); 
        }
        //   System.out.println(currentToken); 
        return currentToken; 
    }


    protected Object peek() { 
        return peek(0); 
    }

    protected Object getCurrentToken() { 
        return currentToken; 
    }

    protected void emptyLookAheadQueue() { 
        nextTokens.removeAllElements(); 
    }

    protected Object peek(int n) { 
        while (n >= nextTokens.size()) { 
            nextTokens.addElement(reallyReadToken()); 
        }
        return nextTokens.elementAt(n); 
    }
}





