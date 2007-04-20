/*
 * MyGame: Copyright (c) 2007  Sun Microsystems, Inc. All Rights Reserved. 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER 
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. 
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.  A copy is included at
 *                      http://doc.java.sun.com/MyGame/license.html
 * 
 * You should have received a copy of the GNU General Public 
 * License version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 or visit www.sun.com if you need additional information or have any
 * questions.
 */


package greenfoot.export.gameserver;

import java.io.*;

/**
 * @author James Gosling
 * @created July 31, 2006
 */
public class PrefixedWriter extends Writer {
    private final Writer out;
    private String prefix = ": ";
    public PrefixedWriter(Writer o) {
        out = o;
    }
    public void setPrefix(String p) {
        prefix = p;
    }
    private void flushPrefix() throws IOException {
        if(prefix!=null) {
            out.write(prefix);
            prefix = null;
        }
    }
    public void write(char[] c, int i, int len) throws IOException {
        if(len>0) {
            flushPrefix();
            out.write(c,i,len);
        }
    }
    public void write(String s, int i, int len) throws IOException {
        if(len>0) {
            flushPrefix();
            out.write(s);
        }
    }
    public void write(int c) throws IOException {
        flushPrefix();
        out.write(c);
    }
    public void flush() throws IOException { out.flush(); }
    public void close() throws IOException { out.close(); }
    
}
