package sun.tools.javadoc;

import bluej.utility.Debug;
import bluej.views.Comment;
import bluej.views.CommentList;

import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import sun.tools.javac.BatchEnvironment;
import sun.tools.java.*;
import sun.tools.tree.LocalField;
import sun.tools.util.ModifierFilter;

/**
 ** BlueJDocumentationGenerator class - generates BlueJ documentation from
 ** Java sources
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **/

public class BlueJDocumentationGenerator extends sun.tools.javadoc.DocumentationGenerator
{
	Vector prefixes = new Vector();
	boolean humanReadable = false;	// whether to generate "human readable" type names
	
	BlueJDocumentationGenerator()
	{
		addPrefix("java.lang.");
	}
	
	public BlueJDocumentationGenerator(BatchEnvironment env)
	{
		this();
		this.env = env;
		Main.showAuthors = true;
		Main.showVersion = true;
		Main.showAccess = new ModifierFilter(ModifierFilter.ALL_ACCESS);
	}
	
	public void addPrefix(String prefix)
	{
		prefixes.addElement(prefix);
	}
	
	String classString(ClassDeclaration c)
	{
		String classname = c.getName().toString();
		
		if(humanReadable)
		{
			for(Enumeration e = prefixes.elements(); e.hasMoreElements(); )
			{
				String prefix = (String)e.nextElement();
	
				if(classname.startsWith(prefix))
				{
					classname = classname.substring(prefix.length());
					break;
				}
			}
		}
			
		return classname;
	}
	String commentString(String str)	{ return str; }
	String returnString(String str)	{ return str; }
	String versionString(String str)	{ return str; }
	String authorString(String str)	{ return str; }
	
	void genClassDocumentation(ClassDeclaration decl, ClassDeclaration prev, ClassDeclaration next)
	{
		ClassDefinition c;
		try {
			c = getClassDefinition(decl);
		} catch(ClassNotFound e) {
			// Debug.message("Class not found for " + decl);
			return;
		}
		
		CommentList comments = genComments(c);
		
		Properties props = new Properties();
		props.put("numComments", String.valueOf(comments.numComments()));
		Enumeration e = comments.getComments();
		for(int i = 0; e.hasMoreElements(); i++)
		{
			Comment comment = (Comment)e.nextElement();
			comment.save(props, "comment" + i);
		}
		
		try {
			OutputStream out = openStream(getReferenceName(c.getName()) + ".ctxt");
			props.save(out, "BlueJ class context for class " + c);
			out.close();
		} catch (IOException ex) {
			throw new Error(Main.getText("doc.Can_not_open_output_file"));
		}
	}
	
	/**
	 ** Walk over the Sun internal structures built up by the parser and extract the
	 ** context needed by BlueJ
	 **/
	public CommentList genComments(ClassDefinition c)
	{
		ClassDeclaration decl = c.getClassDeclaration();
		// Debug.message("Generating documentation for " + decl);
		Vector comments = new Vector();
		
		String cdoc = c.getDocumentation();
		if(cdoc != null)
		{
			Vector mergeDoc = mergeDoc(cdoc);
		
			Comment classComment = new Comment();
			classComment.setTarget("class " + c.getName().toString());
			classComment.setText(getComment(mergeDoc));
			classComment.setAuthors(getAuthors(mergeDoc));
			classComment.setVersion(getVersion(mergeDoc));
			classComment.setDeprecation(getDeprecated(mergeDoc));

			Vector seeAlso = getSees(mergeDoc, decl);
			handleSeeAlso(classComment, seeAlso);
			
			comments.addElement(classComment);
		}
		
		Vector variables = allVariables(c);
		variables = localFieldsOf(c, variables);
		genVariableComments(comments, variables, decl);
		
		Vector constructors = allConstructors(c);
		constructors = localFieldsOf(c, constructors);
		genConstructorComments(comments, constructors, decl);
		
		Vector methods = allMethods(c);
		methods = localFieldsOf(c, methods);
		genMethodComments(comments, methods, decl);
		
		return new CommentList(comments);
	}
	
	void genVariableComments(Vector comments, Vector variables, ClassDeclaration decl)
	{
		for(int i = 0; i < variables.size(); i++)
		{
			FieldDefinition var = (FieldDefinition)variables.elementAt(i);
			
			String doc = var.getDocumentation();
			
			if (doc == null)	// no comment for this variable
				continue;
				
			Vector mergeDoc = mergeDoc(doc);
		
			Comment comment = new Comment();
			comment.setTarget(makeVarTarget(var, decl));
			
			comment.setText(getComment(mergeDoc));
			comment.setAuthors(getAuthors(mergeDoc));
			comment.setVersion(getVersion(mergeDoc));
			comment.setDeprecation(getDeprecated(mergeDoc));

			Vector seeAlso = getSees(mergeDoc, decl);
			handleSeeAlso(comment, seeAlso);
			
			comments.addElement(comment);
		}
	}
	
