package bluej.editor.moe.autocomplete.parser;

import bluej.editor.moe.autocomplete.Debug;
import java.util.*;


/**
 * THIS CLASS HAS BEEN TAKEN FROM SPEED JAVA.
 * <br>SOME UNNECESSARY CODE HAS BEEN REMOVED.
 * <br><br>
 * Following description by Jim Wissner
 * (<A href="mailto:jim@jbrix.org">jim@jbrix.org</A>):
 * <br><br>
 * Copyright (c) 2001 Jim Wissner
 * <br><br>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * <br><br>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * @author Jim Wissner (<A href="mailto:jim@jbrix.org">jim@jbrix.org</A>)
 */
public class Scope extends Object {
    private static final TreeMap emptyMembers = new TreeMap();
    private Scope parent;
    private int startLine;
    private int endLine = -1;
    private int level;
    private int mods = 0;
    private Vector items = new Vector();
    private String scopeName;
    private String scopeKind;
    private String[] params = new String[0];
//  private TreeMap sortedItems = new TreeMap();

    public Scope(Scope parent, int sl) {
        this.parent = parent;
//      this.scopeName = scopeName;
        startLine = sl - 1;
        if (parent == null) {
            level = 0;
        } else {
            level = parent.level + 1;
        }
    }

    void addParameter(String param) {
        String[] array = new String[params.length + 1];
        for (int i = 0; i < params.length; i++) {
            array[i] = params[i];
        }
        array[params.length] = param;
        params = array;
    }

    public String[] getParameters() {
        return params;
    }

    public Scope push(int ln) {
        Scope sc = new Scope(this,ln);
        addElement(sc);
        return sc;
    }

    void setStartLine(int sl) {
        startLine = sl - 1;
    }

    public void setScopeName(String s) {
        scopeName = s;
    }

    public void setModifiers(int m) {
        mods = m;
    }

    public void setScopeKind(String s) {
        scopeKind = s;
    }

    public Scope getParentScope() {
        return parent;
    }

