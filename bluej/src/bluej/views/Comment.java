package bluej.views;

import bluej.utility.Utility;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 ** @version $Id: Comment.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** Comment class - the source information associated with a class or field
 **/
public final class Comment
{
	private String target;		// identifies what this comment is for
	private String text;
	private String authors;
	private String version;
	private Vector refs;
	private String deprecation;
	private String shortDesc;	// Short description of the target
	private String longDesc;	// Longer description of the target
	
	public void load(Properties p, String prefix)
	{
		target = p.getProperty(prefix + ".target", "<no target>");
		text = p.getProperty(prefix + ".text");
		authors = p.getProperty(prefix + ".authors");
		version = p.getProperty(prefix + ".version");
		deprecation = p.getProperty(prefix + ".deprecation");
		shortDesc = p.getProperty(prefix + ".shortDesc");
		longDesc = p.getProperty(prefix + ".longDesc");
	}
	
	public void save(Properties p, String prefix)
	{
		if(target != null)
			p.put(prefix + ".target", target);
		if(text != null)
			p.put(prefix + ".text", text);
		if(authors != null)
			p.put(prefix + ".authors", authors);
		if(version != null)
			p.put(prefix + ".version", version);
		if(deprecation != null)
			p.put(prefix + ".deprecation", deprecation);
		if(shortDesc != null)
			p.put(prefix + ".shortDesc", shortDesc);
		if(longDesc != null)
			p.put(prefix + ".longDesc", longDesc);
	}
	
	public void setTarget(String target)
	{
		this.target = target;
	}
	
	public String getTarget()
	{
		return target;
	}
	
	public void setText(String text)
	{
		this.text = text;
	}
	
	public String getText()
	{
		return text;
	}
	
	public void setAuthors(String authors)
	{
		this.authors = authors;
	}
	
	public String getAuthors()
	{
		return authors;
	}
	
	public void setVersion(String version)
	{
		this.version = version;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	public void setDeprecation(String deprecation)
	{
		this.deprecation = deprecation;
	}
	
	public String getDeprecation()
	{
		return deprecation;
	}
	
	public void setShortDesc(String shortDesc)
	{
		this.shortDesc = shortDesc;
	}
	
	public String getShortDesc()
	{
		return (shortDesc != null) ? shortDesc : getTarget();
	}
	
	public void setLongDesc(String longDesc)
	{
		this.longDesc = longDesc;
	}
	
	public String getLongDesc()
	{
		return (longDesc != null) ? longDesc : getShortDesc();
	}
	
	public void addReference(String classname, String fieldname)
	{
		// refs.addElement(new Reference(classname, fieldname));
	}
	
	public Enumeration getReferences()
	{
		return refs.elements();
	}
	
	public void print(FormattedPrintWriter out)
	{
		print(out, "");
	}
	
	public void print(FormattedPrintWriter out, String prefix)
	{
		if((text != null) || (authors != null) || (version != null) || (deprecation != null))
		{
			out.setBold(false);
			out.setItalic(true);
			if(text != null)
			{
				String[] lines = Utility.splitLines(text);
		
				for(int i = 0; i < lines.length; i++)
					out.println(prefix + lines[i]);
			}
			if(authors != null)
				out.println(prefix + "@author " + authors);
			if(version != null)
				out.println(prefix + "@version " + version);
			if(deprecation != null)
				out.println(prefix + "@deprecated " + deprecation);
		}
	}
}
