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
  * @version $Id: Installer.java 911 2001-05-24 07:28:51Z mik $
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
    private static final String nl = System.getProperty("line.separator");
    private static final char slash = File.separatorChar;
    private static final String colon = File.pathSeparator;


    // File to test for JDK (relative to javaPath):
    static private String jdkFile = "/lib/tools.jar";
    static private String jdkFile2 = "/Classes/jpda.jar";  // for MacOS

    static final int BUFFER_SIZE=8192;

    // user interface components
    Color backgroundColour;
    Color textColour;
    JTextField directoryField;
    JTextField javaField;
    JLabel textLabel1;
    JLabel textLabel2;
    JButton browseDirButton;
    JButton browseJdkButton;
    JRadioButton jdk12Button;
    JRadioButton jdk13Button;
    JButton installButton;
    JButton cancelButton;
    JProgressBar progress;

 
    int progressPercent = 0;
    javax.swing.Timer timer;

    String currentDirectory;    // the user's working dir
    String osname;              // "SunOS", "Windows*", "Linux*", etc
    String architecture;        // "sparc", "i386", etc
    String javaVersion;
    boolean isJDK12;

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
        if (args.length > 0) {
            if (args[0].equals("build"))
                buildInstaller(args[1]);
            else
                System.out.println("unknown argument: " + args[0]);
            System.exit(0);
        }
        else    // Install time
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
     * Helper function to read a GIF off the disk.
     */
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
     * Load the properties file and create a capsule to be used at install
     * time.
     */
    public static Hashtable loadProperties(String fileName) 
    {
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
        currentDirectory = System.getProperty("user.dir");
        if(currentDirectory.endsWith("bluej"))
            installationDir = currentDirectory;
        else
            installationDir = currentDirectory + File.separator + "bluej";
        osname = System.getProperty("os.name");
        architecture = System.getProperty("os.arch");
        javaVersion = System.getProperty("java.version");
        isJDK12 = javaVersion.startsWith("1.2");
        javaPath = findJavaPath();

        //System.out.println(javaPath);
        //System.out.println(osname);
        //System.out.println(javaVersion);
        //System.out.println(architecture);

        unpackTo(false);
        makeWindow();
    }

    private String findJavaPath()
    {
        String javaHome = System.getProperty("java.home");
        if(isJDKPath(javaHome))
            return javaHome;

        // try to remove "/jre"
        if(javaHome.endsWith("jre")) {
            javaHome = javaHome.substring(0, javaHome.length()-4);
            if(isJDKPath(javaHome))
                return javaHome;
        }

        // have a few wild guesses...

        String shortVersion = javaVersion.substring(0, javaVersion.length()-2);
        String[] tryPaths = { 
            "C:\\jdk" + javaVersion, 
            "C:\\jdk" + shortVersion, 
            "D:\\jdk" + javaVersion, 
            "D:\\jdk" + shortVersion, 
            "/usr/java", 
            "/usr/local/java",
            "/usr/jdk" + javaVersion, 
            "/usr/jdk" + shortVersion, 
            "/usr/local/jdk" + javaVersion, 
            "/usr/local/jdk" + shortVersion, 
            "/System/Library/Frameworks/JavaVM.framework", 
        };

        for(int i = 0; i < tryPaths.length; i++)
            if(isJDKPath(tryPaths[i]))
                return tryPaths[i];

        // give up
        return "";
    }

    /**
     * Handle button press.
     */
    public void actionPerformed(ActionEvent evt) 
    {
        Object src = evt.getSource();

        if(src == browseDirButton) {
            getInstallDirectory();
        }
        else if(src == browseJdkButton) {
            getJDKDirectory();
        }
        else if(src == installButton) {
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
     * Install button action
     */
    public void doInstall() 
    {
        readInputValues();
        if(! isJDKPath(javaPath)) {
            jdkPathProblem();
            return;
        }

        try {
            if(!checkInstallDir(installationDir, true))
                return;

            unpackTo(true);

            // Write the scripts, if this is an application
            if (getProperty("exeName") != null) {

                if(osname == null) {	// if we don't know, write both
                    writeWindows();
                    writeUnix();
                }
                else if(osname.startsWith("Windows")) {
                    if(isJDK12)
                        writeWindows12();
                    else
                        writeWindows();
                }
                else if(osname.startsWith("Mac")) {
                        writeMacOS();
                }
                else if( ! isJDK12)
                    writeUnix();      // for 1.3 and later
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
            finish("BlueJ has been installed to " + installationDir,
                   "To run it, execute \"" + 
                   (String)getProperty("exeName") + "\".");
        } else {
            finish("The package has been installed to "+installationDir, " ");
        }

    }

    /**
     * Cancel button action
     */
    private void doCancel() {
        System.exit(0);
    }

    /**
     * Get an installtion directory from the user via a file selection
     * dialogue.
     */
    private void getInstallDirectory()
    {
        String dirName = getDirName("Select installation directory");
        if(dirName != null) {
            if(dirName.endsWith("bluej"))
                installationDir = dirName;
            else
                installationDir = dirName + File.separator + "bluej";
            directoryField.setText(installationDir);
            checkInstallDir(installationDir, false);
        }
    }

    /**
     * Get the jdk directory from the user via a file selection
     * dialogue.
     */
    private void getJDKDirectory()
    {
        String dirName = getDirName("Select JDK directory");
        if(dirName != null) {
            javaPath = dirName;
            javaField.setText(javaPath);
            if(! isJDKPath(javaPath))
                jdkPathProblem();
        }
    }

    /**
     * Read the values that the user selected into the appropriate variables.
     */
    private void readInputValues()
    {
        installationDir = directoryField.getText();
        javaPath = javaField.getText();
        isJDK12 = jdk12Button.isSelected();
    }

    /**
     * Check that the current Java version is a full JDK. Warn if not.
     */
    public boolean isJDKPath(String path) 
    {
        if(osname.startsWith("Mac"))
            return true;   // check disabled for MacOS system

        String jdkFilePath = path + jdkFile;
        if(new File(jdkFilePath).exists())
            return true;
        else {
            jdkFilePath = path + jdkFile2;
            return new File(jdkFilePath).exists();
        }
    }

    /**
     * Update the status dialog
     */
    public void setStatus(String text)
    {
        textLabel1.setText(text);
        textLabel1.repaint();
    }

    /**
     * Show message in main window and finish.
     */
    public void finish(String msg1, String msg2) {
        textLabel1.setText(msg1);
        textLabel2.setText(msg2);
        installButton.setEnabled(false);
        cancelButton.setText("Done");
        getRootPane().setDefaultButton(cancelButton);
    }

    /**
     * Pop up a dialog box with the error message. After an error,
     * installation cannot proceed.
     */
    public void notifyError(String error, String msg) {
        JOptionPane.showMessageDialog(this, error);
        finish(msg, "Installation aborted."); 
    }


    /**
     * Inform user of invalid jdk.
     */
    private void jdkPathProblem() 
    {
        notifyProblem(
                      "The Java directory you have specified is not a valid \n" +
                      "JDK directory. The JDK directory is the directory \n" +
                      "that JDK (aka Java 2 SDK) was installed to. It must \n" +
                      "have a subdirectory \"lib\" with a file named \n" +
                      "\"tools.jar\" in it.");
    }

    /**
     * Inform user of invalid install dir. Return true if everything is fine.
     */
    private boolean checkInstallDir(String dirName, boolean make) 
    {
        File installDir = new File(dirName);
        if(installDir.exists()) {
            if(installDir.isDirectory())
                return true;
            else {
                notifyProblem("The name you specified exists\n" +
                              "and is not a directory. Cannot\n" +
                              "install there.");
                return false;
            }
        }

        else {  // dir does not exist

            // see whether parent dir exists
            File parent = installDir.getParentFile();
            if(parent.exists()) {
                if(parent.isDirectory()) {
                    // parent exists. that's fine. create dir if requested.
                    if(make)
                        installDir.mkdir();
                    return true;
                }
                else {
                    notifyProblem(parent.getAbsolutePath() + " is not\n" +
                                  "a directory. Cannot install there.");
                    return false;
                }
            }
            else {
                notifyProblem(
                              "The directory " + parent.getAbsolutePath() +
                              "\ndoes not exist.\n" +
                              "Please check the path and enter again.");
                return false;
            }
        }
    }

    /**
     * Pop up a dialog box with the message. After a problem,
     * installation can proceed.
     */
    private void notifyProblem(String problem) {
        JOptionPane.showMessageDialog(this, problem);
    }


    /**
     * Create and show the main window
     */
    public void makeWindow() 
    {
        backgroundColour = (Color)getProperty("color.background");
        textColour = (Color)getProperty("color.text");
        setBackground(backgroundColour);

        String title = (String)getProperty("title");
        if(title != null)
            setTitle(title);

        JPanel mainPanel = (JPanel)getContentPane();
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        mainPanel.setBackground(backgroundColour);

        // insert logo
        Image img = getToolkit().createImage((byte[])getProperty("gif.logo"));
        JLabel logoLabel = new JLabel(new ImageIcon(img));
        mainPanel.add(logoLabel, BorderLayout.NORTH);

        // create the buttons (south)

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(backgroundColour);

        installButton = new JButton("Install");
        buttonPanel.add(installButton);
        installButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(this);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);


        // create the centre panel

        Box centrePanel = new Box(BoxLayout.Y_AXIS);

        Box dirPanel = new Box(BoxLayout.X_AXIS);
        dirPanel.add(Box.createHorizontalGlue());
        dirPanel.add(new JLabel("Directory to install to:"));
        directoryField = new JTextField(installationDir, 16);
        dirPanel.add(directoryField);
        browseDirButton = new JButton("Browse");
        browseDirButton.addActionListener(this);
        dirPanel.add(browseDirButton);
        centrePanel.add(dirPanel);

        centrePanel.add(Box.createVerticalStrut(5));

        Box jdkDirPanel = new Box(BoxLayout.X_AXIS);
        jdkDirPanel.add(Box.createHorizontalGlue());
        jdkDirPanel.add(new JLabel("Java (JDK) directory:"));
        javaField = new JTextField(javaPath, 16);
        jdkDirPanel.add(javaField);
        browseJdkButton = new JButton("Browse");
        browseJdkButton.addActionListener(this);
        jdkDirPanel.add(browseJdkButton);
        centrePanel.add(jdkDirPanel);

        centrePanel.add(Box.createVerticalStrut(5));

        Box jdkPanel = new Box(BoxLayout.X_AXIS);
        jdkPanel.add(new JLabel("JDK version:", JLabel.LEFT));
        jdkPanel.add(Box.createHorizontalStrut(20));
        jdk12Button = new JRadioButton("jdk 1.2", isJDK12);
        jdk13Button = new JRadioButton("jdk 1.3", !isJDK12);
        jdk12Button.setBackground(backgroundColour);
        jdk13Button.setBackground(backgroundColour);

        ButtonGroup bGroup = new ButtonGroup();
        {
            bGroup.add(jdk12Button);
            bGroup.add(jdk13Button);
        }

        jdkPanel.add(jdk12Button);
        jdkPanel.add(jdk13Button);
        jdkPanel.add(Box.createHorizontalGlue());
        centrePanel.add(jdkPanel);

        centrePanel.add(Box.createVerticalStrut(12));

        progress = new JProgressBar(); 
        centrePanel.add(progress);

        centrePanel.add(Box.createVerticalStrut(5));

        JPanel labelPanel = new JPanel(new GridLayout(0,1));
        labelPanel.setBackground(backgroundColour);
        textLabel1 = new JLabel(" ", JLabel.LEFT);
        labelPanel.add(textLabel1);
        textLabel2 = new JLabel(" ", JLabel.LEFT);
        labelPanel.add(textLabel2);

        centrePanel.add(labelPanel);

        String tagline = (String)getProperty("tagline");
        if(tagline != null)
            textLabel2.setText(tagline);


        mainPanel.add(centrePanel, BorderLayout.CENTER);

        getRootPane().setDefaultButton(installButton);

        pack();
        setLocation(100,100);
        setVisible(true);

        //Create a timer to update progress
        timer = new javax.swing.Timer(50, new ActionListener() {
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
     * Write out a Unix, Bourne shell script to start the application
     * For JDK 1.3 and later
     */
    public void writeUnix() throws IOException 
    {

        File outputFile = new File(installationDir, (String)getProperty("exeName"));
        FileWriter out = new FileWriter(outputFile.toString());
        out.write("#!/bin/sh\n");
        out.write("APPBASE=" + installationDir + "\n");
        String commands;
        commands = getProperty("unixCommands").toString();
        if(commands != null) {
            commands = replace(commands, '~', "$APPBASE");
            commands = replace(commands, '!', javaPath);
            commands = replace(commands, '@', architecture);
            out.write(commands);
            out.write("\n");
        }
        String classpath;
        classpath = getProperty("classpath").toString();
        classpath = classpath.replace(';', ':');
        classpath = replace(classpath, '~', "$APPBASE");
        classpath = replace(classpath, '!', javaPath);
        classpath = replace(classpath, '@', architecture);
        out.write("CLASSPATH=" + classpath + "\n");
        out.write("export CLASSPATH\n");
        out.write(javaPath + "/bin/java " + getProperty("javaOpts") +
                  " " + getProperty("mainClass") + " $*\n");
        out.close();
		
        try {
            Runtime.getRuntime().exec("chmod 755 " + outputFile);
        } catch(Exception e) {
            // ignore it - might not be Unix
        }
    }

    /**
     * Write out a Unix, Bourne shell script to start the application
     * For JDK 1.2.2
     */
    public void writeUnix12(boolean localJPDA) throws IOException 
    {

        File outputFile = new File(installationDir, (String)getProperty("exeName"));
        FileWriter out = new FileWriter(outputFile.toString());
        out.write("#!/bin/sh\n");
        out.write("APPBASE=" + installationDir + "\n");
        String commands;
        if (localJPDA)
            commands = getProperty("unixCommands.localJPDA").toString();
        else
            commands = getProperty("unixCommands.systemJPDA").toString();
        if(commands != null) {
            commands = replace(commands, '~', "$APPBASE");
            commands = replace(commands, '!', javaPath);
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
        classpath = replace(classpath, '!', javaPath);
        classpath = replace(classpath, '@', architecture);
        out.write("CLASSPATH=" + classpath + "\n");
        out.write("export CLASSPATH\n");
        out.write(javaPath + "/bin/java " + 
                  getProperty("javaOpts.1.2") + " " + 
                  getProperty("mainClass") + " $*\n");
        out.close();
		
        try {
            Runtime.getRuntime().exec("chmod 755 " + outputFile);
        } catch(Exception e) {
            // ignore it - might not be Unix
        }
    }

    /**
     * Write out a MacOS X, Bourne shell script to start the application
     */
    public void writeMacOS() throws IOException 
    {

        File outputFile = new File(installationDir, (String)getProperty("exeName"));
        FileWriter out = new FileWriter(outputFile.toString());
        out.write("#!/bin/sh\n");
        out.write("APPBASE=" + installationDir + "\n");
        String commands;
        commands = getProperty("unixCommands").toString();
        if(commands != null) {
            commands = replace(commands, '~', "$APPBASE");
            commands = replace(commands, '!', javaPath);
            commands = replace(commands, '@', architecture);
            out.write(commands);
            out.write("\n");
        }
        String classpath;
        classpath = getProperty("classpath.mac").toString();
        classpath = classpath.replace(';', ':');
        classpath = replace(classpath, '~', "$APPBASE");
        classpath = replace(classpath, '!', javaPath);
        classpath = replace(classpath, '@', architecture);
        out.write("CLASSPATH=" + classpath + "\n");
        out.write("export CLASSPATH\n");
        //out.write(javaPath + "/Commands/java " + getProperty("javaOpts") +
        out.write("java " + getProperty("javaOpts") + " " +
                  getProperty("mainClass") + " $*\n");
        out.close();
		
        try {
            Runtime.getRuntime().exec("chmod 755 " + outputFile);
        } catch(Exception e) {
            // ignore it - might not be Unix
        }
    }

    /**
     * Write out an MSDOS style batch file to start the application.
     * (JDK 1.3 and later)
     */
    public void writeWindows() throws IOException 
    {
        File outputFile = new File(installationDir,
                                   (String)getProperty("exeName") + ".bat");
			
        FileWriter out = new FileWriter(outputFile.toString());
        out.write("@echo off\r\n");
        out.write("set OLDPATH=%CLASSPATH%\r\n");
        out.write("set APPBASE=" + installationDir + "\r\n");
        String commands = getProperty("winCommands").toString();
        if(commands != null) {
            commands = replace(commands, '~', "%APPBASE%");
            commands = replace(commands, '!', javaPath);
            commands = replace(commands, '@', architecture);
            out.write(commands);
            out.write("\r\n");
        }
        String classpath = getProperty("classpath").toString();
        classpath = classpath.replace('/', '\\');
        classpath = replace(classpath, '~', "%APPBASE%");
        classpath = replace(classpath, '!', javaPath);
        classpath = replace(classpath, '@', architecture);
        out.write("set CLASSPATH=" + classpath + "\r\n");
        out.write("\"" + javaPath + "\\bin\\java\" " +
                  getProperty("javaOpts") + " " +
                  getProperty("mainClass") + 
                  " %1 %2 %3 %4 %5 %6 %7 %8 %9\r\n");
        out.write("set CLASSPATH=%OLDPATH%\r\n");

        out.close();
    }

    /**
     * Write out an MSDOS style batch file to start the application.
     */
    public void writeWindows12() throws IOException 
    {
        File outputFile = new File(installationDir,
                                   (String)getProperty("exeName") + ".bat");
			
        FileWriter out = new FileWriter(outputFile.toString());
        out.write("@echo off\r\n");
        out.write("set OLDPATH=%CLASSPATH%\r\n");
        out.write("set APPBASE=" + installationDir + "\r\n");
        String commands = getProperty("winCommands.12").toString();
        if(commands != null) {
            commands = replace(commands, '~', "%APPBASE%");
            commands = replace(commands, '!', javaPath);
            commands = replace(commands, '@', architecture);
            out.write(commands);
            out.write("\r\n");
        }
        String classpath = getProperty("classpath.localJPDA").toString();
        classpath = classpath.replace('/', '\\');
        classpath = replace(classpath, '~', "%APPBASE%");
        classpath = replace(classpath, '!', javaPath);
        classpath = replace(classpath, '@', architecture);
        out.write("set CLASSPATH=" + classpath + "\r\n");
        out.write("\"" + javaPath + "\\bin\\java\" " + 
                  getProperty("javaOpts.1.2") + " " + 
                  getProperty("mainClass") + 
                  " %1 %2 %3 %4 %5 %6 %7 %8 %9\r\n");
        out.write("set CLASSPATH=%OLDPATH%\r\n");

        out.close();
    }


    // ===========================================================
    // File I/O (JAR extraction)
    // ===========================================================


    /**
     * Grab the jar data from the class file and unjar it into the 
     * install directory.
     */
    public void unpackTo(boolean doJar) {
        try {
            InputStream cpin =
                ClassLoader.getSystemResourceAsStream("Installer.class");

			File tempInstallerClass = File.createTempFile("bluej", null);
			FileOutputStream cpout = new FileOutputStream(tempInstallerClass);
            byte[] buffer = new byte[8192];
            int len;
            while((len = cpin.read(buffer)) != -1)
                cpout.write(buffer, 0, len);
            cpin.close();
            cpout.close();
			
            RandomAccessFile in = new RandomAccessFile(tempInstallerClass, "r");
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
                dumpJar(installationDir, new FileInputStream(in.getFD()));
            }
            in.close();
            tempInstallerClass.deleteOnExit();
        } catch (Exception e) {
            notifyError("Installer failed to open: " + e,
                        "Could not open install file.");
        }
    }


    /**
     * Recursively make directories needed for a file.
     */
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
     * Extract a JAR from a file stream to the given directory on disk.
     */
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
     * Constructs a new string by replacing the character in
     * pattern found in src by the string subst
     */
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
	
    /**
     * Get a directory name via a file selection dialog.
     */
    public String getDirName(String title)
    {
        JFileChooser newChooser = new JFileChooser();

        newChooser.setDialogTitle(title);
        newChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = newChooser.showDialog(this, "Choose");

        if (result == JFileChooser.APPROVE_OPTION)
            return newChooser.getSelectedFile().getPath();
        else 
            return null;
    }

    /**
     * property access
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }


} // End class install

