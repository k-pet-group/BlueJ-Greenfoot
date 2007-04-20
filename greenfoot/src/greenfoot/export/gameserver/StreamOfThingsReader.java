/*
 * DocWeb - a doclet implemented as a servlet
 * Copyright (c) 2006 by Sun Microsystems
 *
 * This program is free software.
 *
 * You may redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation.
 * Version 2 of the license should be included with this distribution in
 * the file LICENSE, as well as License.html. If the license is not
 * included with this distribution, you may find a copy at the FSF web
 * site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
 * Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
 * NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
 * OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
 * CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
 * REDISTRIBUTION OF THIS SOFTWARE.
 *
 */

package greenfoot.export.gameserver;

import java.io.*;

/**
 *
 * @author jag
 */
public class StreamOfThingsReader {
    public static final String EOF = uniqueString("EOF");
    public static final String EOL = uniqueString("EOL");
    private final InputStream in;
    private Object current="", previous=EOL, next = null;
    /** Creates a new instance of StreamOfStrings */
    public StreamOfThingsReader(InputStream r) {
//        System.err.println("New Reader");
        if(!(r instanceof BufferedInputStream))
            r = new BufferedInputStream(r,1<<13);
        in = r;
    }
    private static final String uniqueString(String s) {
        return new String(s);
    }
    private /*final*/ StringBuilder sb = new StringBuilder();
    public Object read() throws IOException {
        previous = current;
        if(next!=null) {
//            System.err.println("gotNext "+next);
            current = next;
            next = null;
            return current;
        }
//        final StringBuilder sb = this.sb;
        final InputStream in = this.in;
        sb.setLength(0);
        sb = new StringBuilder();
        int c;
        while((c = readChar())==' ') ;
        if(c<0) return current = EOF;
        if(c=='\n') return current = EOL;
        int quote = ' ';
        if(c=='"' || c=='\'') { quote = c; c = readChar(); }
//        System.err.println("[[startloop]]");
        if(Character.isDigit(c) || c=='-') {
            long v = 0;
            boolean neg = false;
            if(c=='-') {
                c = readChar();
                neg = true;
            }
            do v = v*10+Character.digit(c,10);
            while(Character.isDigit(c = readChar()));
            if(neg) v = -v;
            if(c=='#') {
                byte[] b = new byte[(int)v];
//                System.err.println("   Reading "+v);
//                for(int i = 0; i<v; i++) b[i] = (byte) readChar();
                for(int i = 0; i<v; i++) {
                    b[i] = (byte)((readHex()<<4)|readHex());
                }
//                System.err.println("\tdone Reading");
                return b;
            }
            if(c=='\n') next = EOL;
            if(c<0) next = EOF;
            return Long.valueOf(v);
        }
        while(c>=0 && c!=quote && c!='\n') {
            if(c == '\\')
                switch(c = readChar()) {
                    case 'n': c = '\n'; break;
                    case 'r': c = '\r'; break;
                    case 'b': c = '\b'; break;
                    case '0': c = 0; break;
                    case 'u': c = (Character.digit(readChar(),16)<<12) |
                                  (Character.digit(readChar(),16)<<8) |
                                  (Character.digit(readChar(),16)<<4) |
                                   Character.digit(readChar(),16);
                              break;
                }
            sb.append((char)c);
//            System.err.println("[[part "+sb+"]]");
            c = readChar();
        }
        if(c=='\n') next = EOL;
        else if(c<0) next = EOF;
//        System.err.println("Read <"+sb+">");
        return current = sb.toString().intern();
    }
    private final int readChar() throws IOException {
        int c = in.read();
//        System.err.println(c=='\n' ? "<newline>" : "<Read "+((char)c)+">");
        return c;
    }
    private final int readHex() throws IOException {
        int c;
        while((c=in.read())>=0 && c<=' ');
        return '0'<=c && c<='9' ? c-'0' : 'A'<=c && c<='F' ? c+(10-'A') : -1;
    }
    public String toString() { return String.valueOf(current); }
    public Object getPrevious() { return previous; }
    public Object getCurrent() { return current; }
    public Object peek() throws IOException {
        if(next==null) {
            Object prev = previous;
            next = read();
            current = previous;
            previous = prev;
        }
        return next;
    }
    public void close() throws IOException {
        in.close();
    }
}