	void genConstructorComments(Vector comments, Vector constructors, ClassDeclaration decl)
	{
		for(int i = 0; i < constructors.size(); i++)
		{
			FieldDefinition cons = (FieldDefinition)constructors.elementAt(i);
			
			String doc = cons.getDocumentation();
			
			Comment comment = new Comment();
			comment.setTarget(makeConsTarget(cons, decl));
			comment.setShortDesc(makeConsShortDesc(cons, decl));
			comment.setLongDesc(makeConsLongDesc(cons, decl));
			
			if (doc != null)	// no comment for this variable
			{
				Vector mergeDoc = mergeDoc(doc);
		
				comment.setText(getComment(mergeDoc));
				comment.setAuthors(getAuthors(mergeDoc));
				comment.setVersion(getVersion(mergeDoc));
				comment.setDeprecation(getDeprecated(mergeDoc));

				Vector seeAlso = getSees(mergeDoc, decl);
				handleSeeAlso(comment, seeAlso);
			}
			
			comments.addElement(comment);
		}
	}
	
	void genMethodComments(Vector comments, Vector methods, ClassDeclaration decl)
	{
		for(int i = 0; i < methods.size(); i++)
		{
			FieldDefinition meth = (FieldDefinition)methods.elementAt(i);
			
			String doc = meth.getDocumentation();
			
			Comment comment = new Comment();
			comment.setTarget(makeMethTarget(meth, decl));
			comment.setShortDesc(makeMethShortDesc(meth, decl));
			comment.setLongDesc(makeMethLongDesc(meth, decl));
			
			if (doc != null)	// no comment for this variable
			{
				Vector mergeDoc = mergeDoc(doc);
			
				comment.setText(getComment(mergeDoc));
				comment.setAuthors(getAuthors(mergeDoc));
				comment.setVersion(getVersion(mergeDoc));
				comment.setDeprecation(getDeprecated(mergeDoc));

				Vector seeAlso = getSees(mergeDoc, decl);
				handleSeeAlso(comment, seeAlso);
			}
			
			comments.addElement(comment);
		}
	}
	
	String makeVarTarget(FieldDefinition var, ClassDeclaration decl)
	{
		// Should match java.lang.reflect.Field.toString()
		int mod = var.getModifiers();
		return ((mod == 0) ? "" : (Modifier.toString(mod) + " "))
			+ var.getType().typeString(decl.getName().toString()+ "."
				+ var.getName());
	}
	
	String makeConsTarget(FieldDefinition cons, ClassDeclaration decl)
	{
		try {
			StringBuffer sb = new StringBuffer();
			int mod = cons.getModifiers();
			if(mod != 0)
			{
				sb.append(Modifier.toString(mod));
				sb.append(" ");
			}
			sb.append(decl.getName().toString());
			appendArgs(sb, cons, true, false);
			appendExceptions(sb, cons, false);
			return sb.toString();
		} catch (Exception e) {
			return "<" + e + ">";
		}
	}
	
	/**
	 ** Construct a String that is a short representation of a constructor
	 **/
	String makeConsShortDesc(FieldDefinition cons, ClassDeclaration decl)
	{
		String ret = null;
		humanReadable = true;	// generate "human readable" class names
		
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(classString(decl));
			appendArgs(sb, cons, false, true);
			ret = sb.toString();
		} catch (Exception e) {
			// ignore it
		}
		
