/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.ast;

import java.util.LinkedList;
import java.util.List;

import nu.xom.Element;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.utility.Utility;

public class JavadocUnit
{
    public static final String ELEMENT = "javadoc";
    private String content;

    public JavadocUnit(String text)
    {
        this.content = text == null ? "" : text;
    }

    public JavadocUnit(Element el)
    {
        content = el.getValue();
        if (content == null) {
            content = "";
        }
    }

    public List<String> getJavaCode()
    {
        List<String> code = new LinkedList<>();
        code.add("/**");
        for (String line : Utility.splitLines(content)) {
            code.add(" * ".concat(line));
        }
        code.add(" */");
        return code;
    }

    public Element toXML()
    {
        Element docEl = new Element(ELEMENT);
        CodeElement.preserveWhitespace(docEl);
        docEl.appendChild(content);
        return docEl;
    }

    @Override
    public String toString()
    {
        return content;
    }
}
