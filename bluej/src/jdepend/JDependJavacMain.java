package sun.tools.javac;

import sun.tools.javac.BatchEnvironment;
import sun.tools.javac.SourceClass;
import sun.tools.java.*;
import sun.tools.asm.Assembler;

import java.util. *;
import java.io. *;

/**
 ** @version $Id: JDependJavacMain.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** JavacMain class - an interface to Sun's javac compiler to get class dependencies
 ** Modifies the main program of the Java compiler from the Sun JDK
 **/

public class JDependJavacMain extends sun.tools.javac.Main
{
	OutputStream myOut;	// Same as super.out
	
	/**
	 ** Constructor.
	 **/
	public JDependJavacMain(OutputStream out)
	{
		super(out, "javac");
		
		this.myOut = out;
	}
    
	/**
	 ** Run the compiler
	 **/
	public synchronized boolean compile(String argv[], JDependJavacWatcher watcher)
	{
		String classPathString = System.getProperty("java.class.path");
		File destDir = null;
		int flags = F_INNERCLASSES | F_PRINT_DEPENDENCIES;
		long tm = System.currentTimeMillis();
		Vector v = new Vector();
		boolean nowrite = false;
		String props = null;
		String encoding = null;

		// Parse arguments
		for(int i = 0; i < argv.length; i++)
		{
			if (argv[i].equals("-g"))
			{
				flags &= ~F_OPTIMIZE;
				flags |= F_DEBUG;
			}
			else if (argv[i].equals("-O"))
			{
				flags &= ~F_DEBUG;
				flags |= F_OPTIMIZE;
			}
			else if (argv[i].equals("-nowarn"))
				flags &= ~F_WARNINGS;
			else if (argv[i].equals("-deprecation"))
				flags |= F_DEPRECATION;
			else if (argv[i].equals("-debug"))
				flags |= F_DUMP;
			else if (argv[i].equals("-depend"))
				flags |= F_DEPENDENCIES;
			else if (argv[i].equals("-verbose"))
				flags |= F_VERBOSE;
			else if (argv[i].equals("-nowrite"))
				nowrite = true;
			else if (argv[i].equals("-classpath"))
			{
				if ((i + 1) < argv.length)
					classPathString = argv[++i];
				else
				{
					error("-classpath requires argument");
					usage();
					return false;
				}
			}
			else if (argv[i].equals("-d"))
			{
				if ((i + 1) < argv.length)
				{
					destDir = new File(argv[++i]);
					if (!destDir.exists())
					{
						error(destDir.getPath() + " does not exist");
						return false;
					}
				}
				else
				{
					error("-d requires argument");
					usage();
					return false;
				}
			}
			else if(argv[i].startsWith("-"))
			{
				error("invalid flag: " + argv[i]);
				usage();
				return false;
			}
			else if(argv[i].endsWith(".java"))
				v.addElement(argv[i]);
			else
			{
				error("invalid argument: " + argv[i]);
				usage();
				return false;
			}
		}
		if (v.size() == 0)
		{
			usage();
			return false;
		}

		// Create batch environment
		if (classPathString == null)
			classPathString = ".";

		ClassPath classPath = new ClassPath(classPathString);
		BatchEnvironment env = new BatchEnvironment(myOut, classPath);

		env.flags |= flags;
		env.setCharacterEncoding(encoding);

		try
		{
			for(Enumeration e = v.elements(); e.hasMoreElements();)
			{
				File file = new File((String)e.nextElement());

				try
				{
					env.parseFile(new ClassFile(file));
				} catch(FileNotFoundException ee) {
					env.error(0, "cant.read", file.getPath());
				}
			}

			// Do a post-read check on all newly-parsed classes,
			// after they have all been read.
			for (Enumeration e = env.getClasses(); e.hasMoreElements();)
			{
				ClassDeclaration decl = (ClassDeclaration)e.nextElement();
					
				if (decl.getStatus() == CS_PARSED)
				{
					if (decl.getClassDefinition().isLocal())
						continue;
					try
					{
						ClassDefinition defn = decl.getClassDefinition(env);

						if((watcher != null)
						  && decl.isDefined()
						  && !defn.isSynthetic()
						  && (defn instanceof SourceClass))
							watcher.notifyParsed(decl, (SourceClass)defn, env);
					} catch(ClassNotFound ee) {}
				}
			}

			// compile all classes that need compilation
			ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
			boolean done;

			do
			{
				done = true;
				env.flushErrors();
				for (Enumeration e = env.getClasses(); e.hasMoreElements();)
				{
					ClassDeclaration c = (ClassDeclaration) e.nextElement();
					SourceClass src;

					switch (c.getStatus())
					{
					case CS_UNDEFINED:
						if(!env.dependencies())
							break;
						// fall through

					case CS_SOURCE:
						done = false;
						env.loadDefinition(c);
						if(c.getStatus() != CS_PARSED)
							break;
						// fall through

					case CS_PARSED:
						if(c.getClassDefinition().isInsideLocal())
							// the enclosing block will check this one
							continue;
						done = false;
						src = (SourceClass)c.getClassDefinition(env);
						src.check(env);
						c.setDefinition(src, CS_CHECKED);
						// fall through

					case CS_CHECKED:
						src = (SourceClass) c.getClassDefinition(env);
						// bail out if there were any errors
						if(src.getError())
						{
							c.setDefinition(src, CS_COMPILED);
							break;
						}
						done = false;
						buf.reset();
						src.compile(buf);
						c.setDefinition(src, CS_COMPILED);
						src.cleanup(env);

						if(watcher != null)
							watcher.notifyCompiled(src, env);

						if(src.getError() || nowrite)
							continue;

						String pkgName = c.getName().getQualifier().toString().replace('.', File.separatorChar);
						String className = c.getName().getFlatName().toString().replace('.', SIGC_INNERCLASS) + ".class";

						File file;

						if(destDir != null)
						{
							if(pkgName.length() > 0)
							{
								file = new File(destDir, pkgName);
								if(!file.exists())
									file.mkdirs();
								file = new File(file, className);
							}
							else
								file = new File(destDir, className);
						}
						else
						{
							ClassFile classfile = (ClassFile)src.getSource();

							if (classfile.isZipped())
							{
								env.error(0, "cant.write", classfile.getPath());
								continue;
							}
							file = new File(classfile.getPath());
							file = new File(file.getParent(), className);
						}

						// Create the file
						try
						{
							FileOutputStream out = new FileOutputStream(file.getPath());

							buf.writeTo(out);
							out.close();

							if(env.verbose())
								output(getText("main.wrote", file.getPath()));
						} catch(IOException ee) {
							env.error(0, "cant.write", file.getPath());
						}
					}
				}
			} while (!done);
		} catch(Error ee) {
			if (env.nerrors == 0 || env.dump())
			{
				ee.printStackTrace();
				env.error(0, "fatal.error");
			}
		} catch(Exception ee) {
			if (env.nerrors == 0 || env.dump())
			{
				ee.printStackTrace();
				env.error(0, "fatal.exception");
			}
		}

		boolean status = (env.nerrors == 0);

		env.flushErrors();
		env.shutdown();

		return status;
	}

	public static Enumeration getDependencies(SourceClass src)
	{
		return src.deps.elements();
	}
}
