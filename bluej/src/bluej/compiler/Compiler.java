package bluej.compiler;

/**
 ** @version $Id: Compiler.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** Compiler class - an abstract interface to a source -> bytecode compiler.
 ** This can be implemented by different compiler implementations.
 **
 ** Currently known implementations: JavacCompiler.
 **/

public abstract class Compiler
{
	public abstract void setDestDir(String destdir);
	public abstract void setClassPath(String classpath);
	public abstract void setDebug(boolean debug);
	public abstract boolean compile(String[] sources, CompileObserver observer);
}
