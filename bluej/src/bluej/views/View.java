package bluej.views;

import bluej.utility.JavaNames;

import java.lang.reflect.*;
import java.io.InputStream;
import java.util.*;


/**
 * A representation of a Java class in BlueJ
 *
 * @author  Michael Cahill
 * @version $Id: View.java 1954 2003-05-15 06:06:01Z ajp $
 */
public class View
{
    private final String classIgnore = "class$";
    private final String accessIgnore = "access$";

    /** The class that this view is for **/
    protected Class cl;

    protected FieldView[] fields;
    protected FieldView[] allFields;
    protected ConstructorView[] constructors;
    protected MethodView[] methods;
    protected MethodView[] allMethods;

    protected Comment comment;
    private int instanceNum = 0;

    private static Map views = new HashMap();

    /**
     * Return a view of a class.
     * This is the only way to obtain a View object.
     */
    public static View getView(Class cl)
    {
        if(cl == null)
            return null;

        // Debug.message("Started getView for class " + cl);

        View v = (View)views.get(cl);
        if(v == null) {
            v = new View(cl);
            views.put(cl, v);
        }

        // Debug.message("Ended getView for class " + cl);

        return v;
    }

    /**
     * Remove from the view cache, all views of classes
     * which were loaded by loader
     */
    public static void removeAll(ClassLoader loader)
    {
        Iterator it = views.values().iterator();

        while(it.hasNext()) {
            View v = (View) it.next();

            if (v.getClassLoader() == loader) {
            	System.out.println("removing" + v.getLongDesc());
                it.remove();
            }
        }
    }

    private View(Class cl)
    {
        this.cl = cl;
    }

    private ClassLoader getClassLoader()
    {
        return cl.getClassLoader();
    }

    public String getQualifiedName()
    {
        return cl.getName();
    }

    /**
     * Gets the Class this view is looking into.
     * This is used to know the exact return type of a method and is consistent 
     * with the Java Reflection API. Damiano
     */
    public Class getViewClass ()
    {
        return cl;
    }

    public String getBaseName()
    {
        return JavaNames.getBase(cl.getName());
    }

    public View getSuper()
    {
        return getView(cl.getSuperclass());
    }

    public View[] getInterfaces()
    {
        Class[] interfaces = cl.getInterfaces();

        View[] interfaceViews = new View[interfaces.length];
        for(int i = 0; i < interfaces.length; i++)
            interfaceViews[i] =  getView(interfaces[i]);

        return interfaceViews;
    }

    public final boolean isInterface()
    {
        return cl.isInterface();
    }

    /**
     * Return views of all methods of this class (including inherited ones).
     * Walk superclasses + interfaces for methods. Method definitions higher
     * up in the inheritance hierarchy are first in the array, with the latest 
     * redefinition last.
     */
    public MethodView[] getAllMethods()
    {
        if(allMethods == null) {
            HashMap map = new HashMap();
            getAllMethods(map, 0);
            
            List methods = new ArrayList(map.values());
            Collections.sort(methods, new ElementComparer());

            int numMethods = methods.size();
            allMethods = new MethodView[numMethods];
            for(int i = 0; i < numMethods; i++) {
                MemberElement elem = (MemberElement)methods.get(i);
                allMethods[i] = (MethodView)elem.member;
            }
        }

        return allMethods;
    }

    /**
     ** Walk superclasses + interfaces for fields.
     ** All fields are inherited (+ overridden) from everywhere.
     **/
    public FieldView[] getAllFields()
    {
        if(allFields == null) {
            HashMap map = new HashMap();
            getAllFields(map, 0);
            
            List fields = new ArrayList(map.values());
            Collections.sort(fields, new ElementComparer());

            int numFields = fields.size();
            allFields = new FieldView[numFields];
            for(int i = 0; i < numFields; i++) {
                MemberElement elem = (MemberElement)fields.get(i);
                allFields[i] = (FieldView)elem.member;
            }
        }

        return allFields;
    }

    /**
     ** (Attempt at an) efficient implementation of getAllMethods + getAllFields
     ** The old version had shocking performance - this one uses a HashMap
     ** to notice the conflicts
     **/

    class MemberElement
    {
        int index;
        MemberView member;

        MemberElement(int index, MemberView member)
        {
            this.index = index;
            this.member = member;
        }
    }

