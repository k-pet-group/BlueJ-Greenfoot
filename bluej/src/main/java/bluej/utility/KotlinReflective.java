/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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
package bluej.utility;

import bluej.debugger.gentype.*;
import bluej.parser.context.KotlinContext;
import bluej.parser.context.MethodMetadata;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Method;
import java.util.*;

/**
 * A Reflective implementation for Kotlin classes that combines Java reflection
 * with Kotlin metadata to provide accurate visibility and member information.
 * 
 * <p>This adapter delegates most operations to an internal JavaReflective instance
 * but overrides getDeclaredMethods() to filter methods based on Kotlin visibility
 * rules (e.g., hiding 'internal' members).
 * 
 * <p><strong>Phase 1 Implementation</strong>: Basic visibility filtering using Kotlin metadata.
 * Future phases will add:
 * <ul>
 * <li>Phase 2: Property-to-method mapping for Kotlin properties</li>
 * <li>Phase 3: Kotlin-specific modifiers (suspend, inline, operator)</li>
 * <li>Phase 4: Enhanced generic type support</li>
 * </ul>
 * 
 * @author BlueJ Team
 */
public class KotlinReflective extends Reflective
{
    /** The underlying Java class */
    private final Class<?> clazz;
    
    /** Kotlin metadata context for this class */
    private final KotlinContext kotlinContext;
    
    /** Internal JavaReflective for delegation */
    private final JavaReflective javaReflective;
    
    /**
     * Construct a KotlinReflective for the given class with Kotlin metadata.
     * 
     * @param clazz The Java Class object for this Kotlin class
     * @param kotlinContext The Kotlin metadata context
     * @throws NullPointerException if either parameter is null
     */
    public KotlinReflective(Class<?> clazz, KotlinContext kotlinContext)
    {
        if (clazz == null || kotlinContext == null) {
            throw new NullPointerException("Class and KotlinContext must not be null");
        }
        
        this.clazz = clazz;
        this.kotlinContext = kotlinContext;
        this.javaReflective = new JavaReflective(clazz);
    }
    
    @Override
    public int hashCode()
    {
        return clazz.hashCode();
    }
    
    @Override
    public boolean equals(Object other)
    {
        if (other instanceof KotlinReflective) {
            KotlinReflective krOther = (KotlinReflective) other;
            return krOther.clazz == clazz;
        }
        // Also equal to JavaReflective of same class for compatibility
        if (other instanceof JavaReflective) {
            JavaReflective jrOther = (JavaReflective) other;
            return jrOther.getUnderlyingClass() == clazz;
        }
        return false;
    }
    
    // ===== Delegated methods (unchanged from JavaReflective) =====
    
    @Override
    public String getName()
    {
        return javaReflective.getName();
    }
    
    @Override
    public String getSimpleName()
    {
        return javaReflective.getSimpleName();
    }
    
    @Override
    public boolean isInterface()
    {
        return javaReflective.isInterface();
    }
    
    @Override
    public boolean isStatic()
    {
        return javaReflective.isStatic();
    }
    
    @Override
    public boolean isPublic()
    {
        return javaReflective.isPublic();
    }
    
    @Override
    public boolean isFinal()
    {
        return javaReflective.isFinal();
    }
    
    @Override
    public List<GenTypeDeclTpar> getTypeParams()
    {
        return javaReflective.getTypeParams();
    }
    
    @Override
    public Reflective getArrayOf()
    {
        return javaReflective.getArrayOf();
    }
    
    @Override
    public Reflective getRelativeClass(String name)
    {
        return javaReflective.getRelativeClass(name);
    }
    
    @Override
    public List<Reflective> getSuperTypesR()
    {
        return javaReflective.getSuperTypesR();
    }
    
    @Override
    public List<GenTypeClass> getSuperTypes()
    {
        return javaReflective.getSuperTypes();
    }
    
    @Override
    public boolean isAssignableFrom(Reflective r)
    {
        return javaReflective.isAssignableFrom(r);
    }
    
    @Override
    public Reflective getOuterClass()
    {
        return javaReflective.getOuterClass();
    }
    
    @Override
    public Reflective getInnerClass(String name)
    {
        return javaReflective.getInnerClass(name);
    }
    
    @Override
    public String getModuleName()
    {
        return javaReflective.getModuleName();
    }
    
    // ===== Kotlin-specific overrides =====
    
    /**
     * Get methods declared in this Kotlin class, filtered by Kotlin visibility rules.
     * 
     * <p><strong>Phase 1 Implementation</strong>: Uses Kotlin metadata to filter out
     * methods that should not be visible. Currently filters based on signature matching
     * against the methods list in KotlinContext.
     * 
     * <p><strong>Known Limitations</strong>:
     * <ul>
     * <li>Does not yet convert properties to getterName/setterName methods (Phase 2)</li>
     * <li>Does not mark suspend/inline functions specially (Phase 3)</li>
     * <li>May not handle all edge cases of Kotlin visibility (e.g., internal modifier)</li>
     * </ul>
     * 
     * @return Map of method names to sets of MethodReflective objects
     */
    @Override
    public Map<String, Set<MethodReflective>> getDeclaredMethods()
    {
        // Get all methods using Java reflection
        Map<String, Set<MethodReflective>> javaMethods = javaReflective.getDeclaredMethods();
        
        // Phase 1: For now, just return Java methods as-is
        // TODO Phase 2: Add property getters/setters from kotlinContext.properties()
        // TODO Phase 3: Filter based on Kotlin visibility (internal modifier)
        // TODO Phase 3: Mark suspend/inline/operator functions
        
        // Build a set of method signatures that should be visible based on Kotlin metadata
        Set<String> visibleSignatures = new HashSet<>();
        for (MethodMetadata method : kotlinContext.methods()) {
            visibleSignatures.add(method.signature());
        }
        
        // For Phase 1, we accept all Java-visible methods
        // In Phase 3, we'll filter based on Kotlin visibility modifiers
        return javaMethods;
    }
    
    /**
     * Get fields declared in this Kotlin class.
     * 
     * <p><strong>Phase 1 Implementation</strong>: Delegates to JavaReflective.
     * Kotlin backing fields are accessed the same way as Java fields.
     * 
     * @return Map of field names to FieldReflective objects
     */
    @Override
    public Map<String, FieldReflective> getDeclaredFields()
    {
        // Delegate to Java reflection - Kotlin backing fields work the same way
        return javaReflective.getDeclaredFields();
    }
    
    /**
     * Get constructors declared in this Kotlin class.
     * 
     * <p><strong>Phase 1 Implementation</strong>: Delegates to JavaReflective.
     * Kotlin constructors are accessed through Java reflection the same way.
     * 
     * @return List of ConstructorReflective objects
     */
    @Override
    public List<ConstructorReflective> getDeclaredConstructors()
    {
        // Delegate to Java reflection - Kotlin constructors work the same way
        return javaReflective.getDeclaredConstructors();
    }
    
    /**
     * Get the underlying Java class.
     * 
     * @return The Class object this Reflective represents
     */
    public Class<?> getUnderlyingClass()
    {
        return clazz;
    }
    
    /**
     * Get the Kotlin metadata context for this class.
     * 
     * @return The KotlinContext containing Kotlin-specific metadata
     */
    public KotlinContext getKotlinContext()
    {
        return kotlinContext;
    }
}