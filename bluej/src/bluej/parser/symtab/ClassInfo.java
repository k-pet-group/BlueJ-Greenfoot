package bluej.parser.symtab;

import java.util.*;

public final class ClassInfo {

    private static final String[] appletClasses = { "Applet", "JApplet" };

    private String name;
    private String superclass;
    
    private Vector implemented = new Vector();
    private Vector imported = new Vector();
    private Vector used = new Vector();
    private Vector comments = new Vector();

    class SavedComment {

        public String target;   // the method signature of the item we have a
                                // comment for. Can be
                                // class name      or
                                // interface name
                                // in the case of a comment for a whole class/interface        

        public String comment;  // the actual text of the comment
        
        public String paramnames;   // if this is a method or constructor, then
                                    // this is a comma seperated list of name
                                    // associated with the parameters        

        SavedComment(String target, String comment, String paramnames) {
            if (target == null)
                throw new NullPointerException();
            this.target = target;
            this.comment = comment;
            this.paramnames = paramnames;
        }
        
        public void save(Properties p, String prefix)
        {
            p.put(prefix + ".target", target);
            if(comment != null)
                p.put(prefix + ".text", comment);
            if(paramnames != null)
                p.put(prefix + ".params", paramnames);      
        }
    }
        
    private boolean isInterface = false;
    private boolean isAbstract = false;
    private boolean isApplet = false;

    // source positions
    int superClassLine;
    int superClassCol;


    public void setName(String name)
    {
        this.name = name;
    }

    public void setSuperclass(String name)
    {
        if(name.equals(this.name))
            return;
    
        superclass = name;
        if(used.contains(name))
            used.remove(name);
    
        for (int i = 0; i < appletClasses.length; i++) {
            if(name.equals(appletClasses[i]))
            isApplet = true;
        }
    }

    public void addImplements(String name)
    {
        if(name.equals(this.name))
            return;
    
        if(!implemented.contains(name))
            implemented.addElement(name);
    }

    public void addImported(String name)
    {
        if(name.equals(this.name))
            return;
    
        if(!imported.contains(name))
            imported.addElement(name);
    }

    public void addUsed(String name)
    {
        if(name.equals(this.name))
            return;
    
        // don't add predefined types (int, boolean, String, etc)
        if(SymbolTable.getPredefined().contains(name))
            return;
    
        // don't add superclass
        if(name.equals(superclass))
            return;
    
        // don't add if already there
        if(! used.contains(name))
            used.addElement(name);
    }

    public void addComment(String target, String comment)
    {
        addComment(target, comment, null);        
    }
    
    public void addComment(String target, String comment, String paramnames)
    {
            comments.addElement(new SavedComment(target, comment, paramnames));
    }
    
    public void setInterface(boolean b)
    {
        isInterface = b;
    }

    public void setAbstract(boolean b)
    {
        isAbstract = b;
    }

    private Selection classextendsinsertselection;
    private Selection classextendsreplaceselection; 
    private Selection classsuperclassreplaceselection;

    public void setClassExtendsInsertSelection(Selection s) {
        classextendsinsertselection = s;
    }

    public Selection getClassExtendsInsertSelection() {
        return classextendsinsertselection;
    }
    
    public void setClassExtendsReplaceSelection(Selection s) {
        classextendsreplaceselection = s;
    }

    public Selection getClassExtendsReplaceSelection() {
        return classextendsreplaceselection;
    }

    public void setClassSuperClassReplaceSelection(Selection s)
    {
        classsuperclassreplaceselection = s;
    }

    public Selection getClassSuperClassReplaceSelection() {
        return classsuperclassreplaceselection;
    }
        
    // accessors:

    public String getSuperclass()
    {
        return superclass;
    }

    public Vector getImplements()
    {
        return implemented;
    }

    public Vector getImported()
    {
        return imported;
    }

    public Vector getUsed()
    {
        return used;
    }

    public Properties getComments()
    {
        Properties props = new Properties();
        props.put("numComments", String.valueOf(comments.size()));
        Enumeration e = comments.elements();
        for(int i = 0; e.hasMoreElements(); i++)
        {
            SavedComment c = (SavedComment)e.nextElement();
            c.save(props, "comment" + i);
        }
        return props;
    }
    
    public boolean isInterface()
    {
        return this.isInterface;
    }

    public boolean isAbstract()
    {
        return this.isAbstract;
    }

    public boolean isApplet()
    {
        return this.isApplet;
    }


    public void print()
    {
        System.out.println();
        System.out.println("superclass: " + superclass);
    
        System.out.println();
        System.out.println("implements:");
        Enumeration e = implemented.elements();
        while(e.hasMoreElements())
        System.out.println("   " + (String)e.nextElement());

        System.out.println();
        System.out.println("uses:");
        e = used.elements();
        while(e.hasMoreElements())
        System.out.println("   " + (String)e.nextElement());

        System.out.println();
        System.out.println("imports:");
        e = imported.elements();
        while(e.hasMoreElements())
        System.out.println("   " + (String)e.nextElement());
    }
}
