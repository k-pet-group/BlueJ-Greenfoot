package bluej.editor.moe.autocomplete.parser;

import bluej.editor.moe.autocomplete.Debug;
import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * THIS CLASS HAS BEEN TAKEN FROM SPEED JAVA.
 * <br>SOME UNNECESSARY CODE HAS BEEN REMOVED.<br><br>
 *
 * Following description by Jim Wissner
 * (<A href="mailto:jim@jbrix.org">jim@jbrix.org</A>):<br><br>
 *
 * First, what this is NOT: a full-blown java parser.
 *
 * This is meant to be a very small, very fast parser of just as much
 * information as we need, and no more.  There are purposely no external
 * "Info" classes (MethodInfo, etc) so as to keep things small and light.
 * If the richest set of information is the need, other people probably
 * have better parsers.
 *
 * In place of *Info classes, we maintain a map of members, each of which
 * is a string array.  The first in the array is the "type" and the second
 * is the member name. Note that they are indexed in the map by a lower-case
 * version of their name, to make things smooth and case-insensitive when
 * working with completion.
 *
 * It tries to find fields given the specified scope (by line number) of
 * where this was "called from".
 *
 * This is a work in progress, and while it is decent, it does have
 * issues.
 *
 * @version 0.3 - This class was created for version 0.3 of SpeedJava
 *
 * @author Jim Wissner (<A href="mailto:jim@jbrix.org">jim@jbrix.org</A>)
 */
public class JavaSourceInfo extends java.lang.Object {
    public static final int MOD_PUBLIC = 1;
    public static final int MOD_PRIVATE = 2;
    public static final int MOD_PROTECTED = 4;
    public static final int MOD_ABSTRACT = 8;
    public static final int MOD_STATIC = 16;
    public static final int MOD_LOCAL = 32;
    public static final int MOD_FINAL = 64;
    public static final int MOD_INSTANCE = 128;
    private final static String[] keywords = {
        "abstract",
        "boolean",
        "byte",
        "char",
        "double",
        "float",
        "int",
        "long",
        "short",
        "do",
        "true",
        "false",
        "for",
        "while",
        "if",
        "else",
        "public",
        "protected",
        "private",
        "static",
        "final",
        "native",
        "synchronized",
        "class",
        "interface",
        "import",
        "package",
        "extends",
        "implements",
        "switch",
        "case",
        "return",
        "new",
        "this",
        "super",
        "default"
    };

    private static final String[] types = {
        "boolean",
        "byte",
        "char",
        "double",
        "float",
        "int",
        "long",
        "short",
        "void"
    };

    //private static Hashtable classes = new Hashtable();
    private static Hashtable absoluteTypes = new Hashtable();

    private HashMap listeners = new HashMap();
    private Vector imports;
    private String className;
    private Class superclass = Object.class;
    private Scope rootScope = null;
    private final Object syncToken = new Object();


    public JavaSourceInfo() {
    }

    public Class getSuperclass() {
        synchronized (syncToken) {
            return superclass;
        }
    }

    public String getClassName() {
        synchronized (syncToken) {
            return className;
        }
    }

