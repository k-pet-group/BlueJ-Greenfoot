package bluej.views;

import bluej.utility.Utility;

import java.util.*;

/**
 ** @version $Id: Comment.java 244 1999-08-20 06:42:33Z mik $
 ** @author Michael Cahill
 **
 ** Comment class - the source information associated with a class or field
 **/
public final class Comment
{
    private String target;		// identifies what this comment is for
    private String text;
    private String paramnames[];
	
    public void load(Properties p, String prefix)
    {
	target = p.getProperty(prefix + ".target", "<no target>");
	text = p.getProperty(prefix + ".text");

        String paramnamestring = p.getProperty(prefix + ".params");

        if (paramnamestring != null) {
            StringTokenizer st = new StringTokenizer(paramnamestring, " ");

            paramnames = new String[st.countTokens()];
            int i = 0;
            while(st.hasMoreTokens()) {
                paramnames[i] = st.nextToken();
                i++;
            }
        }
    }
	
    public String getTarget()
    {
	return target;
    }
	
    public String getText()
    {
	return text;
    }
	
    public String getParamName(int i)
    {
        if (paramnames != null) {
            if(i >= 0 && i < paramnames.length)
                return paramnames[i];
        }
        return null;
    }

    public int getParamCount()
    {
        if (paramnames != null)
            return paramnames.length;
        return 0;
    }
    
    /*	public void addReference(String classname, String fieldname)
	{
	// refs.addElement(new Reference(classname, fieldname));
	}
	
	public Enumeration getReferences()
	{
	return refs.elements();
	}
    */
 	
    public void print(FormattedPrintWriter out)
    {
	print(out, 0);
    }
	
    public void print(FormattedPrintWriter out, int indents)
    {
	out.setBold(false);
	out.setItalic(true);
	if(text != null)
	    {
		String[] lines = Utility.splitLines(text);
	
		for(int i = 0; i < lines.length; i++)
		    {
			for(int j=0; j<indents; j++)
			    out.indentLine();
			out.println(lines[i].trim());
		    }
	    }
    }
}
