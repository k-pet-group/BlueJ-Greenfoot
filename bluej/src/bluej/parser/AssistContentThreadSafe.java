/*
 This file is part of the BlueJ program.
 Copyright (C) 2014,2015,2016,2017,2019,2020 Michael Kölling and John Rosenberg

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
package bluej.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import bluej.parser.AssistContent.Access;
import bluej.pkgmgr.target.role.Kind;
import bluej.stride.framedjava.elements.CodeElement;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import nu.xom.Attribute;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;

@OnThread(Tag.Any)
public final class AssistContentThreadSafe
{
    private final String name;
    private final List<ParamInfo> params;
    private final String type;
    private final AssistContent.Access access;
    private final String declaringClass;
    private final CompletionKind kind;
    private final Kind typeKind;
    private final String javadoc;
    private final List<String> superTypes;
    private final String packageName;

    @OnThread(Tag.FXPlatform)
    public AssistContentThreadSafe(AssistContent copyFrom)
    {
        name = copyFrom.getName();
        params = copyFrom.getParams();
        type = copyFrom.getType();
        access = copyFrom.getAccessPermission();
        declaringClass = copyFrom.getDeclaringClass();
        kind = copyFrom.getKind();
        javadoc = copyFrom.getJavadoc();
        superTypes = copyFrom.getSuperTypes();
        typeKind = copyFrom.getTypeKind();
        packageName = copyFrom.getPackage();
    }
    
    @OnThread(Tag.FXPlatform)
    public static AssistContentThreadSafe copy(AssistContent copyFrom)
    {
        return new AssistContentThreadSafe(copyFrom);
    }

    @OnThread(Tag.Any)
    public AssistContentThreadSafe(Access access, String declaringClass, String javadoc, CompletionKind kind, String name, String packageName, List<ParamInfo> params, List<String> superTypes, String type, Kind typeKind)
    {
        this.access = access;
        this.declaringClass = declaringClass;
        this.javadoc = javadoc;
        this.kind = kind;
        this.name = name;
        this.packageName = packageName;
        this.params = params;
        this.superTypes = superTypes;
        this.type = type;
        this.typeKind = typeKind;
    }

    /** The name of the variable or method or type */
    public String getName() { return name; }
    
    /** Will return empty list if it's a method with no parameters,
     *  but null if it is a variable or type and thus can't have parameters */
    public List<ParamInfo> getParams() { return params; }

    /** Get the type for this completion (as a string).
     *  For methods, this is the return type; for variables it is the type of the variable. 
     *  Confusingly, for types this returns null (use getName instead). */
    public String getType() { return type; }
    
    /**
     *  Get the access for this completion (as a string).
     */
    public AssistContent.Access getAccessPermission() { return access; }

    /** Get the declaring class of this completion (as a string).
     * Returns null if it is a local variable (i.e. not a member of a class)
     * or a non-inner-class type. */
    public String getDeclaringClass() { return declaringClass; }
    
    public CompletionKind getKind() { return kind; }

    /**
     * Get the javadoc comment for this completion. The comment has been stripped of the
     * delimiters (slash-star at the start and star-slash at the end) and intermediate
     * star characters.
     */
    public String getJavadoc() { return javadoc; }
    
    public String getPackage() { return packageName; }

    public static Comparator<AssistContentThreadSafe> getComparator(String targetType)
    {
        return (a, b) -> { 
            if (targetType != null)
            {
                // Anything matching target type goes ahead:
                // Lower is better, so 0 if the types match
                int compareTypes = Integer.compare(targetType.equals(a.getType()) ? 0 : 1, targetType.equals(b.getType()) ? 0 : 1);
                
                if (compareTypes != 0)
                    return compareTypes;
            }
            
            int compareNames = a.getName().toLowerCase().compareTo(b.getName().toLowerCase());
            
            if (compareNames != 0)
                return compareNames;
            
            // Fields before methods, methods sorted by number of params:
            int aParams = a.getParams() == null ? -1 : a.getParams().size();
            int bParams = b.getParams() == null ? -1 : b.getParams().size();
            
            return Integer.compare(aParams, bParams);
        };
    }

    public List<String> getSuperTypes()
    {
        return Collections.unmodifiableList(superTypes);
    }
    
    public Kind getTypeKind()
    {
        return typeKind;
    }

    @OnThread(Tag.FXPlatform)
    public String getDocHTML()
    {
        String header = (getType() == null ? "" : Utility.escapeAngleBrackets(getType()))
                          + " <b>" + getName() + "</b>";
        if (getParams() != null)
        {
            header += "(" + getParams().stream().map(p -> { 
               String type = Utility.escapeAngleBrackets(p.getUnqualifiedType());
               if (p.getFormalName() != null)
                   return type + "&nbsp;" + p.getFormalName();
               else
                   return type;
            }).collect(Collectors.joining(", ")) + ")";
        }
        header += "<br><br>"; // TODO make this proper HTML spacing
        
        // Match font with that of a Label:
        Font font = new Label().getFont();
        String start = "<html><body style='font-family:" + font.getFamily() + ";font-size:" + font.getSize() + ";'>";
        String end = "</body></html>";
        String javadoc = getJavadoc() != null ? getJavadoc() : "";
        return start + header + JavaUtils.javadocToHtml(javadoc.replace("\n\n", "<br><br>")) + end;
    }

    public boolean accessibleFromPackage(String pkgName)
    {
        if (access == null)
            return true;
        switch (access)
        {
            case PRIVATE:
                return false;
            case PROTECTED:
            case PACKAGE:
                return pkgName.equals(this.packageName);
            case PUBLIC:
                return true;
        }
        // Impossible:
        return true;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssistContentThreadSafe that = (AssistContentThreadSafe) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (params != null ? !params.equals(that.params) : that.params != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (access != that.access) return false;
        if (declaringClass != null ? !declaringClass.equals(that.declaringClass) : that.declaringClass != null)
            return false;
        if (kind != that.kind) return false;
        if (typeKind != that.typeKind) return false;
        if (javadoc != null ? !javadoc.equals(that.javadoc) : that.javadoc != null) return false;
        if (superTypes != null ? !superTypes.equals(that.superTypes) : that.superTypes != null) return false;
        return !(packageName != null ? !packageName.equals(that.packageName) : that.packageName != null);

    }

    @Override
    public int hashCode()
    {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (params != null ? params.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (access != null ? access.hashCode() : 0);
        result = 31 * result + (declaringClass != null ? declaringClass.hashCode() : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (typeKind != null ? typeKind.hashCode() : 0);
        result = 31 * result + (javadoc != null ? javadoc.hashCode() : 0);
        result = 31 * result + (superTypes != null ? superTypes.hashCode() : 0);
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        return result;
    }

    // For debugging:
    @Override
    public String toString()
    {
        return "AssistContentThreadSafe{" +
                "access=" + access +
                ", name='" + name + '\'' +
                ", params=(" + (params == null ? "<blank>" : params.stream().map(ParamInfo::toString).collect(Collectors.joining(","))) +
                "), type='" + type + '\'' +
                ", declaringClass='" + declaringClass + '\'' +
                ", kind=" + kind +
                ", typeKind=" + typeKind +
                ", javadoc='" + (javadoc == null ? "null" : javadoc.hashCode()) + '\'' +
                ", superTypes=" + superTypes +
                ", packageName='" + packageName + '\'' +
                '}';
    }

    /**
     * Serialise the content of this item to XML.  Only valid and tested
     * for imported types at the moment.
     */
    public Element toXML()
    {
        Element el = new Element("acts");
        if (name != null) el.addAttribute(new Attribute("name", name));
        if (type != null) el.addAttribute(new Attribute("type", type));
        if (access != null) el.addAttribute(new Attribute("access", access.toString()));
        if (declaringClass != null) el.addAttribute(new Attribute("declaringClass", declaringClass));
        if (kind != null) el.addAttribute(new Attribute("kind", kind.toString()));
        if (typeKind != null) el.addAttribute(new Attribute("typeKind", typeKind.toString()));
        if (packageName != null) el.addAttribute(new Attribute("packageName", packageName));
        if (params != null)
        {
            throw new IllegalStateException();
            // We don't actually need to serialised params yet, just types:
            /*
            Element paramsEl = new Element("params");
            for (ParamInfo paramInfo : params)
                paramsEl.appendChild(paramInfo.toXML());
            el.appendChild(paramsEl);
            */
        }
        if (superTypes != null)
        {
            Element superTypesEl = new Element("superTypes");
            for (String superType : superTypes)
            {
                Element superEl = new Element("superType");
                superEl.addAttribute(new Attribute("superType", superType));
                superTypesEl.appendChild(superEl);
            }
            el.appendChild(superTypesEl);
        }
        if (javadoc != null)
        {
            Element javadocEl = new Element("javadoc");
            CodeElement.preserveWhitespace(javadocEl);
            javadocEl.appendChild(javadoc);
            el.appendChild(javadocEl);
        }

        return el;
    }

    /**
     * Opposite of toXML; load this item from XML.
     */
    public AssistContentThreadSafe(Element el)
    {
        if (!el.getLocalName().equals("acts")) throw new IllegalArgumentException();
        name = el.getAttributeValue("name");
        type = el.getAttributeValue("type");
        access = loadEnum(Access.values(), el.getAttributeValue("access"));
        declaringClass = el.getAttributeValue("declaringClass");
        kind = loadEnum(CompletionKind.values(), el.getAttributeValue("kind"));
        typeKind = loadEnum(Kind.values(), el.getAttributeValue("typeKind"));
        packageName = el.getAttributeValue("packageName");
        ArrayList<ParamInfo> paramsList = null;
        ArrayList<String> superTypesList = null;
        String javadocStr = null;
        for (int i = 0; i < el.getChildElements().size(); i++)
        {
            Element subEl = el.getChildElements().get(i);
            switch (subEl.getLocalName())
            {
                // We don't actually need to serialised params yet, just types:
                /*
                case "params":
                    paramsList = new ArrayList<>();
                    for (int j = 0; j < subEl.getChildElements().size(); j++)
                    {
                        Element paramEl = subEl.getChildElements().get(j);
                        paramsList.add(new ParamInfo(paramEl));
                    }
                    break;
                */
                case "superTypes":
                    superTypesList = new ArrayList<>();
                    for (int j = 0; j < subEl.getChildElements().size(); j++)
                    {
                        Element superTypeEl = subEl.getChildElements().get(j);
                        superTypesList.add(superTypeEl.getAttributeValue("superType"));
                    }
                    break;
                case "javadoc":
                    javadocStr = subEl.getChild(0).getValue();
                    break;
            }
        }
        params = paramsList;
        superTypes = superTypesList;
        javadoc = javadocStr;
    }

    /**
     * Helper method for loading enums from a String
     */
    private static <E extends Enum<E>> E loadEnum(E[] values, String src)
    {
        for (E e : values)
            if (e.toString().equals(src))
                return e;

        return null;
    }
}