    private static boolean isPrimitive(String s) {
        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return true;
        }
        return false;
    }

    private static boolean isType(String s) {
        if (s == null || s.length() < 1) return false;
        s = s.trim();

        if (s.endsWith("]") && s.length() > 2) {
            if (s.substring(0,s.length()-1).trim().endsWith("[")) {
                s = s.substring(0,s.lastIndexOf('[')).trim();
            }
        }

        // This is a NASTY heuristic.  We really need to clean up parse()
        // to determine if something is a type the right way.  But until
        // then we'll leave this be:

        int x = s.lastIndexOf('.') + 1;
        char c = s.charAt(x < s.length()? x : 0);
        char c2 = s.charAt(s.length()-1);

        if ((c >= 'A' && c <= 'Z') && (c2 < 'A' || c2 > 'Z')) return true;

        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return true;
        }

        return false;
    }



    private static boolean isNonTypeKeyword(String s) {
        for (int i = 0; i < keywords.length; i++) {
            if (s.equals(keywords[i])) {
                for (int j = 0; j < types.length; j++) {
                    if (s.equals(types[j])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isKeyword(String s) {
        for (int i = 0; i < keywords.length; i++) {
            if (s.equals(keywords[i])) return true;
        }
        for (int i = 0; i < types.length; i++) {
            if (s.equals(types[i])) return true;
        }
        return false;
    }

    public Scope getRootScope() {
        synchronized (syncToken) {
            return rootScope;
        }
    }

    public String getAbsoluteType(String type) {
        return getAbsoluteType(type,imports);
    }

    public String getAbsoluteType(String type, Vector imports) {

        Debug.printParserMessage("");
        Debug.printParserMessage("getAbsoluteType()    type=[" + type + "], imports=[" + imports + "]");
        Debug.printParserMessage("getAbsoluteType()    absoluteTypes.get(type)=" + absoluteTypes.get(type));

        if (type == null) return null;
        int n = imports.size();


        if (absoluteTypes.get(type) != null) {
            String absType = (String)(absoluteTypes.get(type));
            if (type.equals(absType) || isPrimitive(type)) {
                return type;
            }
            String s = "java.lang." + type;
            if (absType.equals(s)) {
                return s;
            }
            for (int i = 0; i < n; i++) {
                s = (String)(imports.elementAt(i)) + "." + type;
                if (absType.equals(s)) {
                    return s;
                }
            }
            return type;
        }

        if (isPrimitive(type)) {
            absoluteTypes.put(type,type);
            return type;
        }

        for (int i = 0; i < n; i++) {
            if (((String)(imports.elementAt(i))).endsWith("." + type)) {
// this doesn't involve class.forname, so it's quick, so don't need to,
// plus import might change...  So, let's not put this in absoluteTypes.
//              absoluteTypes.put(type,imports.elementAt(i));
                return (String)(imports.elementAt(i));
            }
        }

        try {
            if (ClassUtilities.findClass(type) != null) {
                absoluteTypes.put(type,type);
                return type;
            }
        }
        catch (java.lang.Throwable ex){}

        try {
            if (ClassUtilities.findClass("java.lang." + type) != null) {
                absoluteTypes.put(type,"java.lang." + type);
                return "java.lang." + type;
            }
        }
        catch (java.lang.Throwable ex) {}


        String str;
        for (int i = 0; i < n; i++) try {
            str = (String)(imports.elementAt(i)) + "." + type;
            if (ClassUtilities.findClass(str) != null) {
                absoluteTypes.put(type,str);
                return str;
            }
        }
        catch (java.lang.Throwable ex) {}

        return type;
    }






    public Class getMemberType(String memberName, int line) {

        Debug.printParserMessage("getMemberType()    memberName=[" + memberName + "], line=" + line);

        if (memberName == null) {
            return null;
        }
        Scope scope = getRootScope();

        Debug.printParserMessage("getMemberType()    rootScope=" + scope);

        if (scope == null) {
            return null;
        }
        TreeMap members = scope.getMembers(line);
        memberName = memberName.trim();

        String[] member = (String[])(members.get(memberName.toLowerCase()));

        if(member==null){
            Debug.printParserMessage("getMemberType()    member=null");
        }
        else{
            for(int i=0; i < member.length; i++){
                Debug.printParserMessage("getMemberType()    member[" + i + "]=" + member[i]);
            }
        }

        if (member != null) {
            String type = member[0];
            if (type.endsWith("[]")) {
                type = type.substring(0,type.length() - 2);
            }
            type = getAbsoluteType(type,imports);
            Debug.printParserMessage("getMemberType()    type=" + type);

            if (type != null) {

                //Class cls = (Class)(classes.get(type));
                //Class cls = null;

                //if (cls != null) {
                //    return cls;
                //}

                try {
                    Class cls = ClassUtilities.findClass(type);
                    //classes.put(type,cls);
                    return cls;
                } catch (java.lang.Throwable ex) {
                }
            }
        }
        return null;

    }



    private String lastFileName = null;
    private boolean running = false;

    public void parse(String java) {



       rootScope = null;
       superclass = Object.class;
       imports = new Vector();
       className = "Unknown";
       running = true;

        try {
            Vector newImports = new Vector();

            Scope newRootScope = new Scope(null, 0);
            Class newSuperclass = Object.class;
            String newClassName = "Unknown";

            //  /* */ comments with blank line in between (not even white space)
            //  throw off line numbering...   so, for now:
            //java = SpeedJava.substitute(java,"\n\n","\n \n");


            try {
                StringBuffer buff = new StringBuffer(java.length() + 1000);
                long time = System.currentTimeMillis();
                int z=0;
                int i = 0;
                int x = 0;

                while ((i = java.indexOf("\n\n",i)) > -1) {
                    buff.append(java.substring(x, i));
                    buff.append("\n \n");
                    i += 2;
                    x = i;
                    z++;
                }
                buff.append(java.substring(x));
                java = buff.toString();
                time = (System.currentTimeMillis() - time) / 1000;

            } catch (Exception ex) {}


            int previ = -1;
            Class sclass = Object.class;
            String cName = null;
            Vector lv = new Vector();
            String prev = null;
            String prevPrev = null;
            String prevPrevPrev = null;
            String s;
            int n,ptok = 0,pptok = 0;
            StringReader reader = new StringReader(java);
            StreamTokenizer stok = new StreamTokenizer(reader);
            stok.slashSlashComments(true);
            stok.slashStarComments(true);
            stok.wordChars('_','_');
            int i = stok.nextToken();
            String lastType = null;
            Scope scope = newRootScope;
            boolean dec = false;
            Vector tmpv = new Vector();
            String[] lastMethod = null;
            boolean expectingParamType = false;
            boolean fullParams = true;
            int lineNo;
            String nextScopeName = null;
            String nextScopeKind = null;
            int nextScopeStartLine = -1;
            int mods = 0;
            ArrayList params = new ArrayList();
            long pauseCounter = 0;
            String constructorName = null;

            while (i != stok.TT_EOF) {

                if (++pauseCounter % 100 == 0) {
                    Thread.currentThread().sleep(20);
                }

                s = stok.sval;
                if (s != null) s = s.trim();

                lineNo = stok.lineno();
                n = stok.nextToken();

                //pushBack() causes the next call to the nextToken method of this
                //tokenizer to return the current value in the type field, and
                //not to modify the value in the nval or sval field.
                //This comment added by Darren Link
                stok.pushBack();

                if (s != null) {
                    if (s.equals("static")) {
                        mods = mods | MOD_STATIC;
                    } else if (s.equals("abstract")) {
                        mods = mods | MOD_ABSTRACT;
                    } else if (s.equals("final")) {
                        mods = mods | MOD_FINAL;
                    } else if (s.equals("public")) {
                        mods = mods | MOD_PUBLIC;
                    } else if (s.equals("private")) {
                        mods = mods | MOD_PRIVATE;
                    } else if (s.equals("protected")) {
                        mods = mods | MOD_PROTECTED;
                    }
                }

                if (lastMethod != null) {

                    if (i == ')') {
                        lastMethod[2] += ")";
                        lastMethod = null;
                    } else if (s != null) {
                        if (expectingParamType) {
                            if (lastMethod[2].length() > 1) {
                                lastMethod[2] += ", ";
                            }

                            Debug.printParserMessage("getAbsoluteType 1");
                            params.add(getAbsoluteType(s,newImports));

                            if (fullParams) {
                                Debug.printParserMessage("getAbsoluteType 2");
                                lastMethod[2] += getAbsoluteType(s,newImports);
                            } else {
                                lastMethod[2] += s;
                            }
                            expectingParamType = false;
                        } else {
                            expectingParamType = true;
                        }
                    }
                }


                if (i == ';') nextScopeName = null;
                if (i == '{' || i == ';') dec = false;
                if (i == '{') {

                    Debug.printScopeMessage("{ lineNo=" + lineNo);

                    scope = scope.push(lineNo);
                    if (constructorName != null || nextScopeName != null) {
                        if (constructorName != null) {
                            scope.setScopeName(constructorName);
                        } else {
                            scope.setScopeName(nextScopeName);
                        }

                        scope.setScopeKind(nextScopeKind);

mods = (mods |
    (scope.getLevel() > 2? MOD_LOCAL : 0) |
    (scope.getLevel() <= 2 && ((mods | MOD_STATIC) != MOD_STATIC)?
        MOD_INSTANCE : 0) );
if ((mods & MOD_STATIC) == MOD_STATIC) {
    mods = mods & ~MOD_INSTANCE;
}

                        scope.setModifiers(mods);
                        if (nextScopeStartLine > -1) {
                            scope.setStartLine(nextScopeStartLine);
                        }
                        for (int j = 0; j < params.size(); j++) {
                            scope.addParameter((String)(params.get(j)));
                        }
                        nextScopeName = null;
                        nextScopeKind = null;
                    } else {
                        scope.setScopeName(nextScopeName);
                    }

                    if (scope == null) {
                        // What's the best way to really deal with this?
                        return;
                    }
                    for (int k = 0; k < tmpv.size(); k++) {
                        scope.addElement(tmpv.elementAt(k));
                    }
                    tmpv.removeAllElements();
constructorName = null;
                } else if (i == '}') {
                    Debug.printScopeMessage("} lineNo=" + lineNo);
                    scope = scope.pop(lineNo);
                    if (scope == null) {
                        // What's the best way to really deal with this?
                        return;
                    }
                }


                if (i == '{' || i == '}' || i == ';') {
                    params.clear();
                    mods = 0;
                    nextScopeStartLine = -1;
                } else if (nextScopeStartLine == -1) {
                    nextScopeStartLine = lineNo;
                }

                if (s != null) {
                    if (prev != null && (prev.equals("import") || prev.equals("package"))) {
                        if (s.endsWith("*")) s = s.substring(0,s.length() - 1);
                        if (s.endsWith(".")) s = s.substring(0,s.length() - 1);
                        newImports.addElement(s);
                    }
                    if (prev != null) {
                        if (prev.equals("class") || prev.equals("interface")) {
                            if (cName == null) {
                                cName = s;
                            }
                            nextScopeName = s;
                            nextScopeKind = prev;
                        } else if (prev.equals("new")) {
                            nextScopeName = s;
                            nextScopeKind = "anonymous class";
                        }
                    }

                    if (prev != null && prev.equals("extends")
                        && scope.getParentScope() == null) {
                        try {

                            Debug.printParserMessage("getAbsoluteType 3");
                            sclass = ClassUtilities.findClass(
                                getAbsoluteType(s,newImports));
                        }
                        catch (java.lang.Throwable ex) {}
                    }

                    if ((isType(s) || s.equals("void"))
                            // we need these since isType is messed up -
                            // it incorrectly doesn't check if it's in a
                            // literal.  We also need it for our
                            // s.equals("void") call.  Again, we need
                            // a better type test mechanism!!!!
                            && i != '\"'
                            && i != '\'') {
                        lastType = s;
                    }

                } else if (i == ';' || i == '{') {
                    lastType = null;
                } else if (i == '(') {
                    if (prev != null
                        && prev.equals("" + scope.getScopeName())) {
                        constructorName = prev;
                        nextScopeKind = "constructor";
                    } else {
                    }
                    lastType = null;
                }

                if (lastType == null || s == null || isNonTypeKeyword(s)) {
                    prevPrevPrev = prevPrev;
                    prevPrev = prev;
                    prev = s;
                    pptok = ptok;
                    ptok = i;
                    previ = i;
                    i = stok.nextToken();
                    continue;
                } else if (n != '(') {
                    if ((prev != null && isType(prev)) || ptok == ']' || ptok == ',') {

                        if (s.length() > 0  && !isType(s)
                            && (ptok != ',' || lastMethod == null) ) {

                            Debug.printParserMessage("getAbsoluteType 4");
                            String type = getAbsoluteType(lastType, newImports);

                            int qfa = stok.nextToken();
                            stok.pushBack();
                            String[] var = new String[4];
                            var[0] = type + (ptok == ']' || qfa == '['? "[]" : "");
                            var[1] = s;

                            int m = (mods &
                                (lastMethod != null? ~MOD_STATIC : mods) |
                                (scope.getLevel() > 1 || lastMethod != null? MOD_LOCAL : 0) |
                                (scope.getLevel() <= 1 && lastMethod == null? MOD_INSTANCE : 0) );

                            if ((m & MOD_STATIC) == MOD_STATIC) {
                                m = m & ~MOD_INSTANCE;
                            }

                            var[2] = "" + m;
                            var[3] = "" + ((nextScopeStartLine > -1? nextScopeStartLine : lineNo) - 1);

                            if (dec || constructorName != null) {
                                tmpv.addElement(var);
                            } else if (scope != null && lastType != null) {
                                scope.addElement(var);
                            }
                        }
                    }
                } else {

                    if ( (prev != null && (isType(prev) || prev.equals("void")))
                        || (ptok == ']' && pptok == '['
                        && prevPrevPrev != null && (isType(prevPrevPrev)
                        || prevPrevPrev.equals("void")))) {


                        String mname = "";
                        Scope sc = scope.getParentScope();
                        while (sc != null) {
                            if (sc.getParentScope() != null) {
                                mname = sc.getScopeName() + "/" + mname;
                            }
                            sc = sc.getParentScope();
                         }

                         mname += s;
                         String[] method = new String[5];
                         // should add 3rd item for params

                         Debug.printParserMessage("getAbsoluteType 5");
                         method[0] = getAbsoluteType(lastType,newImports);
                         method[1] = s;//+" - '"+(char)ptok+"' '"+prevPrevPrev+"' '"+(char)pptok+"' ";
                         method[2] = "("; // just this for now, add params later...
                         method[3] = "" + lineNo; // just this for now, add params later...
                         method[4] = (scope.getLevel() > 1? "local" : "instance");
                         //members.put(s.toLowerCase(),method);
                         newRootScope.addElement(method);
                        expectingParamType = true;
                        lastMethod = method;
                        //scope.setScopeName(s);
                        nextScopeName = s;
                        nextScopeKind = "method(" + method[0] + ")";

                        dec = true;
                    }
                }

                if (i == '{' || i == '}' || i == ';') {
                    mods = 0;
                }

                pptok = ptok;
                ptok = i;
                prevPrevPrev = prevPrev;
                prevPrev = prev;
                prev = s;
                previ = i;
                i = stok.nextToken();
            }

            newClassName = (cName != null? cName : "Unknown");
            newSuperclass = sclass;

            synchronized (syncToken) {
                rootScope = newRootScope;
                superclass = newSuperclass;
                imports = newImports;
                className = newClassName;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            synchronized (syncToken) {
                rootScope = null;
                superclass = Object.class;
                imports = new Vector();
                className = "Unknown";
            }
        } finally {
            synchronized (syncToken) {
                //running = false;
            }
        }

    }  //End of parse

}  //End of class


