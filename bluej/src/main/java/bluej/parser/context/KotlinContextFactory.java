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
package bluej.parser.context;

import kotlin.metadata.*;
import kotlin.metadata.jvm.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Factory for creating {@link KotlinContext} instances from Kotlin bytecode metadata.
 * 
 * <p>This factory extracts structured information from {@link KotlinClassMetadata}
 * using direct property access to {@link KmClass}. It processes:
 * <ul>
 *   <li>Properties (Kotlin val/var) with getterName/setterName information</li>
 *   <li>Functions (excluding property accessors)</li>
 *   <li>Fields (backing fields only - extracted from properties)</li>
 * </ul>
 * 
 * <p>Since JVM signature information may not be directly accessible from Java,
 * this implementation constructs signatures from property types and names using
 * Kotlin's standard naming conventions (getPropertyName, setPropertyName).
 * 
 * @see KotlinContext
 * @see KotlinMetadataReader
 */
public class KotlinContextFactory {
    
    /**
     * Creates a {@link KotlinContext} from Kotlin bytecode metadata.
     * 
     * <p>This method extracts all metadata from the provided {@link KotlinClassMetadata}
     * and constructs a complete {@link KotlinContext} with properties, methods, and fields.
     * 
     * <p>If the metadata is not for a class (e.g., file facade, synthetic class),
     * an empty context is returned.
     * 
     * @param className the fully qualified class name
     * @param classFile the path to the .class file (for error reporting)
     * @param metadata the Kotlin class metadata read from bytecode
     * @return a complete KotlinContext with all extracted metadata
     */
    public static KotlinContext fromMetadata(
        String className,
//        Path classFile,
        KotlinClassMetadata metadata
    ) {
        // Only process class metadata (not file facades, etc.)
        if (!(metadata instanceof KotlinClassMetadata.Class)) {
            return new KotlinContext(className, List.of(), List.of(), List.of());
        }
        
        KotlinClassMetadata.Class classMetadata = (KotlinClassMetadata.Class) metadata;
        KmClass kmClass = classMetadata.getKmClass();
        
        // Extract properties, methods, and fields from kmClass
        List<PropertyMetadata> properties = extractProperties(kmClass);
        List<MethodMetadata> methods = extractMethods(kmClass);
        List<FieldMetadata> fields = extractFields(kmClass);
        
        return new KotlinContext(className, methods, fields, properties);
    }
    
    /**
     * Extracts property metadata from a Kotlin class.
     * 
     * <p>Properties in Kotlin compile to getterName/setterName methods. This method extracts
     * the property information and constructs JVM method signatures based on Kotlin
     * naming conventions:
     * <ul>
     *   <li>Getter: getPropertyName() for properties, isPropertyName() for Boolean</li>
     *   <li>Setter: setPropertyName(Type) for var properties</li>
     * </ul>
     * 
     * @param kmClass the Kotlin class metadata
     * @return list of property metadata
     */
    private static List<PropertyMetadata> extractProperties(KmClass kmClass) {
        List<PropertyMetadata> properties = new ArrayList<>();
        
        for (KmProperty property : kmClass.getProperties()) {
            String name = property.getName();
            KmType returnType = property.getReturnType();
            
            // Determine property type
            String propertyType = formatKotlinType(returnType);

            var getter = Optional.ofNullable(property.getGetter());
            var setter = Optional.ofNullable(property.getSetter());

            var getterName = getter.map(p -> constructGetterName(name, propertyType));
            var setterName = setter.map(p -> constructSetterName(name));

            properties.add(new PropertyMetadata(
                name,
                propertyType,
                getterName,
                setterName,
                Optional.empty() // KDoc not available at runtime
            ));
        }
        
        return properties;
    }
    
    /**
     * Constructs getterName method name for a property.
     * 
     * <p>Follows Kotlin conventions:
     * <ul>
     *   <li>Boolean properties: isPropertyName</li>
     *   <li>Other properties: getPropertyName</li>
     * </ul>
     * 
     * @param propertyName the property name
     * @param propertyType the property type
     * @return the getterName method name
     */
    private static String constructGetterName(String propertyName, String propertyType) {
        String capitalizedName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        
        // Use "is" prefix for boolean properties
        if ("boolean".equals(propertyType)) {
            return "is" + capitalizedName;
        }
        
        return "get" + capitalizedName;
    }
    
    /**
     * Constructs setterName method name for a property.
     * 
     * @param propertyName the property name
     * @return the setterName method name
     */
    private static String constructSetterName(String propertyName) {
        String capitalizedName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        return "set" + capitalizedName;
    }
    
