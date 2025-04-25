package bluej.classmgr;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassFileReader {
    public static String readSourceFile(File classFile) {
        try (InputStream in = new FileInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            return node.sourceFile;
        } catch (IOException e) {
            return null;
        }
    }
}