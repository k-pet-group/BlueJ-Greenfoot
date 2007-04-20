/*
 * DocWeb: Copyright (c) 2007  Sun Microsystems, Inc. All Rights Reserved. 
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
 *                      http://doc.java.sun.com/DocWeb/license.html
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
 */
public class StreamOfThingsWriter {
    private Writer out;
    /**
     * Creates a new instance of StreamOfThingsWriter
     */
    public StreamOfThingsWriter(OutputStream o) {
        if(!(o instanceof BufferedOutputStream))
            o = new BufferedOutputStream(o,1<<13);
        try {
            out = new BufferedWriter(new OutputStreamWriter(o,"UTF-8"),1<<13);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }
    private boolean needsSpace = false;
    public StreamOfThingsWriter write(String s) throws IOException {
        if(needsSpace) {
            writeChar(' ');
            needsSpace = false;
        }
        if(s==null) { writeChar('$'); return this; }
        int len = s.length();
        char escape = len>20 || s.indexOf(' ')>=0 || len==0
                || len>0 && !Character.isJavaIdentifierStart(s.charAt(0))? '"' : ' ';
        if(escape!=' ') writeChar(escape);
        for(int i = 0; i<len; i++) {
            char c = s.charAt(i);
            switch(c) {
                case '\\':
                case '"':
                    writeChar('\\');
                    writeChar(c);
                    break;
                case '\n':
                    writeChar('\\');
                    writeChar('n');
                    break;
                default:
                    writeChar(c);
            }
        }
        if(escape!=' ') { writeChar('"'); needsSpace = false; }
        else needsSpace = true;
        return this;
    }
    public StreamOfThingsWriter write(byte[] b) throws IOException {
        final int len = b.length;
        write(len);
//        System.err.println("   Writing "+len);
        writeChar('#');
        for(int i = 0; i<len; i++) { // *sigh* binary and UTF8 don't mix
            byte t = b[i];
            out.write(hex[(t>>4)&0xF]);
            out.write(hex[t&0xF]);
        }
        return this;
    }
    static final char[] hex = "0123456789ABCDEF".toCharArray();
    public StreamOfThingsWriter write(long l) throws IOException {
        if(needsSpace) { writeChar(' '); needsSpace = false; }
        if(l<0) { writeChar('-'); l = -l; }
        if(l>=10) {
            write(l/10);
            l = l%10;
        }
        writeChar((char)('0'+l));
        needsSpace = true;
        return this;
    }
    public StreamOfThingsWriter writeEOL() throws IOException {
        writeChar('\n');
        needsSpace = false;
        return this;
    }
    private final void writeChar(char c) throws IOException {
        out.write(c);
    }
    public StreamOfThingsWriter writeln(Object... rest) throws IOException {
        for(int i = 0; i<rest.length; i++) {
            Object o = rest[i];
            if(o instanceof Number) write(((Number)o).longValue());
            else if(o instanceof byte[]) write((byte[])o);
            else write(o==null ? "" : o.toString());
        }
        writeEOL();
//        StringBuilder sb = new StringBuilder();
//        sb.append("Sent cmd:");
//        for(int i = 0; i<rest.length; i++) {
//            sb.append(' ');
//            sb.append(rest[i]);
//        }
//        sb.append('\n');
//        System.err.print(sb.toString());
        return this;
    }
    public StreamOfThingsWriter flush() throws IOException {
        out.flush();
        return this;
    }
    public void close() throws IOException {
        out.close();
    }
}
