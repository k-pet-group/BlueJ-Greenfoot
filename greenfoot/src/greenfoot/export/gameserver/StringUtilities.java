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

/**
 * @author James Gosling
 * @created August 17, 2006
 */
public class StringUtilities {
    private StringUtilities() {
    }
    public static String valueOf(int i) {
        if(i<-coff || i>=(clen-coff))
            return String.valueOf(i);
        String s = vcache[i+coff];
        if(s==null) vcache[i+coff] = s = String.valueOf(i);
        return s;
    }
    public static String valueOf(long l) {
        int i = (int) l;
        return l==i ? valueOf(i) : String.valueOf(l);
    }
    public static String clean(String t) {
        if(t!=null) {
            t = t.trim();
            if(t.length()==0) t = null;
        }
        return t;
    }
    public static int lineCount(final String s) {
        int limit = s.length()-1;
        int pos = 0;
        int lpos = 0;
        int nl = 1;
        while(pos<limit) {
            pos = s.indexOf('\n', pos);
            if(pos<0 || pos>=limit) {
                nl+= (limit-lpos)/60;
                break;
            }
            nl += (pos-lpos)/60 + 1;
            pos++;
            lpos = pos;
        }
        return nl;
    }
    /** compare two strings for equality, ignoring spaces */
    public static boolean nsequals(final String s1, final String s2) {
        if(s1==null) return s2==null;
        if(s2==null) return false;
        final int l1 = s1.length();
        final int l2 = s2.length();
        int p1 = 0;
        int p2 = 0;
        while(p1<l1 && p2<l2) {
            char c1 = s1.charAt(p1);
            if(c1<=' ') { p1++; continue; }
            char c2 = s2.charAt(p2);
            if(c2<=' ') { p2++; continue; }
            if(c1!=c2)
                return false;
            p1++;
            p2++;
        }
        while(p1<l1 && s1.charAt(p1)<=' ') p1++;
        while(p2<l2 && s2.charAt(p2)<=' ') p2++;
        return p1>=l1 && p2>=l2;
    }
    private static final int coff = 10;
    private static final int clen = 200;
    private static final String[] vcache = new String[clen];
    /** If s is a string that contains only 8 bit characters that can be
     * decoded as a utf string, return the decoded string, otherwise return
     * the original. This is primarily used in situations where strings like
     * URL parameters sometimes become strings without being properly
     * decoded as UTF */
    public static String unUTF(String s) {
        if(s!=null) {
            int limit = s.length();
            char c;
            simpleTest: { // do simple cases quickly
                for(int i = 0; i<limit; i++)
                    if((c = s.charAt(i))>0x7F) 
                        if(c>0xFF) break; // to big, not UTF-8
                        else break simpleTest; // maybe UTF-8
                return s; // not UTF=8
            }
            StringBuilder sb = new StringBuilder();
            int dc = 0;
            int expected = 0;
            for(int i = 0; i<limit; i++) {
                c = s.charAt(i);
                if(expected>0) {
                    if((c&0300)!=0200) return s; // not continuation
                    dc = (dc<<6)|c&077;
                    if(--expected==0) sb.append((char)dc);
                }
                else if(c<=0x7F) // one byte
                    sb.append(c);
                else if((c&0340)==0300) {  // two-byte
                    dc = c&037;
                    expected = 1;
                } else if((c&0360)==0340) { // three-byte
                    dc = c&017;
                    expected = 2;
                } else if((c&0370)==0360) { // four-byte
                    dc = c&07;
                    expected = 3;
                } else return s; // Not UTF-8
            }
            return sb.toString();
        }
        return s;
    }
    public static String camelCase(String s) {
        int pfxc = 0;
        int limit = s.length();
        while(pfxc<limit && isOK(s.charAt(pfxc))) pfxc++;
        if(pfxc==limit) return s;
        StringBuilder sb = new StringBuilder();
        sb.append(s,0,pfxc);
        boolean needCap = true;
        while(pfxc<limit) {
            char c = s.charAt(pfxc++);
            if(isOK(c)) {
                if(needCap && Character.isLowerCase(c))
                    c = Character.toUpperCase(c);
                needCap = false;
                sb.append(c);
            } else needCap = true;
        }
        return sb.toString();
    }

    public int getClen() {
        return clen;
    }
    public static String StringValueOf(Object o, String dflt) {
        if(o==null) return dflt;
        String s = o.toString();
        if(s==null) return dflt;
        s = s.trim();
        return s.length()<=0 ? dflt : s;
    }
    public static String StringValueOf(Object o) {
        return StringValueOf(o,"");
    }
    public static String safe(Object o, String dflt) {
        if(o==null) return dflt;
        String s = o.toString();
        if(s==null) return dflt;
        s = s.trim();
        return s.length()<=0 ? dflt : s;
    }
    public static int intValueOf(Object o, int dflt) {
        if(o==null) return dflt;
        try {
            if(o instanceof Number) return ((Number)o).intValue();
            String s = o.toString().trim();
            if(s.length()==0) return dflt;
            return Integer.parseInt(o.toString());
        } catch(Throwable t) { return dflt; }
    }
    private static boolean isOK(char c) {
        return 'A'<=c && c<='Z' || 'a'<=c && c<='z' || '0'<=c && c<='9' || c=='-' || c=='_';
    }
}