    class ElementComparer implements Comparator
    {
        /** Return { -1, 0, 1 } to represent <a> { <, ==, > } <b> **/
        public final int compare(Object a, Object b)
        {
            int cmp = ((MemberElement)a).index - ((MemberElement)b).index;

            return (cmp < 0) ? -1 : ((cmp > 0) ? 1 : 0);
        }
    }

    protected int getAllMethods(HashMap h, int methnum)
    {
        if(allMethods != null) {
            // carefully copy from allMethods into h
            methnum = addMembers(h, allMethods, methnum);
            return methnum;
        }

        // otherwise, do the real work
        // carefully copy local methods into v

        View sView = getSuper();
        if(sView != null)
            methnum = sView.getAllMethods(h, methnum);

        if(isInterface()) {
            View[] ifaces = getInterfaces();
            for(int i = 0; i < ifaces.length; i++)
                methnum = ifaces[i].getAllMethods(h, methnum);
        }

        methnum = addMembers(h, getDeclaredMethods(), methnum);
        return methnum;
    }

    protected int getAllFields(HashMap h, int fieldnum)
    {
        if(allFields != null) {
            // carefully copy from allFields into h
            fieldnum = addMembers(h, allFields, fieldnum);
            return fieldnum;
        }

        // otherwise, do the real work
        // carefully copy local fields into v

        View sView = getSuper();
        if(sView != null)
            fieldnum = sView.getAllFields(h, fieldnum);

        View[] ifaces = getInterfaces();
        for(int i = 0; i < ifaces.length; i++)
            fieldnum = ifaces[i].getAllFields(h, fieldnum);

        fieldnum = addMembers(h, getDeclaredFields(), fieldnum);
        return fieldnum;
    }

    private int addMembers(HashMap h, MemberView[] members, int num)
    {
        //Debug.message("Started addMembers for " + cl);

        for(int i = members.length - 1; i >= 0; i--) {
            //Debug.message("Adding ->" + members[i].toString() + "<-");
            h.put(members[i].toString(), new MemberElement(num++, members[i]));
        }

        //Debug.message("Ended addMembers for " + cl);
        return num;
    }

    private boolean hideMethodName(String name)
    {
        return (name.startsWith(classIgnore) ||
                name.startsWith(accessIgnore));
    }

    public MethodView[] getDeclaredMethods()
    {
        int count = 0;
        if(methods == null) {
            Method[] cl_methods = cl.getDeclaredMethods();

            for(int i = 0; i < cl_methods.length; i++) {
                if (!hideMethodName(cl_methods[i].getName()))
                    count++;
            }
            methods = new MethodView[count];

            count = 0;
            for(int i = 0; i < cl_methods.length; i++) {
                if (!hideMethodName(cl_methods[i].getName())) {
                    methods[count] = new MethodView(this, cl_methods[i]);
                    count++;
                }
            }
        }

        return methods;
    }

    public FieldView[] getDeclaredFields()
    {
        if(fields == null)
            {
                Field[] cl_fields= cl.getDeclaredFields();
                fields = new FieldView[cl_fields.length];

                for(int i = 0; i < cl_fields.length; i++)
                    fields[i] = new FieldView(this, cl_fields[i]);
            }

        return fields;
    }

    public ConstructorView[] getConstructors()
    {
        if(constructors == null)
            {
                Constructor[] cl_constrs = cl.getDeclaredConstructors();
                constructors = new ConstructorView[cl_constrs.length];

                for(int i = 0; i < constructors.length; i++)
                    constructors[i] = new ConstructorView(this, cl_constrs[i]);
            }

        return constructors;
    }

    public Comment getComment()
    {
        loadComments();
        return comment;
    }

    public void setComment(Comment comment)
    {
        this.comment = comment;
    }

    boolean comments_loaded = false;
    protected void loadComments()
    {
        if(comments_loaded)
            return;     // already loaded - nothing to do

        comments_loaded = true;

        // match the comments against this view's members
        // -> put all members into a hashmap indexed by
        // <member>.getSignature() (== <comment>.getTarget())
        HashMap table = new HashMap();
        addMembers(table, getAllFields());
        addMembers(table, getConstructors());
        addMembers(table, getAllMethods());

        loadClassComments(this, table);
    }

