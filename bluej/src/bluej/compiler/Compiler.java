package bluej.compiler;

/**
 * Compiler class - an abstract interface to a source -> bytecode compiler.
 * This can be implemented by different compiler implementations.
 *
 * Currently known implementations: JavacCompiler, JikesCompiler.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Compiler.java 505 2000-05-24 05:44:24Z ajp $
 */
public abstract class Compiler
{
	public abstract void setDestDir(String destdir);
	public abstract void setClassPath(String classpath);
	public abstract void setDebug(boolean debug);
	public abstract boolean compile(String[] sources, CompileObserver observer);
}
