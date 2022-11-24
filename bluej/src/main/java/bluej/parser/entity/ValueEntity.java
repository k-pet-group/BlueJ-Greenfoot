/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaUtils;

/**
 * Represents a value entity - an unspecified value with a known type.
 * 
 * <p>A value entity might also represent a compile-time constant value as defined by the
 * Java Language Specification section 15.28. To satisfy the requirements of the JLS when
 * determining an expression type is necessary then to know the actual value, so there are
 * methods provided for checking whether a value is known and what it is.
 * 
 * <p>Note that integer values are all representable as a "long" so there is only a single
 * method to retrieve the known integer value; likewise "double" can also represent all
 * "float" values.
 * 
 * @author Davin McCall
 */
public class ValueEntity extends JavaEntity
{
    private String name;
    private JavaType type;
    
    /**
     * Construct a value entity representing a value of the given type.
     */
    public ValueEntity(JavaType type)
    {
        this.type = type;
    }
    
    /**
     * Construct a value entity representing a value of the given type coming from
     * the given name (meaning not well defined...)
     */
    public ValueEntity(String name, JavaType type)
    {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public JavaEntity getSubentity(String name, Reflective accessor)
    {
        GenTypeSolid ubound = type.getUpperBound().asSolid();
        if (ubound == null) {
            return null;
        }
        GenTypeSolid [] ubounds = ubound.getIntersectionTypes();
        
        GenTypeClass ctype = ubounds[0].asClass();
        
        if (ctype != null) {
            Reflective ctypeRef = ctype.getReflective();
            LinkedList<Reflective> stypes = new LinkedList<Reflective>();
            stypes.add(ctypeRef);
            
            FieldReflective field = null;
            
            while (! stypes.isEmpty()) {
                ctypeRef = stypes.poll();
                Map<String,FieldReflective> fields = ctypeRef.getDeclaredFields();
                field = fields.get(name);
                if (field != null) {
                    break;
                }
                stypes.addAll(ctypeRef.getSuperTypesR());
            }
            
            if (field != null) {
                ctype = ctype.mapToSuper(ctypeRef.getName());
                if (JavaUtils.checkMemberAccess(ctype.getReflective(), type.asSolid(), accessor,
                        field.getModifiers(), false)) {
                    JavaType fieldType = field.getType();
                    Map<String,GenTypeParameter> tparMap = ctype.getMap();
                    fieldType = fieldType.mapTparsToTypes(tparMap).asType();
                    return new ValueEntity(this.name + "." + name, fieldType);
                }
            }
        }
        return null;
    }

    @Override
    public JavaType getType()
    {
        return type;
    }

    @Override
    public ValueEntity resolveAsValue()
    {
        return this;
    }
    
    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }
    
    /**
     * Check whether this value entity represents a constant integer (byte,int,long,etc) value
     */
    public boolean hasConstantIntValue()
    {
        return false;
    }
    
    /**
     * Get the constant integer value represented by this value entity
     */
    public long getConstantIntValue()
    {
        throw new RuntimeException("Attempt to get constant value for entity without constant value");
    }
    
    /**
     * Check whether this value entity represents a constant "float" value
     */
    public boolean hasConstantFloatValue()
    {
        return false;
    }
    
    /**
     * Get the constant floating-point value represented by this value entity
     */
    public double getConstantFloatValue()
    {
        throw new RuntimeException("Attempt to get constant value for entity without constant value");
    }
    
    /**
     * Check whether this value entity represents a constant boolean value
     */
    public boolean hasConstantBooleanValue()
    {
        return false;
    }
    
    /**
     * Get the constant boolean value this value entity represents
     */
    public boolean getConstantBooleanValue()
    {
        throw new RuntimeException("Attempt to get constant value for entity without constant value");
    }
    
    /**
     * Check whether this value entity represents a String constant
     */
    public boolean isConstantString()
    {
        return false;
    }
    
    /**
     * Get the constant string value that this entity represents
     */
    public String getConstantString()
    {
        throw new RuntimeException("Attempt to get constant string value for an entity without such a value");
    }
    
    /**
     * Check whether a value entity represents any kind of constant (knonw at compile-time as per the JLS) value.
     */
    public static boolean isConstant(ValueEntity ent)
    {
        return ent.hasConstantBooleanValue() || ent.hasConstantIntValue()
                || ent.hasConstantFloatValue() || ent.isConstantString();
    }
}