    protected void loadClassComments(View curview, HashMap table)
    {
        // move up to the superclass first, so that redefinied comments override
        if(curview.getSuper() != null)
            loadClassComments(curview.getSuper(), table);

        CommentList comments = null;
        String filename = curview.getQualifiedName().replace('.', '/') + ".ctxt";

        try {
            InputStream in = null;

            if (curview.cl.getClassLoader() == null) {
                in = ClassLoader.getSystemResourceAsStream(filename);
            }
            else {
                in = curview.cl.getClassLoader().getResourceAsStream(filename);
            }

            if(in != null) {
                comments = new CommentList();
                comments.load(in);
                in.close();
            }
            //else
            //    Debug.message("Failed to load .ctxt file " + filename);

        } catch(Exception e) {
            e.printStackTrace();
        }

        if(comments != null) {
            // match up the comments read from the file with the members of this view
            for(Iterator it = comments.getComments(); it.hasNext(); ) {
                Comment c = (Comment)it.next();
                
                if(c.getTarget().startsWith("class ") ||
                   c.getTarget().startsWith("interface ")) {
                    // we only want to set a class comment on our base class, not for
                    // our supers
                    if (curview == this)
                        setComment(c);
                    continue;
                }

                MemberView m = (MemberView)table.get(c.getTarget());

                if(m == null) {
                    //Debug.message("No member found for " + c.getTarget() + " in file " + filename);
                    continue;
                }
                else {
                    //Debug.message("Found member for " + c.getTarget() + " in file " + filename);
                    m.setComment(c);
                }
            }
        }
    }

    private void addMembers(HashMap table, MemberView[] members)
    {
        for(int i = 0; i < members.length; i++) {
            //Debug.message("Adding member " + members[i].getSignature());
            table.put(members[i].getSignature(), members[i]);
        }
    }

    public String getTypeName()
    {
        return getTypeName(cl);
    }

    static String getTypeName(Class type)
    {
        if(type.isArray())
            {
                try {
                    Class primtype = type;
                    int dimensions = 0;
                    while(primtype.isArray())
                        {
                            dimensions++;
                            primtype = primtype.getComponentType();
                        }
                    StringBuffer sb = new StringBuffer();
                    sb.append(JavaNames.stripPrefix(primtype.getName()));
                    for (int i = 0; i < dimensions; i++)
                        sb.append("[]");
                    return sb.toString();
                } catch (Throwable e) {
                    // ignore it
                }
            }
        return JavaNames.stripPrefix(type.getName());
    }

    public int getInstanceNum()
    {
        return ++instanceNum;
    }

    /**
     ** Get a longer String describing this member
     **/
    public String getLongDesc()
    {
        String desc = Modifier.toString(cl.getModifiers()) + " class " + cl.getName();

        return desc;
    }

    public void print(FormattedPrintWriter out)
    {
        print(out, null);
    }

    public void print(FormattedPrintWriter out, ViewFilter filter)
    {
        // print self
        Comment comment = getComment();
        if(comment != null) {
            comment.print(out);
            out.println("");
        }

        out.setItalic(false);
        out.setBold(true);
        out.println(getLongDesc());

        // start class
        out.setItalic(false);
        out.setBold(false);
        out.println("{");

        // print fields
        out.setItalic(true);
        out.println("fields:");
        out.setItalic(false);
        FieldView fields[] = getAllFields();
        for(int i = 0; i < fields.length; i++)
            if((filter == null) || filter.accept(fields[i])) {
                fields[i].print(out, 1);
            }
        out.println("");

        // print constructors
        out.setItalic(true);
        out.println("constructors:");
        out.setItalic(false);
        ConstructorView constructors[] = getConstructors();
        for(int i = 0; i < constructors.length; i++)
            if((filter == null) || filter.accept(constructors[i])) {
                constructors[i].print(out, 1);
                out.println("");
            }

        // print methods
        out.setItalic(true);
        out.println("methods:");
        out.setItalic(false);
        MethodView methods[] = getAllMethods();
        for(int i = methods.length-1 ; i >= 0; i--)
            if((filter == null) || filter.accept(methods[i])) {
                methods[i].print(out, 1);
                out.println("");
            }

        // end class
        out.setItalic(false);
        out.setBold(false);
        out.println("}");
    }
}
