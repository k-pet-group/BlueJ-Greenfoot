package bluej.parser.context;

import kotlin.metadata.jvm.KotlinClassMetadata;
import org.objectweb.asm.*;
import kotlin.Metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class KotlinMetadataReader {

    public static KotlinClassMetadata readMetadata(Path classFile) throws IOException {
        return KotlinMetadataReader.metadataFromBytes(Files.readAllBytes(classFile));
    }

    public static KotlinClassMetadata readMetadata(File classFile) throws IOException {
        byte[] byteArray = new byte[(int) classFile.length()];
        try (FileInputStream inputStream = new FileInputStream(classFile)) {
            inputStream.read(byteArray);
        }
        return KotlinMetadataReader.metadataFromBytes(byteArray);
    }

    private static Metadata mapToMetadata(Map<String, Object> values) {
        int[] mv = toIntArray(values.get("mv"));
        int[] bv = toIntArray(values.get("bv"));
        String[] d1 = toStringArray(values.get("d1"));
        String[] d2 = toStringArray(values.get("d2"));
        String xs = (String) values.getOrDefault("xs", "");
        String pn = (String) values.getOrDefault("pn", "");
        int k = ((Number) values.getOrDefault("k", 1)).intValue();
        int xi = ((Number) values.getOrDefault("xi", 0)).intValue();

        // Anonymous implementation of kotlin.Metadata
        return new Metadata() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Metadata.class; }
            @Override public int[] mv() { return mv; }
            @Override public int[] bv() { return bv; }
            @Override public String[] d1() { return d1; }
            @Override public String[] d2() { return d2; }
            @Override public int k() { return k; }
            @Override public String xs() { return xs; }
            @Override public String pn() { return pn; }
            @Override public int xi() { return xi; }
        };
    }

    private static int[] toIntArray(Object o) {
        if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            int[] arr = new int[list.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = ((Number) list.get(i)).intValue();
            return arr;
        }
        if (o instanceof int[]) {
            return (int[]) o;
        }
        return new int[0];
    }

    private static String[] toStringArray(Object o) {
        if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            return list.toArray(new String[0]);
        }
        if (o instanceof String[]) {
            return (String[]) o;
        }
        return new String[0];
    }

    private static class MetadataHolder {
        Map<String, Object> values;
    }

    private static KotlinClassMetadata metadataFromBytes(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        MetadataHolder holder = new MetadataHolder();

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (Type.getDescriptor(Metadata.class).equals(desc)) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        Map<String, Object> values = new HashMap<>();

                        @Override
                        public void visit(String name, Object value) {
                            values.put(name, value);
                        }

                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            List<Object> list = new ArrayList<>();
                            values.put(name, list);
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String n, Object value) {
                                    list.add(value);
                                }
                            };
                        }

                        @Override
                        public void visitEnd() {
                            holder.values = values;
                        }
                    };
                }
                return null;
            }
        }, 0);

        if (holder.values == null) {
            return null; // Not a Kotlin class
        }

        var annotation = mapToMetadata(holder.values);

        var test = KotlinClassMetadata.readLenient(annotation);

        return KotlinClassMetadata.readStrict(annotation);
    }
}
