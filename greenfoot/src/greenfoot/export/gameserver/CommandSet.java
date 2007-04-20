/* Copyright(c) 2005 Sun Microsystems, Inc.  All Rights Reserved. */

package greenfoot.export.gameserver;

import java.util.HashMap;
import java.lang.reflect.*;

/**
 * @author James Gosling
 * @created March 22, 2006
 */
public class CommandSet extends HashMap<Object, UTCmd> {
    private final Class claz;
    private static final UTCmd defaultCommand = new UTCmd() {
        public void execute(Object target, UTCL scl) {
            throw new IllegalArgumentException("No definition found for "+scl+"\nin: "+target);
        }
    };
    private static final HashMap<Class,CommandSet> csets = new HashMap<Class,CommandSet>();
    public synchronized static CommandSet forObject(Object o) {
        Class c = o.getClass();
        CommandSet cs = csets.get(c);
        if(cs==null) {
            cs = new CommandSet(c);
            csets.put(c,cs);
        }
        return cs;
    }
    private static Class[] cmdArgs = new Class[] { UTCL.class };
    public CommandSet(Class claz) {
        this.claz = claz;
    }
    public UTCmd get(Object k) {
        UTCmd v = super.get(k);
        if(v==null) {
            try {
                final Method m = claz.getMethod(k.toString(),cmdArgs);
                v = new UTCmd() {
                    public void execute(Object target, UTCL ctx) {
                        try {
                            m.invoke(target,ctx);
                        } catch (Throwable ex) {
                            System.err.println("Command execution error:"+
                                    "\n\t  cmd: "+ctx+
                                    "\n\t meth: "+m+
                                    "\n\tttype: "+(target==null ? "null" : target.getClass().getName())+
                                    "\n\t  err: "+ex);
                            ex.printStackTrace();
//                            System.exit(-1);
                        }
                    }
                };
                put(k,v);
            } catch (SecurityException ex) {
                ex.printStackTrace();
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
//        System.err.println("get "+k+" "+v+"\n  "+this);
        return v!=null ? v : defaultCommand;
    }
}
