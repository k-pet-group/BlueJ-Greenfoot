package bluej.views;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Comparer;
import bluej.utility.SortableVector;
import bluej.utility.Utility;

import java.lang.reflect.*;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 ** @version $Id: View.java 208 1999-07-23 06:19:40Z ajp $
 ** @author Michael Cahill
 **
 ** View class - a representation of a Java class in BlueJ
 **/
public class View
{
    /** The class that this view is for **/
    protected Class cl;

    protected View superView;
    protected View[] interfaceViews;
    
    protected FieldView[] fields;
    protected FieldView[] allFields;
    protected ConstructorView[] constructors;
    protected MethodView[] methods;
    protected MethodView[] allMethods;
    
    protected Comment comment;
    private int instanceNum = 0;
    
    protected static Hashtable views = new Hashtable();
    
    public static View getView(Class cl)
    {
        if(cl == null)
            return null;
                
        // Debug.message("Started getView for class " + cl);
                
        View v = (View)views.get(cl);
        if(v == null)
        {
            v = new View(cl);
            views.put(cl, v);
        }
                
        // Debug.message("Ended getView for class " + cl);
            
        return v;
    }
    
    public View(Class cl)
    {
        this.cl = cl;
    }
    
    public String getName()
    {
        return cl.getName();
    }
    
    public View getSuper()
    {
        if(superView == null)
            superView = getView(cl.getSuperclass());
        return superView;
    }
    
    public View[] getInterfaces()
    {
        if(interfaceViews == null)
        {
            Class[] interfaces = cl.getInterfaces();
            interfaceViews = new View[interfaces.length];
            for(int i = 0; i < interfaces.length; i++)
                interfaceViews[i] =  getView(interfaces[i]);
        }
            
        return interfaceViews;
    }
    
    public final boolean isInterface()
    {
        return cl.isInterface();
    }
    
