package bluej.utility;

import java.util.*;
import java.io.*;

/**
 * A properties object which outputs its entries in sorted order
 * (allowing the resulting files to exist in CVS repositories without
 *  so much clashing)
 *
 * @author Andrew Patterson
 * @version $Id: SortedProperties.java 285 1999-11-25 05:29:25Z ajp $
 */
public class SortedProperties extends Properties
{
    private static final String keyValueSeparators = "=: \t\r\n\f";
    private static final String strictKeyValueSeparators = "=:";
    private static final String specialSaveChars = "=: \t\r\n\f#!";
    private static final String whiteSpaceChars = " \t\r\n\f";

    /**
     * These functions from the JDK1.2 source (I'm sure Sun won't mind)
     *
     * Copyright 1995-1998 by Sun Microsystems, Inc.,
     * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
     * All rights reserved.
     *
     * Converts unicodes to encoded \\uxxxx
     * and writes out any of the characters in specialSaveChars
     * with a preceding slash
     */
    private String saveConvert(String theString)
    {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len*2);

        for(int x=0; x<len; ) {
            aChar = theString.charAt(x++);
            switch(aChar) {
                case '\\':outBuffer.append('\\'); outBuffer.append('\\');
                          continue;
                case '\t':outBuffer.append('\\'); outBuffer.append('t');
                          continue;
                case '\n':outBuffer.append('\\'); outBuffer.append('n');
                          continue;
                case '\r':outBuffer.append('\\'); outBuffer.append('r');
                          continue;
                case '\f':outBuffer.append('\\'); outBuffer.append('f');
                          continue;
                default:
                    if ((aChar < 20) || (aChar > 127)) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >> 8) & 0xF));
                        outBuffer.append(toHex((aChar >> 4) & 0xF));
                        outBuffer.append(toHex((aChar >> 0) & 0xF));
                    }
                    else {
                        if (specialSaveChars.indexOf(aChar) != -1)
                            outBuffer.append('\\');
                        outBuffer.append(aChar);
                    }
            }

        }
        return outBuffer.toString();
    }

    /**
     * Convert a nibble to a hex character
     * @param   nibble  the nibble to convert.
     */
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
    
    
    public void store(OutputStream out, String header) throws IOException
    {
        BufferedWriter awriter;
        awriter = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
        if (header != null) {
            awriter.write("#" + header);
            awriter.newLine();
        }

        TreeMap tm = new TreeMap(this);
        
        Iterator it = tm.entrySet().iterator();
        
        while(it.hasNext())
        {
            Map.Entry mapEntry = (Map.Entry) it.next();
            
            String key = saveConvert((String)mapEntry.getKey());
            String val = saveConvert((String)mapEntry.getValue());

            awriter.write(key + "=" + val);
            awriter.newLine();
        }
        awriter.flush();        
        
    }

}