		humanReadable = false;
		return ret;
	}
	
	/**
	 ** Construct a String that is a longer representation of a constructor
	 **/
	String makeConsLongDesc(FieldDefinition cons, ClassDeclaration decl)
	{
		String ret = null;
		humanReadable = true;	// generate "human readable" class names
		
		try {
			StringBuffer sb = new StringBuffer();
			int mod = cons.getModifiers();
			if(mod != 0)
			{
				sb.append(Modifier.toString(mod));
				sb.append(" ");
			}
			sb.append(classString(decl));
			appendArgs(sb, cons, true, true);
			appendExceptions(sb, cons, true);
			ret = sb.toString();
		} catch (Exception e) {
			// ignore it
		}
		
		humanReadable = false;
		return ret;
	}
	
	String makeMethTarget(FieldDefinition meth, ClassDeclaration decl)
	{
		try {
			StringBuffer sb = new StringBuffer();
			int mod = meth.getModifiers();
			if(mod != 0)
			{
				sb.append(Modifier.toString(mod));
				sb.append(" ");
			}
			sb.append(typeString(meth.getType().getReturnType()));
			sb.append(" ");
			sb.append(classString(decl));
			sb.append(".");
			sb.append(meth.getName());
			appendArgs(sb, meth, true, false);
			appendExceptions(sb, meth, false);
			return sb.toString();
		} catch (Exception e) {
			return "<" + e + ">";
		}
	}
	
	/**
	 ** Construct a String that is a short representation of a method
	 **/
	String makeMethShortDesc(FieldDefinition meth, ClassDeclaration decl)
	{
		String ret = null;
		humanReadable = true;	// generate "human readable" class names
		
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(typeString(meth.getType().getReturnType()) + " ");
			sb.append(meth.getName());
			appendArgs(sb, meth, false, true);
			ret = sb.toString();
		} catch (Exception e) {
			// ignore it
		}
		
		humanReadable = false;
		return ret;
	}
	
	/**
	 ** Construct a String that is a longer representation of a method
	 **/
	String makeMethLongDesc(FieldDefinition meth, ClassDeclaration decl)
	{
		String ret = null;
		humanReadable = true;	// generate "human readable" class names
		
		try {
			StringBuffer sb = new StringBuffer();
			int mod = meth.getModifiers();
			if(mod != 0)
			{
				sb.append(Modifier.toString(mod));
				sb.append(" ");
			}
			sb.append(typeString(meth.getType().getReturnType()) + " ");
			sb.append(meth.getName());
			appendArgs(sb, meth, true, true);
			appendExceptions(sb, meth, true);
			ret = sb.toString();
		} catch (Exception e) {
			// ignore it
		}
		
		humanReadable = false;
		return ret;
	}
	
	void appendArgs(StringBuffer sb, FieldDefinition meth, boolean doTypes, boolean doNames)
	{
		sb.append("(");
		
		Enumeration e = meth.getArguments().elements();
		if (!meth.isStatic())
			e.nextElement();		// skip "this" argument
			
		Type args[] = meth.getType().getArgumentTypes();
		for(int i = 0; i < args.length; i++)
		{
			if(doTypes)
				sb.append(typeString(args[i]));
				
			if(doTypes && doNames)
				sb.append(" ");
				
			if(doNames)
			{
				LocalField l = (LocalField)e.nextElement();
				sb.append(l.getName());
			}
			
			if (i < args.length - 1)
				sb.append(doNames ? ", " : ",");
		}
		
		sb.append(")");
	}
	
	void appendExceptions(StringBuffer sb, FieldDefinition meth, boolean longForm)
	{
		ClassDeclaration[] exceptions = meth.getExceptions(env);
		if (exceptions.length > 0)
		{
			sb.append(" throws ");
			for(int i = 0; i < exceptions.length; i++)
			{
				sb.append(exceptions[i].getName().toString());
				if(i < exceptions.length - 1)
					sb.append(longForm ? ", " : ",");
			}
		}
	}
	
	void genPackagesDocumentation(String v1[], String v2[])
	{
		// Not relevant to BlueJ
	}
	
	void genPackageDocumentation(Identifier pkg, 
		ClassDeclaration intfDecls[], ClassDeclaration classDecls[],
		ClassDeclaration exceptDecls[], ClassDeclaration errorDecls[])
	{
		// Not relevant to BlueJ
	}
	
	void genFieldIndex()
	{
		// Not relevant to BlueJ
	}
	
	void genClassTree(Hashtable tree, ClassDeclaration objectDecl)
	{
		// Not relevant to BlueJ
	}

	protected void handleSeeAlso(Comment comment, Vector seeAlso)
	{
		if(seeAlso != null)
			for(Enumeration e = seeAlso.elements(); e.hasMoreElements(); )
			{
				ClassDeclaration ref = (ClassDeclaration)(e.nextElement());

				String fieldName = (String)(e.nextElement());
				String what = (String)(e.nextElement());
					
				comment.addReference(ref.getName().toString(), fieldName);
			}
	}

	protected OutputStream openStream(String name)
	{
		if (Main.destDir != null) {
			name = Main.destDir.getPath() + File.separator + name;
		}

		FileOutputStream file;
		try {
			String parent = new File(name).getParent();
			if (parent != null) {
				File parentDir = new File(parent);
				if(! parentDir.exists())
					parentDir.mkdirs();
			}
			file = new FileOutputStream(name);
		} catch (IOException ex) {
			throw new Error(Main.getText("doc.Can_not_open_output_file"));
		}
		
		return file;
	}
}