    /**
     ** Walk superclasses + interfaces for methods.
     ** All methods are inherited (+ overridden) from everywhere.
     **/
    public MethodView[] getAllMethods()
    {
        if(allMethods == null)
        {
            Hashtable hashtable = new Hashtable();
            getAllMethods(hashtable, 0);
            SortableVector v = new SortableVector();
            for(Enumeration e = hashtable.elements(); e.hasMoreElements(); )
                v.addElement(e.nextElement());
            v.sort(new ElementComparer());
                
            int numMethods = v.size();
            allMethods = new MethodView[numMethods];
            for(int i = 0; i < numMethods; i++)
            {
                MemberElement elem = (MemberElement)v.elementAt(i);
                allMethods[i] = (MethodView)elem.member;
                    // if(allMethods[i] == null)
                //Debug.message("Warning: getAllMethods - entry == null");
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
        if(allFields == null)
        {
            Hashtable hashtable = new Hashtable();
            getAllFields(hashtable, 0);
            SortableVector v = new SortableVector();
            for(Enumeration e = hashtable.elements(); e.hasMoreElements(); )
                v.addElement(e.nextElement());
            v.sort(new ElementComparer());
                
            int numFields = v.size();
            allFields = new FieldView[numFields];
            for(int i = 0; i < numFields; i++)
            {
                MemberElement elem = (MemberElement)v.elementAt(i);
                allFields[i] = (FieldView)elem.member;
            }
        }
            
        return allFields;
    }
    
    /**
     ** (Attempt at an) efficient implementation of getAllMethods + getAllFields
     ** The old version had shocking performance - this one uses a Hashtable
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
    
    class ElementComparer implements Comparer
    {
    /** Return { -1, 0, 1 } to represent <a> { <, ==, > } <b> **/
    public final int cmp(Object a, Object b)
    {
        int cmp = ((MemberElement)a).index - ((MemberElement)b).index;
            
        return (cmp < 0) ? -1 : ((cmp > 0) ? 1 : 0);
    }
    }
    
    protected int getAllMethods(Hashtable h, int methnum)
    {
    if(allMethods != null)
        {
        // carefully copy from allMethods into h
        methnum = addMembers(h, allMethods, methnum);
        return methnum;
        }
            
    // otherwise, do the real work
    // carefully copy local methods into v
        
    View sView = getSuper();
    if(sView != null)
        methnum = sView.getAllMethods(h, methnum);
        
    if(isInterface())
        {
        View[] ifaces = getInterfaces();
        for(int i = 0; i < ifaces.length; i++)
            methnum = ifaces[i].getAllMethods(h, methnum);
        }
        
    methnum = addMembers(h, getDeclaredMethods(), methnum);
    return methnum;
    }
    
    protected int getAllFields(Hashtable h, int fieldnum)
    {
    if(allFields != null)
        {
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
    
    private int addMembers(Hashtable h, MemberView[] members, int num)
    {
    //Debug.message("Started addMembers for " + cl);
        
    for(int i = members.length - 1; i >= 0; i--)
        h.put(members[i].toString(), new MemberElement(num++, members[i]));
        
    //Debug.message("Ended addMembers for " + cl);
    return num;
    }
    
    public MethodView[] getDeclaredMethods()
    {
    if(methods == null)
        {
        Method[] cl_methods = cl.getDeclaredMethods();
        methods = new MethodView[cl_methods.length];
            
        for(int i = 0; i < methods.length; i++) {
            methods[i] = new MethodView(this, cl_methods[i]);
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
        
        CommentList comments = null;
        
        try {
            String filename = getName().replace('.', '/') + ".ctxt";

            InputStream in = null;
            
            if (cl.getClassLoader() == null)
                in = ClassLoader.getSystemResourceAsStream(filename);
            else
                in = cl.getClassLoader().getResourceAsStream(filename);

            if(in != null)
            {
                comments = new CommentList();
                comments.load(in);
    
                in.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
            
        if(comments == null) {
            return;
        }
        
        // match the comments against this view's members
        // -> put all members into a hashtable indexed by
        // <member>.getSignature() (== <comment>.getTarget())
        Hashtable table = new Hashtable();
        addMembers(table, getDeclaredFields());
        addMembers(table, getConstructors());
        addMembers(table, getDeclaredMethods());
            
        // match up the comments read from the file with the members of this view
        for(Enumeration e = comments.getComments(); e.hasMoreElements(); ) {
            Comment c = (Comment)e.nextElement();
                
            if(c.getTarget().startsWith("class ") ||
               c.getTarget().startsWith("interface ")) {
                setComment(c);
                continue;
            }
                
            MemberView m = (MemberView)table.get(c.getTarget());
                    
            if(m == null) {
                // Debug.message("No member found for " + c.getTarget());
                continue;
            }
            else {
                // Debug.message("Found member for " + c.getTarget());
                m.setComment(c);
            }
        }
    }
    
    private void addMembers(Hashtable table, MemberView[] members)
    {
        for(int i = 0; i < members.length; i++) {
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
            sb.append(Utility.stripPackagePrefix(primtype.getName()));
            for (int i = 0; i < dimensions; i++)
            sb.append("[]");
            return sb.toString();
        } catch (Throwable e) {
                // ignore it
        }
        }
        return Utility.stripPackagePrefix(type.getName());
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
        if(comment != null)
            comment.print(out);
    
        out.setItalic(false);
        out.setBold(true);
        out.println(getLongDesc());
            
        // start class
        out.setItalic(false);
        out.setBold(false);
        out.println("{");
            
        // print fields
        FieldView fields[] = getAllFields();
        for(int i = 0; i < fields.length; i++)
            if((filter == null) || filter.accept(fields[i]))
            {
                fields[i].print(out, 1);
                out.println("");
            }
                
        // print constructors
        ConstructorView constructors[] = getConstructors();
        for(int i = 0; i < constructors.length; i++)
            if((filter == null) || filter.accept(constructors[i]))
            {
                constructors[i].print(out, 1);
                out.println("");
            }
                
        // print methods
        MethodView methods[] = getAllMethods();
        for(int i = 0; i < methods.length; i++)
            if((filter == null) || filter.accept(methods[i]))
            {
                methods[i].print(out, 1);
                out.println("");
            }
            
        // end class
        out.setItalic(false);
        out.setBold(false);
        out.println("}");
    }
}
