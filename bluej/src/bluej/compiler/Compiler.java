package bluej.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import bluej.Config;

/**
 * Compiler class - an abstract interface to a source -> bytecode compiler. This
 * can be implemented by different compiler implementations.
 * 
 * Currently known implementations: JavacCompiler, JikesCompiler.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Poul Henriksen
 * @version $Id: Compiler.java 2501 2004-04-19 13:57:47Z polle $
 */
abstract class Compiler
{
    public static final String COMPILER_OPTIONS = "bluej.compiler.options";
        
    private File destDir;
    private String classPath;
    private boolean debug;
    private boolean deprecation;
    
    
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setDeprecation(boolean deprecation) {
        this.deprecation = deprecation;
    }

   
    public String getClassPath() {
        return classPath;
    }  
    
    public boolean isDebug() {
        return debug;
    }
  
    public boolean isDeprecation() {
        return deprecation;
    }
   
    public File getDestDir() {
        return destDir;
    }

    /**
     * Creates a list of the options that should be used for the compilation.
     * 
     * @return A list of compile options.
     */
    protected List getCompileOptions() {
        List args = new ArrayList();
        
        if(getDestDir() != null) {
            args.add("-d");
            args.add(getDestDir().getPath());
        }

        if(getClassPath() != null) {
            args.add("-classpath");
            args.add(getClassPath());
        }
        
        if(isDebug())
            args.add("-g");
            
        if(isDeprecation())
            args.add("-deprecation");   
        
        if(Config.systemContainsKey(COMPILER_OPTIONS)) {
            addUserSpecifiedOptions(args);
        } else {
            addDefaultOptions(args);            
        }    
        
        return args;
    }    
    
    
    private void addDefaultOptions(List args) {        
        String majorVersion = System.getProperty("java.vm.version").substring(0,3);        
        args.add("-source");
        args.add(majorVersion);
    }

    private void addUserSpecifiedOptions(List args) {
        // add user specified compiler options
        String compilerOptions = Config.getPropString(COMPILER_OPTIONS, null);
        if(compilerOptions != null) {
            StringTokenizer st = new StringTokenizer(compilerOptions);
            while(st.hasMoreTokens()) {              
                args.add(st.nextToken());
            }
        }
    }
    
    
    //  ========================= ABSTRACT METHODS ==========================        

    public abstract boolean compile(File[] sources, CompileObserver observer);   

   
}