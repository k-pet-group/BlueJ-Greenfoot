// NO PACKAGE.

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.text.Keymap;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
  * Usage: 
  * java Installer build <properties_file>
  *
  * This modifes the Installer.class file, which can then be run as
  * 
  *   java Installer
  *
  * @version $Id: Installer.java 407 2000-03-10 03:19:23Z mik $
  *
  * @author  Michael Kolling
  * @author  based partly on code by Andrew Hunt, Toolshed Technologies Inc.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  */

public class Installer extends JFrame
	implements ActionListener
{
    private static final String libDir = "lib";
    private static final String syslibs = "syslibs.properties";
    private static final String standardClasses = "classes.zip";

    private static final String nl = System.getProperty("line.separator");
    private static final char slash = File.separatorChar;
    private static final String colon = File.pathSeparator;

    static final int BUFFER_SIZE=8192;

    static String[] classpath;   // an array with all the paths in CLASSPATH

    Color backgroundColour;
    Color textColour;
    JTextField directoryField;
    JTextField javaField;
    JLabel textLabel1;
    JLabel textLabel2;
    JButton installButton;
    JButton cancelButton;
    JProgressBar progress; 
    int progressPercent = 0;
    Timer timer;
    String javaHome;
    String currentDirectory;
    String architecture;
    String javaVersion;

    String installationDir = "";
    String javaPath = "";

    Hashtable properties;
    long myTotalBytes;


    /*
     * Default behaviour for JTextFields is to generate an ActionEvent when
     * "Enter" is pressed. We don't want that. Here, we remove the Enter key 
     * from the keymap used by all JTextFields. Then we can use the default
     * button for dialogs (Enter will then activate the default button).
     */
    static {
	JTextField f = new JTextField();
	KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
	Keymap map = f.getKeymap();
	map.removeKeyStrokeBinding(enter);
    }


    public static void main(String[] args) 
    {
	classpath = getClassPath(System.getProperty("java.class.path"));

	if (args.length > 0) {
	    if (args[0].equals("build")) {
		buildInstaller(args[1]);
		System.exit(0);
	    }
	}

	// Install time
	new Installer();
    }


    // ===========================================================
    // Build Time
    // ===========================================================

    private static void buildInstaller(String filename)
    {
	try {
	    Hashtable capsule = loadProperties(filename);
	    RandomAccessFile out = new RandomAccessFile("Installer.class",
							"rw");
	    BufferedInputStream in = new BufferedInputStream(
		     new FileInputStream((String)capsule.get("pkgJar")));
	    long totalBytes = 0;
	    int bytesRead;
	    byte[] cbuf = new byte[BUFFER_SIZE];
	    out.seek(out.length());
	    System.err.println ("Original file is " + out.length());
	    long orig = out.length();

	    // First, write out the capsule values
	    ObjectOutputStream ostr = 
		new ObjectOutputStream(new FileOutputStream(out.getFD()));
	    if (ostr == null)
		System.err.println ("Could not open output file");
	    ostr.writeObject(capsule);
	    ostr.flush();

	    // Then copy the jar.
	    while ((bytesRead = in.read(cbuf,0,BUFFER_SIZE)) != -1) {
		out.write(cbuf,0,bytesRead);
		totalBytes += bytesRead;
	    }

	    System.err.println ("Jar is " + totalBytes + " bytes.");
	    totalBytes = out.length() - orig;
	    System.err.println ("Added a total of " + totalBytes + " bytes.");
	    System.err.println ("New size is " + out.length());
	    out.writeLong(totalBytes);
	    out.close();
	    in.close();
	    System.err.println ("Done.");
	} catch (IOException e) {
	    System.err.println ("Couldn't build installer: " + e.getMessage());
	    System.exit(1);
	}
    } 


    /**
     ** Helper function to read a GIF off the disk.
     **/
    public static byte[] readGIF(String name)
    {
	int len = (int)(new File((String)name)).length();
	byte[] buffer = new byte[len];
	System.err.println ("Loading gif '" + name + "' (" + len + " bytes)");
	try {
	    FileInputStream fs = new FileInputStream((String)name);
	    fs.read(buffer,0,len);
	    fs.close();
	} catch (IOException ge) {
	    System.err.println ("Couldn't load gif: " + ge.getMessage());
	}
		
	return buffer;
    }

    /**
     ** Load the properties file and create a capsule to be used at install
     ** time.
     **/
    public static Hashtable loadProperties(String fileName) {

	Hashtable capsule = new Hashtable();
	Properties props = new Properties();
	try {
	    FileInputStream rdr = new FileInputStream(fileName);
	    props.load(rdr);
	} catch (Exception e) {
	    System.err.println ("Exception " + e.getMessage());
	}

	String cspec = "color.";
	String gspec = "gif.";
	Enumeration list = props.keys();
	while (list.hasMoreElements()) {
	    String key = (String)list.nextElement();
	    Object value = props.getProperty(key);
	    // take out the install. prefix
	    key = key.substring(key.indexOf('.')+1);

	    // If value is something we need to translate (like a GIF, or a
	    // color) do it here and now.
	    if (key.regionMatches(0,cspec,0,cspec.length())) {
				// It's a color
		StringTokenizer tok = new StringTokenizer((String)value," \t,",false);
		int r = (Integer.valueOf(tok.nextToken())).intValue();
		int g = (Integer.valueOf(tok.nextToken())).intValue();
		int b = (Integer.valueOf(tok.nextToken())).intValue();
		value = new Color(r,g,b);
	    } else if (key.regionMatches(0,gspec,0,gspec.length())) {
				// It's a GIF89A
		byte[] buffer = readGIF((String)value);
		value = buffer;
	    }
	    capsule.put(key,value);
	}

	return capsule;
    }

    // ===========================================================
    // Install time
    // ===========================================================

    public Installer()
    {
	super();
	javaHome = System.getProperty("java.home");
	currentDirectory = System.getProperty("user.dir");
        architecture = System.getProperty("os.arch");
        javaVersion = System.getProperty("java.version");

	unpackTo(false);
	makeWindow();
    }


    /**
     ** Check for any required classes, show a help message if not found.
     **/
    public void checkDepends() {
	String dep = (String)getProperty("requires");
	if (dep != null) {
	    StringTokenizer tok = new StringTokenizer(dep," \t,:;",false);
	    while (tok.hasMoreTokens()) {
		String name = tok.nextToken();
		try {
		    Class.forName(name);
		} catch (ClassNotFoundException e) {
		    String help = (String)getProperty("requires.help." + name);
		    if (help == null) help = "";
		    notifyError("Required class " + name + " not found.\n" + 
				help, "Required class missing.");
		}
	    }
	}
	String fileName = (String)getProperty("requiresFile");
	if (fileName != null) {
	    fileName = replace(fileName, '~', currentDirectory);
	    fileName = replace(fileName, '!', javaHome);
	    fileName = replace(fileName, '@', architecture);
	    if(! (new File(fileName).exists())) {
		String help = (String)getProperty("requiresFile.help");
		if (help == null) help = "";
		notifyError(help, "Required file missing.");
	    }
	}
    }

    /**
     ** Internal GUI event dispatch.	We can't use listeners.
     **/
    public void actionPerformed(ActionEvent evt) {
	Object src = evt.getSource();
	if(src == installButton) {
	    InstallThread p = new InstallThread();
	    p.setPriority(Thread.MIN_PRIORITY + 1);
	    p.start();
	    timer.start();
	}
	else if(src == cancelButton)
	    doCancel();
    }

    class InstallThread extends Thread {
	public void run() {
	    doInstall();
	}
    }
       

    /**
     ** Install button action
     **/
    public void doInstall() {

	try {
	    setInstallDir(directoryField.getText());
	    if (installDir().length() == 0)
		throw new RuntimeException("No directory specified");

	    setJavaPath(javaField.getText());
	    if (getJavaPath().length() == 0)
		throw new RuntimeException("No java executable specified");

	    // Make sure installDir is accessible (make it if need be)

	    unpackTo(true);

	    // Write the scripts, if this is an application
	    if (getProperty("exeName") != null) {
		String osname = System.getProperty("os.name");

		if(osname == null) {	// if we don't know, write both
		    writeWindows();
		    writeUnix();
		}
		else if(osname.startsWith("Windows")) {
		    if( javaVersion.startsWith("1.3"))
                        writeWindows();
                    else
                        writeWindows12();
                }
		else if( javaVersion.startsWith("1.3"))
		    writeUnix(); // for 1.3 and later
		else if( osname.startsWith("Linux"))
		    writeUnix12(false);
		else
		    writeUnix12(true);
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	    finish("Installation FAILED: ", e.getMessage());
	    return;
	}

	if (getProperty("exeName") != null) {
	    finish("BlueJ has been installed to " + installDir(),
		   "To run it, execute \"" + 
		   (String)getProperty("exeName") + "\".");
	} else {
	    finish("The package has been installed to " + installDir(), "");
	}

    }

    /**
     ** Cancel button action
     **/
    public void doCancel() {
	System.exit(1);
    }


    /**
     ** Update the status dialog
     **/
    public void setStatus(String text)
    {
	textLabel1.setText(text);
	textLabel1.repaint();
    }

    /**
     ** Show message in main window and finish.
     **/
    public void finish(String msg1, String msg2) {
	textLabel1.setText(msg1);
	textLabel2.setText(msg2);
	installButton.setEnabled(false);
	cancelButton.setText("Done");
	getRootPane().setDefaultButton(cancelButton);
    }

    /**
     ** Pop up a dialog box with the message.
     **/
    public void notifyError(String error, String msg) {
	JOptionPane.showMessageDialog(this, error);
	finish(msg, "Installation aborted."); 
    }


    /**
     ** Create and show the main window
     **/
    public void makeWindow() 
    {
	backgroundColour = (Color)getProperty("color.background");
	textColour = (Color)getProperty("color.text");

	String title = (String)getProperty("title");
	if(title != null)
	    setTitle(title);

	JPanel mainPanel = (JPanel)getContentPane();
	mainPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
	mainPanel.setBackground(backgroundColour);

	// insert logo
	Image img = getToolkit().createImage((byte[])getProperty("gif.logo"));
	JLabel logoLabel = new JLabel(new ImageIcon(img));
	mainPanel.add(logoLabel, BorderLayout.NORTH);

	    JPanel buttonPanel = new JPanel();
	    buttonPanel.setBackground(backgroundColour);

	    installButton = new JButton("Install");
	    buttonPanel.add(installButton);
	    installButton.addActionListener(this);

	    cancelButton = new JButton("Cancel");
	    buttonPanel.add(cancelButton);
	    cancelButton.addActionListener(this);

	mainPanel.add(buttonPanel, BorderLayout.SOUTH);


	JPanel centrePanel = new JPanel(new BorderLayout(10, 10));
	centrePanel.setBackground(backgroundColour);

	    JPanel optionPanel = new JPanel(new GridLayout(2, 2, 8, 8));
	    optionPanel.setBackground(backgroundColour);

	    directoryField = new JTextField(20);
	    directoryField.setText(currentDirectory);

	    javaField = new JTextField(20);
            String javaBase;
            if(javaHome.endsWith("jre"))
                javaBase = javaHome.substring(0, javaHome.length()-4);
            else
                javaBase = javaHome;
            javaField.setText(javaBase +
                              File.separatorChar +
                              "bin" +
                              File.separatorChar +
                              "java");

	    optionPanel.add(new Label("Directory to install to:"));
	    optionPanel.add(directoryField);
	    optionPanel.add(new Label("Name of Java executable:"));
	    optionPanel.add(javaField);

	centrePanel.add(optionPanel, BorderLayout.CENTER);

	JPanel textPanel = new JPanel(new GridLayout(0, 1));
	textPanel.setBackground(backgroundColour);

	    progress = new JProgressBar(); 
	    textPanel.add(progress);
	    textLabel1 = new JLabel();
	    textPanel.add(textLabel1);
	    textLabel2 = new JLabel();
	    textPanel.add(textLabel2);

	    String tagline = (String)getProperty("tagline");
	    if(tagline != null)
		textLabel2.setText(tagline);

	centrePanel.add(textPanel, BorderLayout.SOUTH);

	mainPanel.add(centrePanel, BorderLayout.CENTER);

	getRootPane().setDefaultButton(installButton);

	pack();
	setLocation(100,100);
	setVisible(true);

	checkDepends();

        //Create a timer to update progress
        timer = new Timer(50, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
		progress.setValue(progressPercent);	
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
	    public void windowActivated(WindowEvent e) {
		installButton.requestFocus();
	    }
        });
    }

    // ===========================================================
    // Script generation for BlueJ startup script
    // ===========================================================

    /**
     ** Write out a Unix, Bourne shell script to start the application
     ** For JDK 1.3 and later
     **/
    public void writeUnix() throws IOException 
    {

	File outputFile = new File(installDir(), (String)getProperty("exeName"));
	FileWriter out = new FileWriter(outputFile.toString());
	out.write("#!/bin/sh\n");
	out.write("APPBASE=" + installDir() + "\n");
	String commands;
        commands = getProperty("unixCommands").toString();
	if(commands != null) {
	    commands = replace(commands, '~', "$APPBASE");
	    commands = replace(commands, '!', javaHome);
	    commands = replace(commands, '@', architecture);
	    out.write(commands);
	    out.write("\n");
	}
        String classpath;
        classpath = getProperty("classpath").toString();
	classpath = classpath.replace(';', ':');
	classpath = replace(classpath, '~', "$APPBASE");
	classpath = replace(classpath, '!', javaHome);
        classpath = replace(classpath, '@', architecture);
	out.write("CLASSPATH=" + classpath + ":$CLASSPATH\n");
	out.write("export CLASSPATH\n");
	out.write(getJavaPath() + " " + getProperty("javaOpts") + " -D" + 
		  getProperty("installDirProp") + "=$APPBASE "+ 
		  getProperty("mainClass") + " $*\n");
	out.close();
		
	try {
	    Runtime.getRuntime().exec("chmod 755 " + outputFile);
	} catch(Exception e) {
	    // ignore it - might not be Unix
	}
    }

    /**
     ** Write out a Unix, Bourne shell script to start the application
     ** For JDK 1.2.2
     **/
    public void writeUnix12(boolean localJPDA) throws IOException 
    {

	File outputFile = new File(installDir(), (String)getProperty("exeName"));
	FileWriter out = new FileWriter(outputFile.toString());
	out.write("#!/bin/sh\n");
	out.write("APPBASE=" + installDir() + "\n");
	String commands;
        if (localJPDA)
            commands = getProperty("unixCommands.localJPDA").toString();
        else
            commands = getProperty("unixCommands.systemJPDA").toString();
	if(commands != null) {
	    commands = replace(commands, '~', "$APPBASE");
	    commands = replace(commands, '!', javaHome);
	    commands = replace(commands, '@', architecture);
	    out.write(commands);
	    out.write("\n");
	}
        String classpath;
        if (localJPDA)
            classpath = getProperty("classpath.localJPDA").toString();
        else
            classpath = getProperty("classpath.systemJPDA").toString();
	classpath = classpath.replace(';', ':');
	classpath = replace(classpath, '~', "$APPBASE");
	classpath = replace(classpath, '!', javaHome);
        classpath = replace(classpath, '@', architecture);
	out.write("CLASSPATH=" + classpath + ":$CLASSPATH\n");
	out.write("export CLASSPATH\n");
	out.write(getJavaPath() + " " + getProperty("javaOpts.1.2") + " -D" + 
		  getProperty("installDirProp") + "=$APPBASE "+ 
		  getProperty("mainClass") + " $*\n");
	out.close();
		
	try {
	    Runtime.getRuntime().exec("chmod 755 " + outputFile);
	} catch(Exception e) {
	    // ignore it - might not be Unix
	}
    }

    /**
     ** Write out an MSDOS style batch file to start the application.
     ** (JDK 1.3 and later)
     **/
    public void writeWindows() throws IOException 
    {
	File outputFile = new File(installDir(),
				   (String)getProperty("exeName") + ".bat");
			
	FileWriter out = new FileWriter(outputFile.toString());
	out.write("@echo off\r\n");
	out.write("set OLDPATH=%CLASSPATH%\r\n");
	out.write("set APPBASE=" + installDir() + "\r\n");
	String commands = getProperty("winCommands").toString();
	if(commands != null) {
	    commands = replace(commands, '~', "%APPBASE%");
	    commands = replace(commands, '!', javaHome);
	    commands = replace(commands, '@', architecture);
	    out.write(commands);
	    out.write("\r\n");
	}
	String classpath = getProperty("classpath").toString();
	classpath = classpath.replace('/', '\\');
	classpath = replace(classpath, '~', "%APPBASE%");
	classpath = replace(classpath, '!', javaHome);
	classpath = replace(classpath, '@', architecture);
	out.write("set CLASSPATH=" + classpath + ";%CLASSPATH%\r\n");
	out.write(getJavaPath() + " " + getProperty("javaOpts") + " -D" + 
		  getProperty("installDirProp") + "=%APPBASE% "+ 
		  getProperty("mainClass") + 
		  " %1 %2 %3 %4 %5 %6 %7 %8 %9\r\n");
	out.write("set CLASSPATH=%OLDPATH%\r\n");

	out.close();
    }

    /**
     ** Write out an MSDOS style batch file to start the application.
     **/
    public void writeWindows12() throws IOException 
    {
	File outputFile = new File(installDir(),
				   (String)getProperty("exeName") + ".bat");
			
	FileWriter out = new FileWriter(outputFile.toString());
	out.write("@echo off\r\n");
	out.write("set OLDPATH=%CLASSPATH%\r\n");
	out.write("set APPBASE=" + installDir() + "\r\n");
	String commands = getProperty("winCommands.12").toString();
	if(commands != null) {
	    commands = replace(commands, '~', "%APPBASE%");
	    commands = replace(commands, '!', javaHome);
	    commands = replace(commands, '@', architecture);
	    out.write(commands);
	    out.write("\r\n");
	}
	String classpath = getProperty("classpath.localJPDA").toString();
	classpath = classpath.replace('/', '\\');
	classpath = replace(classpath, '~', "%APPBASE%");
	classpath = replace(classpath, '!', javaHome);
	classpath = replace(classpath, '@', architecture);
	out.write("set CLASSPATH=" + classpath + ";%CLASSPATH%\r\n");
	out.write(getJavaPath() + " " + getProperty("javaOpts.1.2") + " -D" + 
		  getProperty("installDirProp") + "=%APPBASE% "+ 
		  getProperty("mainClass") + 
		  " %1 %2 %3 %4 %5 %6 %7 %8 %9\r\n");
	out.write("set CLASSPATH=%OLDPATH%\r\n");

	out.close();
    }


    // ===========================================================
    // File I/O (JAR extraction)
    // ===========================================================

    /**
     ** Find a classfile in the classpath
     **/
    InputStream getFileFromClasspath(String filename) throws IOException
    {
	for(int i = 0; i < classpath.length; i++) {
	    if(classpath[i].endsWith(".zip") || classpath[i].endsWith(".jar")) {
		InputStream ret = readZip(classpath[i], filename);
		if(ret !=null)
		    return ret;
	    }
	    else {
		if(File.separatorChar != '/')
		    filename = filename.replace('/', File.separatorChar);
		String fullpath = classpath[i] + File.separator + filename;
		File fd = new File(fullpath);
		
		if(fd.exists())
		    return new FileInputStream(fd);
	    }
	}
		
	throw new FileNotFoundException(filename);
    }

    InputStream readZip(String classzip, String arg) throws IOException
    {
	ZipFile zipf = new ZipFile(classzip);
	ZipEntry entry = zipf.getEntry(arg);

	if(entry == null)
	    return null;

	long size = entry.getCompressedSize();

	InputStream is = zipf.getInputStream(entry);
	return is;
    }

    public static String[] getClassPath(String cp)
    {
	String[] entries = {};
		
	try {
	    Vector v = new Vector();
			
	    StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
	    while(st.hasMoreTokens()) {
		String entry = st.nextToken();
		File entryFile = new File(entry);
		if(entryFile.exists())
		    v.addElement(entry);
	    }
			
	    entries = new String[v.size()];
	    v.copyInto(entries);
	} catch(Exception e) {
	    e.printStackTrace();
	}
	
	return entries;
    }

    /**
     ** Grab the jar data from the class file and unjar it into the 
     ** install directory.
     **/
    public void unpackTo(boolean doJar) {
	try {
	    InputStream cpin = getFileFromClasspath("Installer.class");
	    FileOutputStream cpout = new FileOutputStream("Installer.class.tmp");
	    byte[] buffer = new byte[8192];
	    int len;
	    while((len = cpin.read(buffer)) != -1)
		cpout.write(buffer, 0, len);
	    cpin.close();
	    cpout.close();
			
	    RandomAccessFile in = new RandomAccessFile("Installer.class.tmp","r");
	    in.seek(in.length() - 8);
	    long size = in.readLong();
	    in.seek(in.length() - 8 - size);
	    myTotalBytes = size; // this is wrong!!
	    ObjectInputStream istr = new ObjectInputStream(new FileInputStream(in.getFD()));
	    try {
		properties = (Hashtable)istr.readObject();
	    } catch (ClassNotFoundException nf) {
		System.err.println (nf.getMessage());
	    }
	    if (doJar) {
		dumpJar(installDir(), new FileInputStream(in.getFD()));
	    }
	    in.close();
			
	    (new File("Installer.class.tmp")).delete();
	} catch (Exception e) {
	    notifyError("Installer failed to open: " + e.getMessage(),
			"Could not open install file.");
	}
    }


    /**
     ** Recusrively make directories needed for a file.
     **/
    public void makeDirsFor(String start, String path) {
	String sofar = null;
	StringTokenizer tok = new StringTokenizer(path,"/",false);
	sofar = start;
	while (tok.hasMoreTokens()) {
	    String part = tok.nextToken();
	    if (tok.hasMoreTokens()) {
		File d = new File(sofar + File.separatorChar + part);
		sofar = d.toString();
		d.mkdirs();
	    }
	}
	return;
    }

    /**
     ** Extract a JAR from a file stream to the given directory on disk.
     **/
    public void dumpJar(String dir, FileInputStream in) 
	throws IOException, ZipException
    {
	makeDirsFor(dir,"");
	long bytesRead = 0;

	// Make a zip stream
	ZipInputStream zip = new ZipInputStream(in);

	byte[] buffer = new byte[BUFFER_SIZE];
	while (true) {
	    // Get a zip entry
	    ZipEntry z = zip.getNextEntry();
	    if (z == null)
		break;

	    String name = dir + "/" + z.getName();

	    if (z.isDirectory()) {
		File d = new File(name);
		d.mkdirs();
		continue;
	    }

	    // Make any necessary directories
	    if (z.getName().indexOf('/') != -1) {
		makeDirsFor(dir,z.getName());
	    }

	    // Create the file
	    FileOutputStream out;
	    try {
		out = new FileOutputStream(name);
	    } catch (FileNotFoundException e) {
		throw new IOException("Couldn't write to specified file/directory");
	    }

	    int len;

	    setStatus("extracting: " + name);
	    while ((len = zip.read(buffer,0, BUFFER_SIZE)) != -1) {
		bytesRead += len;
		out.write(buffer,0,len);
	    }
	    progressPercent = (int)((bytesRead*100/myTotalBytes)/4);
	    zip.closeEntry();
	    out.close();
	}

	zip.close();
	timer.stop();
	progress.setValue(100);	
    }

    /**
     ** Find and return the full path to an archive in CLASSPATH
     **/
    private String getFullPath(String archiveName)
    {
	for(int i = 0; i < classpath.length; i++) {
	    if(classpath[i].endsWith(archiveName))
		return classpath[i];
	}
	return null;
    }

    /**
     ** Constructs a new string by replacing the character in
     ** pattern found in src by the string subst
     **/
    String replace(String src, char pattern, String subst)
    {
	char[] patterns = { pattern };
	String patString = new String(patterns);
	StringTokenizer tokenizer = new StringTokenizer(src, patString, true);
	StringBuffer ret = new StringBuffer();

	while(tokenizer.hasMoreElements()) {
	    String tok = tokenizer.nextToken();
	    if(tok.length() == 1 && tok.equals(patString))
		ret.append(subst);
	    else
		ret.append(tok);
	}
		
	return ret.toString();
    }
	
    // =============== property access ================

    public Object getProperty(String key) {
	return properties.get(key);
    }

    public String installDir() {
	return installationDir;
    }

    public void setInstallDir(String dir) {
	installationDir = dir;
    }

    public String getJavaPath() {
	return javaPath;
    }

    public void setJavaPath(String p) {
	javaPath = p;
    }

} // End class install

