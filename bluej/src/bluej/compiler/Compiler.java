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
 * @version $Id: Compiler.java 2197 2003-10-02 04:12:34Z ajp $
 */
abstract class Compiler
{
	public abstract void setDestDir(File destDir);
	public abstract void setClassPath(String classPath);
	public abstract void setDebug(boolean debug);
	public abstract boolean compile(File[] sources, CompileObserver observer);
}
