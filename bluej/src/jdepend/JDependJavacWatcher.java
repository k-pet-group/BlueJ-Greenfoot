package sun.tools.javac;

import sun.tools.java.ClassDeclaration;
import sun.tools.javac.BatchEnvironment;
import sun.tools.javac.SourceClass;

/**
 ** @version $Id: JDependJavacWatcher.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** CompileWatcher interface - for classes that are interested in knowing about
 ** compilation of Java sources.
 **/

public interface JDependJavacWatcher
{
	void notifyParsed(ClassDeclaration decl, SourceClass src, BatchEnvironment env);
	void notifyCompiled(SourceClass src, BatchEnvironment env);
}
