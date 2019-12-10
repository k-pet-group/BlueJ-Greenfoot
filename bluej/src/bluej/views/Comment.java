/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2016,2019  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.views;

import bluej.utility.Utility;
import bluej.views.FormattedPrintWriter.ColorScheme;
import bluej.views.FormattedPrintWriter.SizeScheme;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.*;

/**
 * Comment class - the source information associated with a class or field
 * 
 * @author Michael Cahill
 */
public final class Comment
{
    private String target;  // identifies what this comment is for
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
    
    public String[] getParamNames()
    {
        if (paramnames == null)
            return null;
        
        return (String[]) paramnames.clone();
    }

    @OnThread(Tag.FXPlatform)
    public void print(FormattedPrintWriter out)
    {
        print(out, 0);
    }

    @OnThread(Tag.FXPlatform)
    public void print(FormattedPrintWriter out, int indents)
    {
        out.setBold(false);
        out.setItalic(true);
        out.setColor(ColorScheme.GRAY);
        out.setSize(SizeScheme.SMALL);

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
                out.println(lines[i]);
            } 
        } 
    } 
    
    public String toString()   // simply for testing
    {
        return text;
    }
}
