/*
 * SimpleControlLanguage.java
 *
 * Created on March 22, 2006, 12:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package greenfoot.export.gameserver;
import java.io.*;
import java.util.HashMap;

/**
 * UTCL - Unspeakably Trivial Command Language
 *
 * This is pretty much the simplest possible scripting language.  A statement is
 * a line of strings. Executing a statement takes the first string and looks it up
 * in a table of commands and calls the execute method on the command.
 *
 * The class has some of the API of Vector, making it act like an array of Strings.
 * This array of strings is the current statement.
 */
public class UTCL {
    private Object[] things = new Object[1];
    private int size = 0;
    private static final int argBase = 2;
    private StreamOfThingsReader in;
    private final HashMap<String,Target> targets = new HashMap<String,Target>();

    public UTCL(StreamOfThingsReader in) {
        connect(in);
    }
    public UTCL() {
        this((InputStream)null);
    }
    public UTCL(InputStream in) {
        this(in != null ? new StreamOfThingsReader(in) : null);
    }
    public void connect(StreamOfThingsReader in) {
        this.in = in;
    }
    public void connect(InputStream in) {
        connect(new StreamOfThingsReader(in));
    }
    public final void target(String tn, Object tv) {
        targets.put(tn,new Target(tv));
    }
    int tser = 0;
    public final String target(Object tv) {
        String tn;
        while(targets.get(tn="t"+tser++)!=null);
        targets.put(tn,new Target(tv));
        return tn;
    }
    public final void add(Object s) {
        if(size>=things.length) {
            Object[] ns = new Object[size+10];
            System.arraycopy(things,0,ns,0,size);
            things = ns;
        }
        things[size++] = s;
    }
    public boolean readLine() throws IOException {
        clear();
        while(true) {
            Object s = in.read();
//            if(!(s instanceof String)) System.err.println("   non String "+s);
            if(s==in.EOL) return true;
            if(s==in.EOF) return false;
            add(s);
        }
    }
    public void execute() {
        System.err.println("Exec "+this);
        if(size>0)
        if("*".equals(things[0])) {
            for(Target t:targets.values())
                t.commands.get(things[1]).execute(t.target, this);
        } else {
            Target t = targets.get(things[0]);
            if(t==null) throw new IllegalArgumentException("Undefined target "+this);
            else t.commands.get(things[1]).execute(t.target, this);
        }
    }
    public void readExec() throws IOException {
        while(readLine()) execute();
    }
    public final String toString() {
        StringBuilder s = new StringBuilder();
        for(int i = 0; i<size; i++) {
            if(s.length()>0) s.append(' ');
            s.append(things[i]);
        }
        return s.toString();
    }
    public final void setSize(int n) {
        if(n<size) size = n;
    }
    public final void removeRange(int c0, int c1) {
        if(c1>size) c1 = size;
        int nrem = c1-c0;
        if(nrem>0) {
            System.arraycopy(things,c1,things,c0,size-c1);
            size-=nrem;
        }
    }
    public final void close() throws IOException { in.close(); in=null; }
    public final void clear() { size = 0; }
    public final int getSize() { return size; }
    public final Object getArg(int i) {
        i+=argBase;
        return i>=size ? null : things[i];
    }
    public final String getArg(int i, String dflt) {
        i+=argBase;
        return i>=size ? dflt : things[i].toString();
    }
    public final long getArg(int i, long dflt) {
        i+=argBase;
        if(i>=size) return dflt;
        Object o = things[i];
        if(o instanceof Number) return ((Number)o).longValue();
        return o==null ? 0 : Long.parseLong(o.toString());
    }
    class Target {
        CommandSet commands;
        Object target;
        Target(Object t) {
            target = t;
            commands = CommandSet.forObject(t);
        }
    }
}
