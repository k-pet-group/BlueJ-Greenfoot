package bluej.views;

import bluej.utility.Utility;

import java.util.*;

/**
** @version $Id: Comment.java 1083 2002-01-11 16:54:51Z mik $
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
        if(text != null) {
            String[] lines = Utility.splitLines(text);

            // trim spaces
            for(int i = 0; i < lines.length; i++)
                lines[i] = lines[i].trim(); 

            // remove blank lines front and back
            int first = 0;
            while (first<lines.length && lines[first].length() == 0)
                first++;
            int last = lines.length - 1;
            while (last>=0 && lines[last].length() == 0)
                last--;

            // print the comment lines
            for(int i = first; i<=last; i++) { 
                for(int j=0; j<indents; j++)
                    out.indentLine(); 
                out.println("// " + lines[i]); 
            } 
        } 
    } 
    
    public String toString()   // simply for testing
    {
        return text;
    }
}
