package bluej.parser.symtab;

import java.util.*;

import bluej.utility.SortedProperties;

public final class ClassInfo
{
    private static final String[] appletClasses = { "Applet", "JApplet" };
    private static final String[] unitTestClasses = { "TestCase", "junit.framework.TestCase" };

    private boolean foundClass = false, foundPublicClass = false;

    private String name;
    private String superclass;

    private List implemented = new ArrayList();
    private List imported = new ArrayList();
    private List used = new ArrayList();
    private List comments = new LinkedList();
    
    private List typeParameterSelections;
    private List typeParameterTexts;
    private Selection typeParameterText = new Selection(null,1,1);

    private class SavedComment
    {
        public String target;   // the method signature of the item we have a
                                // comment for. Can be
                                // class name      or
                                // interface name
                                // in the case of a comment for a whole class/interface

        public String comment;  // the actual text of the comment

        public String paramnames;   // if this is a method or constructor, then
                                    // this is a comma seperated list of name
                                    // associated with the parameters

        public SavedComment(String target, String comment, String paramnames)
        {
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
    private boolean isUnitTest = false;
    private boolean isEnum = false;

    public boolean foundClass()
    {
        return foundClass;
    }

    public boolean foundPublicClass()
    {
        return foundPublicClass;
    }

    /**
     * Set the name of the class.
     */
    public void setName(String name, boolean pub)
    {
        this.name = name;

        foundClass = true;

        if(pub)
            foundPublicClass = true;
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

        for (int i = 0; i < unitTestClasses.length; i++) {
            if(name.equals(unitTestClasses[i]))
                isUnitTest = true;
        }
    }
    
    public void setEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    public void addImplements(String name)
    {
        if(name.equals(this.name))
            return;

        if(!implemented.contains(name))
            implemented.add(name);
    }
    
    public void addTypeParameter(String paramName)
    {
        typeParameterSelections.add(name);
    }

    public void addImported(String name)
    {
        if(name.equals(this.name))
            return;

        if(!imported.contains(name))
            imported.add(name);
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
            used.add(name);
    }

    public void addComment(String target, String comment)
    {
        addComment(target, comment, null);
    }

    public void addComment(String target, String comment, String paramnames)
    {
        // remove asterisks (*) from beginning of comment

        // a valid comment must being with /* and end with */ so we have
        // at least 4 characters

        if(comment != null && comment.length() > 4) {
            comment = comment.substring(2, comment.length()-2);

            StringBuffer finalComment = new StringBuffer(comment.length());
            StringTokenizer tokenizer = new StringTokenizer(comment,"\n\r\f");

            while(tokenizer.hasMoreTokens()) {
                StringBuffer line = new StringBuffer(tokenizer.nextToken());
                char ch = (line.length() > 0 ? line.charAt(0) : 'x');
                while(ch == ' ' || ch == '\t' || ch == '*') {
                    line.deleteCharAt(0);
                    ch = (line.length() > 0 ? line.charAt(0) : 'x');
                }
                finalComment.append(line.toString());
                finalComment.append('\n');
            }
            comment = finalComment.toString();
        }
        comments.add(new SavedComment(target, comment, paramnames));
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
     * Where we would insert the string "extends" in a class/interface
     */
    private Selection extendsInsertSelection;

    /**
     * Record where we would insert the string "extends" in a class/interface
     *
     * @param s the Selection object which records a location to
     *          insert the "extends" keyword
     */
    public void setExtendsInsertSelection(Selection s)
    {
        extendsInsertSelection = s;
    }

    /**
     * Returns where we would insert the string "extends" in a class/interface
     *
     * @returns s the Selection object which records a location to
     *          insert the "extends" keyword
     */
    public Selection getExtendsInsertSelection() {
        return extendsInsertSelection;
    }

    /*
     * Where we would insert the string "implements" in a class
     */
    private Selection implementsInsertSelection;

    public void setImplementsInsertSelection(Selection s)
    {
        implementsInsertSelection = s;
    }

    public Selection getImplementsInsertSelection()
    {
        return implementsInsertSelection;
    }

    /**
     * Record how we would replace the string "extends" in a class
     *
     * @param s the Section object which records the location of
     *          the "extends" keyword for a class
     */
    public void setExtendsReplaceSelection(Selection s)
    {
        extendsReplaceSelection = s;
    }

    public Selection getExtendsReplaceSelection()
    {
        return extendsReplaceSelection;
    }

    private Selection extendsReplaceSelection;


    // how we would replace the superclass name in a class
    private Selection superReplaceSelection;

    public void setSuperReplaceSelection(Selection s)
    {
        superReplaceSelection = s;
    }

    public Selection getSuperReplaceSelection()
    {
        return superReplaceSelection;
    }

    // a vector of Selections of all the elements in a classes
    // "implements" clause
    // ie "implements" "InterfaceA" "," "InterfaceB"
    // or a interfaces "extends" clause
    // ie "extends" "InterfaceA" "," "InterfaceB"
    // or null if there is no clause
    private List interfaceSelections;
    private List interfaceTexts;

    public void setInterfaceSelections(List selections)
    {
        interfaceSelections = selections;

        interfaceTexts = new ArrayList();

        Iterator it = interfaceSelections.iterator();
        while(it.hasNext()) {
            Selection s = (Selection)it.next();
            // we don't want the interface texts to show type argument info
            // which may be present in the selections. The texts are used to
            // match to the base name of a ClassTarget when dependencies are
            // removed. It would probably be nicer to bring these back from
            // the parser at a lower level. The interfaces however start to
            // get even more cluttered...
            String sel = s.getText();
            int index = sel.indexOf("<");
            if(index > 0)
                sel = sel.substring(0, index);
            interfaceTexts.add(sel);
        }
    }
    
    public void setTypeParameterSelections(List selections)
    {
        typeParameterSelections = selections;
    }
    
    public List getTypeParameterTexts()
    {
        return typeParameterTexts;
    }


    public List getInterfaceSelections()
    {
        return interfaceSelections;
    }

    public List getInterfaceTexts()
    {
        return interfaceTexts;
    }

    public boolean hasInterfaceSelections()
    {
        return (interfaceSelections != null) &&
                (interfaceSelections.size() > 0);
    }

    /**
     * Record the locations of the tokens in a source files "package" statement.
     *
     * These locations start off at the first line and column of a file.
     * If a package line exists, they are updated, otherwise they are
     * left pointing the very start of the file (which is where we would
     * want to insert a package line if we were to add one)
     */
    private boolean packageStatementExists = false;
    private Selection packageStatementSelection = new Selection(null,1,1);
    private Selection packageNameSelection = new Selection(null,1,1);
    private Selection packageSemiSelection = new Selection(null,1,1);
    private String packageName = "";

    public void setPackageSelections(Selection pkgStatement, Selection pkgName, String pkgNameText,
                                        Selection pkgSemi)
    {
        packageStatementSelection = pkgStatement;
        packageNameSelection = pkgName;
        packageName = pkgNameText;
        packageSemiSelection = pkgSemi;

        packageStatementExists = true;
    }

    public boolean hasPackageStatement()
    {
        return packageStatementExists;
    }

    public Selection getPackageStatementSelection()
    {
        return packageStatementSelection;
    }

    public Selection getPackageNameSelection()
    {
        return packageNameSelection;
    }

    public Selection getPackageSemiSelection()
    {
        return packageSemiSelection;
    }

    public String getPackage()
    {
        return packageName;
    }



    // accessors:

    public String getSuperclass()
    {
        return superclass;
    }

    public String getName()
    {
        return name;
    }

    public List getImplements()
    {
        return implemented;
    }

    public void setTypeParameterText(Selection s)
    {
        typeParameterText = s;
    }
    
    public boolean hasTypeParameter()
    {
        return (typeParameterText != null);
    }
    
    public Selection getTypeParameterText()
    {
        return typeParameterText;
    }
    
    public void setTypeParameterSelections(Selection s)
    {
        typeParameterText = s;
    }
    
    public List getImported()
    {
        return imported;
    }

    public List getUsed()
    {
        return used;
    }

    public Properties getComments()
    {
        Properties props = new SortedProperties();
        props.setProperty("numComments", String.valueOf(comments.size()));
        Iterator it = comments.iterator();
        for(int i = 0; it.hasNext(); i++)
        {
            SavedComment c = (SavedComment)it.next();
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

    public boolean isUnitTest()
    {
        return this.isUnitTest;
    }
    
    public boolean isEnum()
    {
        return this.isEnum;
    }


    public void print()
    {
        System.out.println();
        System.out.println("superclass: " + superclass);

        System.out.println();
        System.out.println("implements:");
        Iterator it = implemented.iterator();
        while(it.hasNext())
        System.out.println("   " + (String)it.next());

        System.out.println();
        System.out.println("uses:");
        it = used.iterator();
        while(it.hasNext())
        System.out.println("   " + (String)it.next());

        System.out.println();
        System.out.println("imports:");
        it = imported.iterator();
        while(it.hasNext())
        System.out.println("   " + (String)it.next());
    }
}