    public Scope getRootScope() {
        Scope sc = this;
        while (sc.getParentScope() != null) {
            sc = sc.getParentScope();
        }
        return sc;
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getScopeKind() {
        return scopeKind;
    }

    public int getModifiers() {
        return mods;
    }

    public Scope[] getChildScopes() {
        int i, n = size();
        Vector v = new Vector();
        for (i = 0; i < n; i++) {
            Object o = elementAt(i);
            if (o instanceof Scope) {
                v.addElement(o);
            }
        }
        n = v.size();
        Scope[] array = new Scope[n];
        for (i = 0; i < n; i++) {
            array[i] = (Scope)(v.elementAt(i));
        }
        return array;
    }

    public Scope pop(int endLine) {
        this.endLine = endLine;
        return parent;
    }

    public Object elementAt(int i) {
        return items.elementAt(i);
    }
/*
    public TreeMap getSortedItems() {
        return sortedItems;
    }
*/


    public void addElement(Object o) {
        items.addElement(o);
    }

    public int size() {
        return items.size();
    }

    public int getBeginLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getLevel() {
        return level;
    }

    public Scope scopeOfLine(int ln) {
        if (ln >= startLine && (ln <= endLine || endLine < 0)) {
            Scope sc = null;
            for (int i = 0; i < size(); i++) {
                if (elementAt(i) instanceof Scope) {
                    sc = ((Scope)(elementAt(i))).scopeOfLine(ln);
                    if (sc != null) return sc;
                }
            }
            return this;
        }
        return null;
    }

    public Vector itemsInScope() {
        Vector v;
        if (parent != null) {
            v = parent.itemsInScope();
        } else {
            v = new Vector();
        }
        for (int i = 0; i < size(); i++) {
            if (!(elementAt(i) instanceof Scope)) {
                v.addElement(elementAt(i));
            }
        }
        return v;
    }

    public void print() {
        print(0);
    }

    public void print(int l) {
        
        String indent = "";
        for (int j = 0; j < l; j++) {
            indent = indent + "    ";
        }

        Debug.printScopeMessage(indent + "startLine=" + startLine + ", endLine=" + endLine);

        for (int i = 0; i < size(); i++) {
            if (elementAt(i) instanceof Scope) {
                Scope s = ((Scope)(elementAt(i)));
                s.print(l+1);
            } else {
                Debug.printScopeMessage(indent + "elementAt(" + i + ")" + elementAt(i));
            }
        }
    }
    






    public ArrayList getScopeTree(int line) {
        return getScopeTree(line,true);
    }

    public ArrayList getScopeTree(int line, boolean sort) {
        return getScopeTree(line,sort,true,true,true,true);
    }

    public ArrayList getScopeTree(int line, boolean sort,
        boolean incFields,boolean incMethods,boolean incClasses,
        boolean incRootClasses)
    {
//      synchronized (parsingToken) {
        listIndex = 0;
        ArrayList list = new ArrayList();
        TreeMap map = new TreeMap();
        String[] nearestScope = getScopeTree(null,
            getRootScope(),null,map,line,0,
            sort,incFields,incMethods,incClasses,incRootClasses);
        if (nearestScope != null) {
            nearestScope[0] += ":nearest";
        }
//      s = "";
        enumerateMap(list,map);
/*
        try {
            String fname = "c:\\alpha.xml";
            FileOutputStream fs = new FileOutputStream(fname);
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fs));
            br.write(s, 0, s.length());
            br.flush();
            br.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
*/
        return list;
//      }
    }
//          String s = "";

    private void enumerateMap(ArrayList list, TreeMap map) {
        Iterator iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            Object key = iterator.next();
            Object o = map.get(key);
            if (o instanceof TreeMap) {
if (debug) {
    String[] dummyArray = new String[6];
    dummyArray[0] = "field(boolean) |" + key;
    dummyArray[1] = "0";
    dummyArray[2] = "<--- MAP *************";
    dummyArray[3] = "1";
    dummyArray[4] = "1";
    dummyArray[5] = "18";
    list.add(dummyArray);
}
                enumerateMap(list,(TreeMap)o);
//              s+=""+key+" = (map)\n";
            } else if (o instanceof String[]) {
                list.add(o);
//              s+=""+key+" = "+((String[])o)[2]+"\n";
            }
        }
    }
boolean debug=false;
private int listIndex;
    private String[] getScopeTree(
        String[] lastItem,
        Scope scope, String[] nearestScope,
        TreeMap map,
        int line, int indent, boolean sort,
        boolean incFields,boolean incMethods,boolean incClasses,
        boolean incRootClasses)
    {
        // regarding this if: what if parent is not a named scope?
        // looks weird or not?
        int level = scope.getLevel();
        if ((line >= scope.getBeginLine() && line < scope.getEndLine())
            || scope.getLevel() < 3
            || (scope.getParentScope() != null
                && line >= scope.getParentScope().getBeginLine()
                && line < scope.getParentScope().getEndLine())
            ) {
            String[] array = new String[6];
            array[0] = "" + scope.getScopeKind();
            level = 0;
            Scope s = scope.getParentScope();
            while (s != null) {
                if (s.getScopeName() != null) {
                    level++;
                }
                s = s.getParentScope();
            }
            array[1] = "" + level;
            array[2] = "" + scope.getScopeName();
            array[3] = "" + scope.getBeginLine();
            array[4] = "" + scope.getEndLine();
            array[5] = "" + scope.getModifiers();
            String[] params = scope.getParameters();
            array[0] += "(";
            for (int i = 0; i < params.length; i++) {
                array[0] += (i > 0? "," : "") + params[i];
            }
            array[0] += ")";
            if (scope.getScopeName() != null) {
                if (lastItem != null) {
                    if (!lastItem[1].equals("" + level)) {
                        TreeMap newMap = new TreeMap();
                        String key = (++listIndex < 10? "00"
                            : listIndex < 100? "0" : "") + listIndex;
                        if (sort) {
                            key = lastItem[2]+lastItem[1]
                                + scope.getScopeName() + key;
                        }
/*
if (debug) {
    String[] dummyArray = new String[6];
    dummyArray[0] = "field(boolean) |" + key;
    dummyArray[1] = "" + (level+1);
    dummyArray[2] = "dummy";
    dummyArray[3] = "1";
    dummyArray[4] = "1";
    dummyArray[5] = "18";
    map.put("OHDEAR"+key+".",dummyArray);
}
*/
if ((incMethods/* && array[0].indexOf("method") > -1*/)
    || (incClasses/* && array[0].indexOf("class") > -1*/)) {
                        map.put(key,newMap);
                        map = newMap;
}
                    }
                }
String  key = (++listIndex < 10? "00"
                                : listIndex < 100? "0" : "") + listIndex;
                        if (sort) {
                            key = scope.getScopeName()+array[1];
                        }
if (debug) array[0] += "|"+key;
                if ((array[0].indexOf("field") > -1 && incFields)
                    || (array[0].indexOf("method") > -1 && incMethods)
                    || (array[0].indexOf("class") > -1 && incClasses)) {
if (level > 0 || incRootClasses) {
                    map.put(key,array);
}
                }
                    lastItem = array;
            }
            if (line >= scope.getBeginLine() && line < scope.getEndLine()
                && scope.getScopeName() != null) {
                nearestScope = array;
            }
        }
        if (line >= scope.getBeginLine() && line < scope.getEndLine()) {
            int n = scope.size();
            for (int i = 0; i < n; i++) {
                Object o = scope.elementAt(i);
                if (o instanceof String[]) {
                    String[] sa = (String[])o;
                    String[] array = new String[6];
                    array[0] = "field(" + sa[0] + ")";
                    if (lastItem == null) {
                        array[1] = "0";
//                      level = 0;
                    } else {
//                      String[] pa = (String[])(list.get(list.size() - 1));
                        if (lastItem[0].indexOf("field") > -1) {
                            array[1] = lastItem[1];
                        } else {
                            array[1] = "" + (Integer.parseInt(lastItem[1])
                        + (
                            (incMethods && lastItem[0].indexOf("method") > -1)
                        ||  (incClasses && lastItem[0].indexOf("class") > -1)
                                ? 1 : 0));
                            TreeMap newMap = new TreeMap();
String key = (++listIndex < 10? "00"
                                : listIndex < 100? "0" : "") + listIndex;
                        if (sort) {
key = lastItem[2]+lastItem[1]+array[2] + key;
                        }
/*
if (debug) {
    String[] dummyArray = new String[6];
    dummyArray[0] = "field(boolean) |" + key;
    dummyArray[1] = "" +( level+1);
    dummyArray[2] = "dummy";
    dummyArray[3] = "1";
    dummyArray[4] = "1";
    dummyArray[5] = "18";
    map.put("SHIT"+key+".",dummyArray);
}
*/
if ((incMethods/* && array[0].indexOf("method") > -1*/)
    || (incClasses/* && array[0].indexOf("class") > -1*/)) {
                            map.put(key,newMap);
                            map = newMap;
}
                        }
                    }
//                  array[1] = "" + (level + 1);
                    array[2] = "" + sa[1];
                    array[3] = "" + sa[3];
                    array[4] = "" + sa[3];
                    array[5] = sa[2];
                    if (sa.length == 4) {
String key = (++listIndex < 10? "00"
                                : listIndex < 100? "0" : "") + listIndex;
                        if (sort) {
                            key = array[2]+array[1];
                        }
if (debug) array[0] += "|"+key;
                if ((array[0].indexOf("field") > -1 && incFields)
                    || (array[0].indexOf("method") > -1 && incMethods)
                    || (array[0].indexOf("class") > -1 && incClasses)) {
                    map.put(key,array);
                }
                    lastItem = array;
                    }
                }
            }
        }
        if ((line >= scope.getBeginLine() && line < scope.getEndLine())
            || scope == getRootScope()) {
            Scope[] kids = scope.getChildScopes();
            for (int i = 0; i < kids.length; i++) {
                nearestScope
                    = getScopeTree(lastItem,
                    kids[i],nearestScope,map,line,indent + 1,sort,
                    incFields,incMethods,incClasses,incRootClasses);
            }
        }
        return nearestScope;
    }

    public TreeMap getMembers(int line) {
        Scope rootScope = getRootScope();
        if (rootScope != null) {
            Scope scope = rootScope.scopeOfLine(line);
            if (scope != null) {
                TreeMap members = new TreeMap();
                Vector v = scope.itemsInScope();
                for (int i = 0; i < v.size(); i++) {
                    String[] var = (String[])(v.elementAt(i));
                    members.put(var[1].toLowerCase(),var);
                }
                return members;
            }
        }
        return emptyMembers;
    }

}

