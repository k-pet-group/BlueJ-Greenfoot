/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2015,2017,2019,2020  Michael Kolling and John Rosenberg
 
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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bluej.pkgmgr.target.role.Kind;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.function.Supplier;

/**
 * Describes a possible code completion.
 * 
 * @author Marion Zalk
 */
@OnThread(Tag.FXPlatform)
public abstract class AssistContent
{
    @OnThread(Tag.Any)
    public static enum Access
    {
        PRIVATE, PROTECTED, PACKAGE, PUBLIC;
    }
    @OnThread(Tag.Any)
    public static enum CompletionKind
    {
        METHOD, CONSTRUCTOR, FIELD, LOCAL_VAR, FORMAL_PARAM, TYPE;
        
        public static Set<CompletionKind> allMembers()
        {
            return new HashSet<>(Arrays.asList(METHOD, FIELD));
        }
    }

    @OnThread(Tag.Any)
    public static class ParamInfo
    {
        private final String fullType;
        private final String formalName;
        private final String dummyName;
        private final Supplier<String> javadocDescription;
        
        public ParamInfo(String fullType, String formalName, String dummyName, Supplier<String> javadocDescription)
        {
            this.fullType = fullType;
            this.formalName = formalName;
            this.dummyName = dummyName;
            this.javadocDescription = javadocDescription;
        }
        
        public String getQualifiedType()
        {
            return fullType;
        }
        
        public String getUnqualifiedType()
        {
            // Consider java.util.List<? extends greenfoot.Actor>
            // To dequalify, we must find all sections like (\w+\.) and remove them.
            int beginCurIdent = -1;
            StringBuilder r = new StringBuilder();
            for (int i = 0; i < fullType.length(); i++)
            {
                final char c = fullType.charAt(i);
                if (beginCurIdent == -1 && Character.isJavaIdentifierStart(c))
                {
                    // Started an identifier:
                    beginCurIdent = i;
                }
                else if (beginCurIdent != -1)
                {
                    // Found an identifier; does it continue or stop?
                    
                    if (c == '.')
                    {
                        // Qualififer; ignore the whole identifier and dot:
                        beginCurIdent = -1;
                    }
                    else if (!Character.isJavaIdentifierPart(c))
                    {
                        // Identifier ends, but in something other than a dot; keep it:
                        r.append(fullType.substring(beginCurIdent, i + 1));
                        beginCurIdent = -1;
                    }
                    // Otherwise the identifier continues, so nothing to do
                }
                else
                {
                    // No identifier, add to result
                    r.append(c);
                }
            }
            
            if (beginCurIdent != -1)
                r.append(fullType.substring(beginCurIdent, fullType.length()));
            
            return r.toString();
        }
        
        public String getDummyName()
        {
            return dummyName;
        }

        public String getFormalName()
        {
            return formalName;
        }
        
        public String getJavadocDescription()
        {
            return javadocDescription.get();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ParamInfo paramInfo = (ParamInfo) o;

            if (fullType != null ? !fullType.equals(paramInfo.fullType) : paramInfo.fullType != null) return false;
            if (formalName != null ? !formalName.equals(paramInfo.formalName) : paramInfo.formalName != null)
                return false;
            if (dummyName != null ? !dummyName.equals(paramInfo.dummyName) : paramInfo.dummyName != null) return false;
            return !(javadocDescription != null ? !javadocDescription.equals(paramInfo.javadocDescription) : paramInfo.javadocDescription != null);

        }

        @Override
        public int hashCode()
        {
            int result = fullType != null ? fullType.hashCode() : 0;
            result = 31 * result + (formalName != null ? formalName.hashCode() : 0);
            result = 31 * result + (dummyName != null ? dummyName.hashCode() : 0);
            result = 31 * result + (javadocDescription != null ? javadocDescription.hashCode() : 0);
            return result;
        }

        // For debugging:
        @Override
        public String toString()
        {
            return "ParamInfo{" +
                    "dummyName='" + dummyName + '\'' +
                    ", fullType='" + fullType + '\'' +
                    ", formalName='" + formalName + '\'' +
                    ", javadocDescription='" + (javadocDescription == null ? null : javadocDescription.hashCode()) + '\'' +
                    '}';
        }
    }
    
    /** The name of the variable or method or type */
    @OnThread(Tag.Any)
    public abstract String getName();
    
    /** Will return empty list if it's a method with no parameters,
     *  but null if it is a variable or type and thus can't have parameters */
    @OnThread(Tag.FXPlatform)
    public abstract List<ParamInfo> getParams();

    /** Get the type for this completion (as a string).
     *  For methods, this is the return type; for variables it is the type of the variable. 
     *  Confusingly, for types this returns null (use getName instead). */
    public abstract String getType();
    
    /**
     *  Get the access for this completion (as a string).
     */
    public abstract Access getAccessPermission();

    /** Get the declaring class of this completion (as a string).
     * Returns null if it is a local variable (i.e. not a member of a class)
     * or a non-inner-class type. */
    public abstract String getDeclaringClass();
    
    public abstract CompletionKind getKind();

    /**
     * Get the javadoc comment for this completion. The comment has been stripped of the
     * delimiters (slash-star at the start and star-slash at the end) and intermediate
     * star characters.
     */
    @OnThread(Tag.FXPlatform)
    public abstract String getJavadoc();
    
    /**
     * Gets the package name.  Only valid for class types; returns null otherwise.
     */
    public String getPackage()
    {
        return null;
    }
    
    /**
     * Gets the super types of the type.  Only valid for class types; returns null otherwise.
     */
    public List<String> getSuperTypes()
    {
        return null;
    }
    
    /**
     * Gets the kind of the type (class, interface, etc).  Only valid for types; returns null otherwise.
     */
    public Kind getTypeKind()
    {
        return null;
    }

    /**
     * Callback interface for notification that javadoc is available.
     */
    public interface JavadocCallback
    {
        /**
         * The javadoc for the given method is now available
         * (call getJavadoc() to retrieve it).
         */
        void gotJavadoc(AssistContent content);
    }
    
    public static Access fromModifiers(int modifiers)
    {
        if (Modifier.isPrivate(modifiers)) {
            return Access.PRIVATE;
        }
        if (Modifier.isProtected(modifiers)) {
            return Access.PROTECTED;
        }
        if (Modifier.isPublic(modifiers)) {
            return Access.PUBLIC;
        }
        return Access.PACKAGE;
    }

    public static Comparator<AssistContent> getComparator()
    {
        return Comparator.comparing(AssistContent::getName)
            .thenComparing(AssistContent::getKind)
            .thenComparing(AssistContent::getParams,
                Utility.listComparator(Comparator.comparing(ParamInfo::getQualifiedType)));
    }
}
