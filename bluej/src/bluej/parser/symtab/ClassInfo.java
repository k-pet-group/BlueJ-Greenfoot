package bluej.parser.symtab;

import java.util.*;

import bluej.utility.SortedProperties;

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
                p.setProperty(prefix + ".text", comment);
            if(paramnames != null)
                p.setProperty(prefix + ".params", paramnames);      
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
	// remove asterisks (*) from beginning of comment

	if(comment != null) {
	    StringBuffer finalComment = new StringBuffer(comment.length());
	    StringTokenizer tokenizer = new StringTokenizer(comment,"\n\r\f");

	    while(tokenizer.hasMoreTokens()) {
		StringBuffer line = new StringBuffer(tokenizer.nextToken());
		char ch = (line.length() > 0 ? line.charAt(0) : 'x');
		while(ch == ' ' || ch == '\t' || ch == '*') {
		    line.deleteCharAt(0);
		    ch = (line.length() > 0 ? line.charAt(0) : 'x');
		}
		finalComment.append(line);
		finalComment.append('\n');
	    }
	    comment = finalComment.toString();
	}
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

    /**
     * If we have parsed the definition of the main class or
     * interface for this file then we switch off processing of
     * any more src locations in the file.. this protects us from
     * picking up inner class, private classes etc defined
     * later on in the file
     */
    private boolean parsedfileheader = false;

    public void setParsedFileHeader(boolean status)
    {
        this.parsedfileheader = status;
    }

    // where we would insert the string "extends" in a class
    private Selection classextendsinsertselection;
    // how we would replace the string "extends" in a class
    private Selection classextendsreplaceselection; 
    // how we would replace the superclass name in a class
    private Selection classsuperclassreplaceselection;
    // where we would insert the string "implements" in a class
    private Selection classimplementsinsertselection;

    // where we would insert the string "extends" in an interface
    private Selection interfaceextendsinsertselection;

    // a vector of Selections (and texts) of all the elements in a classes
    // "implements" clause
    // ie "implements" "InterfaceA" "," "InterfaceB"
    // or null if there is no "implements" clause
    private Vector classimplementsselections;
    private Vector classimplementstexts;

    // a vector of Selections (and texts) of all the elements in an interfaces
    // "extends" clause
    // ie "extends" "InterfaceA" "," "InterfaceB"
    // or null if there is no "extends" clause
    private Vector interfaceextendsselections;
    private Vector interfaceextendstexts;

    public void setClassExtendsInsertSelection(Selection s) {
        if(!parsedfileheader)
            classextendsinsertselection = s;
    }

    public Selection getClassExtendsInsertSelection() {
        return classextendsinsertselection;
    }
    
    public void setClassExtendsReplaceSelection(Selection s) {
        if(!parsedfileheader)
            classextendsreplaceselection = s;
    }

    public Selection getClassExtendsReplaceSelection() {
        return classextendsreplaceselection;
    }

    public void setClassSuperClassReplaceSelection(Selection s)
    {
        if(!parsedfileheader)
            classsuperclassreplaceselection = s;
    }

    public Selection getClassSuperClassReplaceSelection() {
        return classsuperclassreplaceselection;
    }

    public void setClassImplementsInsertSelection(Selection s) {
        if(!parsedfileheader)
            classimplementsinsertselection = s;
    }

    public Selection getClassImplementsInsertSelection() {
        return classimplementsinsertselection;
    }

    public void setInterfaceExtendsInsertSelection(Selection s) {
        if(!parsedfileheader)
            interfaceextendsinsertselection = s;
    }

    public Selection getInterfaceExtendsInsertSelection() {
        return interfaceextendsinsertselection;
    }

    public void setClassImplementsSelections(Vector sels, Vector texts) {
        if(!parsedfileheader) {
            classimplementsselections = sels;
            classimplementstexts = texts;
        }
    }

    public Vector getClassImplementsSelections() {
        return classimplementsselections;
    }

    public Vector getClassImplementsTexts() {
        return classimplementstexts;
    }

    public boolean hasClassImplementsSelections() {
        return classimplementsselections != null;
    }

    public void setInterfaceExtendsSelections(Vector sels, Vector texts) {
        if(!parsedfileheader) {
            interfaceextendsselections = sels;
            interfaceextendstexts = texts;
        }
    }

    public Vector getInterfaceExtendsSelections() {
        return interfaceextendsselections;
    }

    public Vector getInterfaceExtendsTexts() {
        return interfaceextendstexts;
    }

    public boolean hasInterfaceExtendsSelections() {
        return interfaceextendsselections != null;
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
        Properties props = new SortedProperties();
        props.setProperty("numComments", String.valueOf(comments.size()));
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
