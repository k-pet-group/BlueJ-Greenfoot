/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.util.*;
import java.io.*;

/**
 * A properties object which outputs its entries in sorted order
 * (allowing the resulting files to exist in CVS repositories without
 *  so much clashing)
 *
 * @author  Andrew Patterson
 * @version $Id: SortedProperties.java 6215 2009-03-30 13:28:25Z polle $
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
