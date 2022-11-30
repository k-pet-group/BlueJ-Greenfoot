/*
 This file is part of the BlueJ program.
 Copyright (C) 2016,2020 Michael Kölling and John Rosenberg

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
package bluej.stride.framedjava.elements;

import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.FrameFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.StructuredSlotFragment;
import bluej.stride.framedjava.ast.SuperThisFragment;
import bluej.stride.framedjava.ast.TextSlotFragment;
import bluej.stride.framedjava.slots.StructuredSlot;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * An extension of the XML Element class which also keeps track of enough information
 * to later form XPaths to particular Attributes and Elements.
 */
public class LocatableElement extends Element
{
    /**
     * The CodeElement which generated this Element.  Will be used for mapping FrameFragments.
     */
    private final CodeElement origin;
    /**
     * A map from attribute names (directly contained in this Element) to
     * JavaFragments from which those attributes came, if applicable.
     */
    private final HashMap<String, JavaFragment> attrNames = new HashMap<>();

    public LocatableElement(CodeElement origin, String name)
    {
        super(name);
        this.origin = origin;
    }

    public void addAttributeStructured(String name, StructuredSlotFragment code)
    {
        attrNames.put(name, code);
        addAttribute(new Attribute(name, code.getContent()));
        addAttribute(new Attribute(name + "-java", code.getJavaCode()));
    }

    public void addAttributeAccess(String name, AccessPermissionFragment access)
    {
        attrNames.put(name, access);
        addAttribute(new Attribute(name, access.getContent()));
    }

    public void addAttributeSuperThis(String name, SuperThisFragment superThis)
    {
        attrNames.put(name, superThis);
        addAttribute(new Attribute(name, superThis.getValue().toString()));
    }

    public void addAttributeCode(String name, TextSlotFragment content)
    {
        attrNames.put(name, content);
        addAttribute(new Attribute(name, content.getContent()));
    }

    public static interface LocationMap
    {
        public String locationFor(JavaFragment fragment);
        public String locationFor(CodeElement element);
    }

    /**
     * Builds a location map, from JavaFragment to the XPath within the Element, taking this
     * node as the root, and handling all children.  So if you have something like:
     *
     * <class name="Foo" ...>
       ...
       <methods>
          <method access="public" type="void" name="act" enable="true">
            ...
            <body>
               <call expression="..." enable="true"/>
              <blank/>
              <call expression="bar()" expression-java="bar()" enable="true"/>
            </body>
         </method>
       </methods>
       </class>
     *
     * Then the map will map the JavaFragment corresponding to the "bar()" expression, to the
     * XPath: "/class[1]/methods[1]/method[1]/body[1]/call[2]/@expression" when called on the class,
     * or "/method[1]/body[1]/call[2]/@expression" when called on the Element from the method.
     *
     * @return A map from JavaFragment to XPath String identifying the location of that fragment.
     */
    public LocationMap buildLocationMap()
    {
        IdentityHashMap<JavaFragment, String> map = new IdentityHashMap<>();
        IdentityHashMap<CodeElement, String> elementMap = new IdentityHashMap<>();

        addToLocationMap(new HashMap<>(), map::put, elementMap::put);

        return new LocationMap()
        {
            @Override
            public String locationFor(JavaFragment fragment)
            {
                if (map.containsKey(fragment))
                {
                    return map.get(fragment);
                }
                else if (fragment instanceof FrameFragment)
                {
                    FrameFragment ff = (FrameFragment)fragment;
                    return elementMap.get(ff.getElement());
                }
                else
                    return null;
            }

            @Override
            public String locationFor(CodeElement element)
            {
                return elementMap.get(element);
            }
        };
    }

    private void addToLocationMap(Map<String, Integer> siblingCounts, BiConsumer<JavaFragment, String> map, BiConsumer<CodeElement, String> elementMap)
    {
        // Form an XPath like "/method[2]" for us, and update counts:
        String me = "/" + getLocalName() + "[" + siblingCounts.getOrDefault(getLocalName(), 1) + "]";
        siblingCounts.put(getLocalName(), siblingCounts.getOrDefault(getLocalName(), 1) + 1);

        if (origin != null)
            elementMap.accept(origin, me);
        attrNames.forEach((attrName, fragment) -> map.accept(fragment, me + "/@" + attrName));
        processChildren(map, elementMap, getChildElements(), me);
    }



    private static void processChildren(BiConsumer<JavaFragment, String> map, BiConsumer<CodeElement, String> elementMap, Elements childElements, String me)
    {
        Map<String, Integer> childCounts = new HashMap<>();
        for (int i = 0; i < childElements.size(); i++)
        {
            Element child = childElements.get(i);
            if (child instanceof LocatableElement)
            {
                ((LocatableElement)child).addToLocationMap(childCounts,
                        (frag, path) -> map.accept(frag, me + path),
                        (el, path) -> elementMap.accept(el, me + path));
            }
            else
            {
                // Plain Element, we'll have to do the legwork ourselves:
                String them = "/" + child.getLocalName() + "[" + childCounts.getOrDefault(child.getLocalName(), 1) + "]";
                childCounts.put(child.getLocalName(), childCounts.getOrDefault(child.getLocalName(), 1) + 1);
                // Can't record the attributes of plain elements
                // For grandchildren, recurse:
                processChildren(map, elementMap, child.getChildElements(), me + them);
            }
        }
    }
}