    /**
     * Extracts method metadata from a Kotlin class.
     * 
     * <p>This method extracts regular functions and filters out property accessors
     * (which are handled separately as properties).
     * 
     * @param kmClass the Kotlin class metadata
     * @return list of method metadata
     */
    private static List<MethodMetadata> extractMethods(KmClass kmClass) {
        List<MethodMetadata> methods = new ArrayList<>();
        
        for (KmFunction function : kmClass.getFunctions()) {
            String name = function.getName();
            
            // Skip property accessors - they're handled by extractProperties
            if (isPropertyAccessor(name)) {
                continue;
            }
            
            // Format return type
            String returnTypeStr = formatKotlinType(function.getReturnType());
            
            // Extract parameter types
            List<String> paramTypes = new ArrayList<>();
            for (KmValueParameter param : function.getValueParameters()) {
                paramTypes.add(formatKotlinType(param.getType()));
            }
            
            // Construct signature: "returnType name(param1,param2)"
            String signature = constructMethodSignature(returnTypeStr, name, paramTypes);
            
            methods.add(new MethodMetadata(
                name,
                signature,
                returnTypeStr,
                paramTypes,
                Optional.empty() // KDoc not available at runtime
            ));
        }
        
        return methods;
    }
    
    /**
     * Constructs a method signature string.
     * 
     * @param returnType the return type
     * @param methodName the method name
     * @param paramTypes the parameter types
     * @return formatted signature
     */
    private static String constructMethodSignature(String returnType, String methodName, List<String> paramTypes) {
        StringBuilder sig = new StringBuilder();
        if (!"void".equals(returnType)) {
            sig.append(returnType).append(" ");
        }
        sig.append(methodName).append("(");
        sig.append(String.join(",", paramTypes));
        sig.append(")");
        return sig.toString();
    }
    
    /**
     * Extracts field metadata from a Kotlin class.
     * 
     * <p>In Kotlin, properties may have backing fields. Since we don't have direct
     * access to field signatures from Java, we construct field metadata based on
     * property information.
     * 
     * @param kmClass the Kotlin class metadata
     * @return list of field metadata (may be empty if fields cannot be determined)
     */
    private static List<FieldMetadata> extractFields(KmClass kmClass) {
        // Note: Without JVM extension access, we cannot reliably determine which
        // properties have backing fields. Return empty list for now.
        // This will be enhanced when JVM signature access is available.
        return List.of();
    }
    
    /**
     * Checks if a function name indicates a property accessor.
     * 
     * <p>Property accessors follow naming conventions: getX, setX, isX where X
     * starts with an uppercase letter.
     * 
     * @param name the function name to check
     * @return true if this is likely a property accessor
     */
    private static boolean isPropertyAccessor(String name) {
        if (name.length() < 4) return false;
        
        // Check for get/set/is prefix followed by uppercase letter
        return (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3)))
            || (name.startsWith("set") && name.length() > 3 && Character.isUpperCase(name.charAt(3)))
            || (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2)));
    }
    
    /**
     * Formats a Kotlin type to readable Java type name.
     * 
     * <p>Converts Kotlin type representation to Java format:
     * <pre>
     * kotlin/String → java.lang.String
     * kotlin/Int → int
     * kotlin/collections/List → java.util.List
     * </pre>
     * 
     * @param type the Kotlin type
     * @return readable Java type name
     */
    private static String formatKotlinType(KmType type) {
        if (type == null) return "void";
        
        KmClassifier classifier = type.getClassifier();
        if (classifier instanceof KmClassifier.Class) {
            String className = ((KmClassifier.Class) classifier).getName();
            return mapKotlinTypeToJava(className);
        }
        
        return "java.lang.Object"; // Fallback for complex types
    }
    
    /**
     * Maps Kotlin type names to Java type names.
     * 
     * <p>Handles Kotlin stdlib types and converts them to Java equivalents.
     * 
     * @param kotlinType the Kotlin type name (internal format)
     * @return Java type name
     */
    private static String mapKotlinTypeToJava(String kotlinType) {
        // Map Kotlin primitives to Java primitives
        return switch (kotlinType) {
            case "kotlin/Int" -> "int";
            case "kotlin/Long" -> "long";
            case "kotlin/Short" -> "short";
            case "kotlin/Byte" -> "byte";
            case "kotlin/Float" -> "float";
            case "kotlin/Double" -> "double";
            case "kotlin/Char" -> "char";
            case "kotlin/Boolean" -> "boolean";
            case "kotlin/Unit" -> "void";
            case "kotlin/String" -> "java.lang.String";
            case "kotlin/Any" -> "java.lang.Object";
            default -> {
                // Convert internal format to Java format
                if (kotlinType.startsWith("kotlin/collections/")) {
                    // Map Kotlin collections to Java equivalents
                    String collectionType = kotlinType.substring("kotlin/collections/".length());
                    yield "java.util." + collectionType;
                }
                // Default: convert slashes to dots
                yield kotlinType.replace('/', '.');
            }
        };
    }
}