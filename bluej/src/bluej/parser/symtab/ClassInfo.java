package bluej.parser.symtab;

import java.util.Vector;
import java.util.Enumeration;

public final class ClassInfo {

    private static final String[] appletClasses = { "Applet", "JApplet" };

    private String superclass;
    private Vector implemented = new Vector();
    private Vector imported = new Vector();
    private Vector used = new Vector();
    private boolean isInterface = false;
    private boolean isAbstract = false;
    private boolean isApplet = false;

    public void setSuperclass(String name)
    {
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
	if(!implemented.contains(name))
	    implemented.addElement(name);
    }

    public void addImported(String name)
    {
	if(!imported.contains(name))
	    imported.addElement(name);
    }

    public void addUsed(String name)
    {
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

    public void setInterface(boolean b)
    {
	isInterface = b;
    }

    public void setAbstract(boolean b)
    {
	isAbstract = b;
    }


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
