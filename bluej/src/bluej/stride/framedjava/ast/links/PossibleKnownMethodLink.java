/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.slots.UnderlineContainer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A possible link (see @{@link PossibleLink}) to a specific method
 * in a specific superclass, originating from a method override in some subclass.
 */
public class PossibleKnownMethodLink extends PossibleLink
{
    /** The qualified name of the declaring class */
    private final String qualClassName;
    /** The name of the method */
    private final String methodName;
    /** The qualified type names of the parameters */
    private final List<String> qualParamTypes;

    public PossibleKnownMethodLink(String qualClassName, String methodName, List<String> qualParamTypes, int startPosition, int endPosition, UnderlineContainer slot)
    {
        super(startPosition, endPosition, slot);
        this.methodName = methodName;
        this.qualParamTypes = qualParamTypes;
        this.qualClassName = qualClassName;
    }

    public String getQualClassName()
    {
        return qualClassName;
    }

    public String getURLMethodSuffix()
    {
        return encodeSuffix(methodName, qualParamTypes);
    }

    /**
     * Encodes the method name and qualified parameter types into a suffix
     * used to find the right place in Javadoc HTML.  Includes the leading '#'.
     */
    public static String encodeSuffix(String methodName, List<String> qualParamTypes)
    {
        return "#" + methodName + "-" + qualParamTypes.stream().map(PossibleKnownMethodLink::chopAtOpenAngle).collect(Collectors.joining("-")) + "-";
    }

    private static String chopAtOpenAngle(String s)
    {
        int i = s.indexOf('<');
        if (i < 0)
            return s;
        else
            return s.substring(0, i);
    }

    public String getDisplayName()
    {
        return methodName + "(" + qualParamTypes.stream().collect(Collectors.joining(", ")) + ")";
    }
}
