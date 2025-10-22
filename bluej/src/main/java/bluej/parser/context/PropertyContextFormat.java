/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2016  Michael Kolling and John Rosenberg 

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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

final class PropertyContextFormat {

    /**
     * Internal record for holding parsed metadata during conversion.
     * Used to convert CommentEntry lists to structured metadata records.
     */
    private record ParsedMetadata(
        List<MethodMetadata> methods,
        List<FieldMetadata> fields
    ) {}

    /**
     * Loads a JavaContext from a .ctxt file.
     *
     * @param className The fully qualified class name
     * @param ctxtFile The .ctxt file to load from
     * @return A JavaContext with parsed metadata, or null if loading fails
     */
    static JavaContext fromFile(@NotNull String className, @NotNull File ctxtFile) {
        try {
            Properties props = loadProperties(ctxtFile);
            return fromProperties(className, props);
        }
        catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Creates a JavaContext from Properties (used by CompilationUnitContextLoader).
     * Package-private for use within the context package.
     *
     * @param className The fully qualified class name
     * @param props The Properties from .ctxt file or ClassInfo
     * @return A JavaContext with parsed metadata
     */
    static JavaContext fromProperties(@NotNull String className, @NotNull Properties props) {
        List<CommentEntry> entries = fromProperties(props);
        ParsedMetadata parsed = parseCommentEntries(entries);
        return new JavaContext(className, parsed.methods(), parsed.fields());
    }
    
    /**
     * Converts package-private CommentEntry list to structured metadata.
     * Internal implementation detail for .ctxt parsing.
     *
     * @param entries The list of comment entries from .ctxt file
     * @return Parsed metadata containing methods and fields
     */
    private static ParsedMetadata parseCommentEntries(List<CommentEntry> entries) {
        List<MethodMetadata> methods = new ArrayList<>();
        List<FieldMetadata> fields = new ArrayList<>();
        
        for (CommentEntry entry : entries) {
            String target = entry.getTarget();
            String text = entry.getText();
            Optional<String> doc = Optional.ofNullable(text)
                .filter(s -> !s.isEmpty());
            
            if (isFieldSignature(target)) {
                // Parse field: just the field name
                fields.add(new FieldMetadata(
                    extractFieldName(target),
                    "Object", // Type info not available in .ctxt files
                    doc
                ));
            } else {
                // Parse method: "returnType methodName(params)"
                methods.add(parseMethodMetadata(entry));
            }
        }
        
        return new ParsedMetadata(methods, fields);
    }
    
    /**
     * Parses a method signature from a CommentEntry.
     *
     * @param entry The comment entry containing method information
     * @return Parsed method metadata
     */
    private static MethodMetadata parseMethodMetadata(CommentEntry entry) {
        String signature = entry.getTarget();
        String comment = entry.getText();
        List<String> paramNames = entry.getParamNames();
        
        // Extract method name and return type
        String methodName = extractMethodName(signature);
        String returnType = extractReturnType(signature);
        List<String> paramTypes = extractParameters(signature);
        
        // Match param types with param names
        List<String> parameters = new ArrayList<>();
        for (int i = 0; i < paramTypes.size(); i++) {
            String paramType = paramTypes.get(i);
            String paramName = i < paramNames.size() ? paramNames.get(i) : "arg" + i;
            parameters.add(paramType + " " + paramName);
        }
        
        Optional<String> documentation = Optional.ofNullable(comment)
            .filter(s -> !s.isEmpty());
        
        return new MethodMetadata(
            methodName,
            signature,
            returnType,
            parameters,
            documentation
        );
    }
    
    /**
     * Determines if a signature represents a field (no parentheses).
     *
     * @param target The signature string
     * @return true if this is a field signature
     */
    private static boolean isFieldSignature(String target) {
        return !target.contains("(");
    }
    
    /**
     * Extracts the method name from a signature.
     * Format: "returnType methodName(params)"
     *
     * @param signature The method signature
     * @return The method name
     */
    private static String extractMethodName(String signature) {
        int parenIndex = signature.indexOf('(');
        if (parenIndex == -1) {
            return signature.trim();
        }
        
        int spaceIndex = signature.lastIndexOf(' ', parenIndex);
        if (spaceIndex == -1) {
            // No space before paren - likely a constructor or malformed
            return signature.substring(0, parenIndex).trim();
        }
        
        return signature.substring(spaceIndex + 1, parenIndex).trim();
    }
    
    /**
     * Extracts the return type from a signature.
     * Format: "returnType methodName(params)"
     *
     * @param signature The method signature
     * @return The return type, or empty string if not present
     */
    private static String extractReturnType(String signature) {
        int parenIndex = signature.indexOf('(');
        if (parenIndex == -1) {
            return "";
        }
        
        int spaceIndex = signature.lastIndexOf(' ', parenIndex);
        if (spaceIndex == -1) {
            // No return type (constructor or malformed)
            return "";
        }
        
        return signature.substring(0, spaceIndex).trim();
    }
    
    /**
     * Extracts parameter types from a signature.
     * Format: "returnType methodName(type1,type2,type3)"
     *
     * @param signature The method signature
     * @return List of parameter types
     */
    private static List<String> extractParameters(String signature) {
        int start = signature.indexOf('(');
        int end = signature.indexOf(')');
        
        if (start == -1 || end == -1 || start >= end) {
            return List.of();
        }
        
        String params = signature.substring(start + 1, end).trim();
        if (params.isEmpty()) {
            return List.of();
        }
        
        return Arrays.stream(params.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * Extracts the field name from a target string.
     * For fields, the target is just the field name.
     *
     * @param target The field target string
     * @return The field name
     */
    private static String extractFieldName(String target) {
        return target.trim();
    }

    /**
     * Writes a JavaContext to a .ctxt file.
     * Converts structured metadata back to CommentEntry format for serialization.
     *
     * @param context The JavaContext to write
     * @param ctxtFile The .ctxt file to write to
     * @throws IOException if writing fails
     */
    static void writeToFile(@NotNull JavaContext context, @NotNull File ctxtFile) throws IOException {
        // Convert JavaContext back to CommentEntry list for serialization
        List<CommentEntry> entries = toCommentEntries(context);
        Properties props = toProperties(entries);

        File parentDir = ctxtFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (OutputStream out = new FileOutputStream(ctxtFile)) {
            props.store(out, "BlueJ class context");
        }
    }
    
    /**
     * Converts a JavaContext back to CommentEntry list for .ctxt serialization.
     * Internal conversion for backward compatibility with .ctxt file format.
     *
     * @param context The JavaContext to convert
     * @return List of CommentEntry objects
     */
    private static List<CommentEntry> toCommentEntries(JavaContext context) {
        List<CommentEntry> entries = new ArrayList<>();
        
        // Convert methods to CommentEntry
        for (MethodMetadata method : context.methods()) {
            String text = method.documentation().orElse("");
            List<String> paramNames = method.parameters().stream()
                .map(param -> {
                    // Extract just the name from "type name" format
                    String[] parts = param.split("\\s+");
                    return parts.length > 1 ? parts[1] : parts[0];
                })
                .toList();
            
            entries.add(new CommentEntry(method.signature(), text, paramNames));
        }
        
        // Convert fields to CommentEntry
        for (FieldMetadata field : context.fields()) {
            String text = field.documentation().orElse("");
            entries.add(new CommentEntry(field.name(), text, List.of()));
        }
        
        return entries;
    }

    static Properties toProperties(List<CommentEntry> comments) {
        Properties props = new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return java.util.Collections.enumeration(new TreeSet<>(super.keySet()));
            }
        };

        props.setProperty("numComments", String.valueOf(comments.size()));

        for (int i = 0; i < comments.size(); i++) {
            CommentEntry entry = comments.get(i);
            props.setProperty("comment" + i + ".target", entry.getTarget());
            String text = entry.getText();
            if (text != null && !text.isEmpty()) {
                props.setProperty("comment" + i + ".text", text);
            }
            List<String> params = entry.getParamNames();
            if (!params.isEmpty()) {
                props.setProperty("comment" + i + ".params", String.join(" ", params));
            }
        }

        return props;
    }

    static List<CommentEntry> fromProperties(Properties props) {
        int numComments = 0;
        try {
            numComments = Integer.parseInt(props.getProperty("numComments", "0"));
        } catch (NumberFormatException e) {
            // ignore invalid value
        }

        List<CommentEntry> entries = new ArrayList<>(numComments);
        for (int i = 0; i < numComments; i++) {
            String prefix = "comment" + i;
            String target = props.getProperty(prefix + ".target");
            if (target == null) {
                continue;
            }
            String text = props.getProperty(prefix + ".text");
            String paramString = props.getProperty(prefix + ".params");
            List<String> params = null;
            if (paramString != null && !paramString.isEmpty()) {
                params = Arrays.asList(paramString.split(" "));
            }
            else {
                params = Collections.emptyList();
            }
            entries.add(new CommentEntry(target, text, params));
        }
        return entries;
    }

    private static Properties loadProperties(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return loadProperties(in);
        }
    }

    private static Properties loadProperties(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        return props;
    }
}
