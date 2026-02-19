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
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@OnThread(value = Tag.Any, ignoreParent = true)
final class PropertyContextFormat {

    /**
     *
     * @param className
     * @param ctxtFile
     * @return
     */
    static CompilationUnitContext fromFile(@NotNull String className, @NotNull File ctxtFile) {
        try {
            Properties props = loadProperties(ctxtFile);
            CompilationUnitContext context;

            if (ctxtFile.canWrite()) {
                context = CompilationUnitContext.writable(className, ctxtFile);
            } else {
                context = CompilationUnitContext.readOnly(className);
            }

            context.setComments(fromProperties(props));

            return context;
        }
        catch (IOException e) {
            return null;
        }
    }

    static void writeToFile(@NotNull CompilationUnitContext context, @NotNull File ctxtFile) throws IOException {
        Properties props = toProperties(context.getComments());

        File parentDir = ctxtFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (OutputStream out = new FileOutputStream(ctxtFile)) {
            props.store(out, "BlueJ class context");
        }
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
