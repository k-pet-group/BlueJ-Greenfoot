package bluej.compiler;

import java.io.File;
import java.util.ArrayList;
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
 * @version $Id: Compiler.java 2500 2004-04-19 11:37:19Z polle $
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
        
        if(! System.getProperty("java.vm.version").startsWith("1.3")) {
            args.add("-source");
            args.add("1.4");
        }
        
        /** Not used at present...
        // add user specified compiler options
        List userOptions = CompileUtility.getUserCompilerOptions();
        if(userOptions != null && userOptions.size() > 0) {
            Iterator it = userOptions.iterator();
            while(it.hasNext()) {
                args.add((String)it.next());
            }
        }
        */
        
        return args;
    }
    
    
    //  ========================= ABSTRACT METHODS ==========================    
    
    public abstract boolean compile(File[] sources, CompileObserver observer);
    

    //  ========================= STATIC METHODS ============================
        
    /**
     * returns user specified compiler options from bluej defs.
     *
     * @return compiler options
     */
    public static List getUserCompilerOptions()
    {
        ArrayList options = new ArrayList();   
        String compilerOptions = Config.getPropString(COMPILER_OPTIONS, null);
        if(compilerOptions != null) {
            StringTokenizer st = new StringTokenizer(compilerOptions);
            while(st.hasMoreTokens()) 
                options.add(st.nextToken());
        }
        return options;
    }
}