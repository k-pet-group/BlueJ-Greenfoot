package bluej.compiler;

import java.io.File;

/**
 * Compiler class - an abstract interface to a source -> bytecode compiler.
 * This can be implemented by different compiler implementations.
 *
 * Currently known implementations: JavacCompiler, JikesCompiler.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Compiler.java 1765 2003-04-09 05:56:45Z ajp $
 */
public abstract class Compiler
{
	public abstract void setDestDir(File destDir);
	public abstract void setClassPath(String classPath);
	public abstract void setDebug(boolean debug);
	public abstract boolean compile(File[] sources, CompileObserver observer);
}
