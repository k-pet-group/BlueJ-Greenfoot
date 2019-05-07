// NO PACKAGE.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.swing.*;
import javax.swing.text.Keymap;

/**
  * Java installer. This is a GUI for unpacking an application (BlueJ or
  * Greenfoot) bundled inside a jar file. A properties file also bundled
  * in the jar file controls the installation.
  *
  * @author  Michael Kolling
  * @author  based partly on code by Andrew Hunt, Toolshed Technologies Inc.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  * 
  * Modified by Davin McCall, 2005-09-06, and 2010-09-24
  */
public class Installer extends JFrame
    implements ActionListener
{
    private static final String nl = System.getProperty("line.separator");
    private static final char slash = File.separatorChar;
    private static final String colon = File.pathSeparator;


    // File to test for JDK (relative to javaPath):
    static private String jdkFile = "/jmods/jdk.compiler.jmod";

    static private String javaFxFile = "/lib/javafx.graphics.jar";

    static final int BUFFER_SIZE=8192;

    // user interface components
    JTextField directoryField;
    JTextField javaField;
    JTextField javaFxField;
    JLabel textLabel1;
    JLabel textLabel2;
    JButton browseDirButton;
    JButton browseJdkButton;
    JButton browseJavaFxButton;
    //JRadioButton jdk12Button;
    //JRadioButton jdk13Button;
    JButton installButton;
    JButton cancelButton;
    JProgressBar progress;

    UpdateProgress progressUpdater;

    String currentDirectory;    // the user's working dir
    String osname;              // "SunOS", "Windows*", "Linux*", etc
    String architecture;        // "sparc", "i386", etc
    String javaVersion;
    boolean isJDK12;
    boolean isJDK13;

    String installationDir = "";
    String javaPath = "";
    String javaFxPath = "";

    Properties properties;
    long myTotalBytes = 400000;


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
        // Run the installer on the AWT event dispatch thread.
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    new Installer();
                }
            });
        }
        catch (InterruptedException ie) { ie.printStackTrace(); }
        catch (InvocationTargetException ite) { ite.printStackTrace(); }
    }

    // ===========================================================
    // Install time
    // ===========================================================

    public Installer()
    {
        super();
        currentDirectory = System.getProperty("user.dir");
        
        osname = System.getProperty("os.name");
        architecture = System.getProperty("os.arch");
        javaVersion = System.getProperty("java.specification.version");
        javaPath = findJavaPath();

        //System.out.println(javaPath);
        //System.out.println(osname);
        //System.out.println(javaVersion);
        //System.out.println(architecture);
        loadProperties();

        String installDirName = getProperty("installDirName");
        if(currentDirectory.endsWith(installDirName))
            installationDir = currentDirectory;
        else
            installationDir = currentDirectory + File.separator + installDirName;
        
        try {
            myTotalBytes = Integer.parseInt(getProperty("pkgJarSize"));
        }
        catch (NumberFormatException nfe) {}
        
        progressUpdater = new UpdateProgress();
        makeWindow();

        String requiredJavaVersion = getProperty("requiredJavaVersion");
        if(javaVersion.compareTo(requiredJavaVersion) < 0) {
            notifyError(getProperty("jdkError1") + javaVersion + " " + getProperty("jdkError2"), getProperty("jdkMsg"));
        }
    }

    /**
     * Load installer properties - name of the archive to extract, the logo, color scheme,
     * etc.
     */
    private void loadProperties()
    {
        InputStream propStream = ClassLoader.getSystemResourceAsStream("installer.props");
        properties = new Properties();
        try {
            properties.load(propStream);
        }
        catch (IOException ioe) {
            System.err.println("Error loading installer configuration:");
            ioe.printStackTrace(System.err);
            System.exit(1);
        }
    }
    
    /**
     * Try and figure out the path to the currently-running JDK
     * @return  The path, or an empty string if could not be determined
     */
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
        else if (src == browseJavaFxButton) {
            getJavaFxDirectory();
        }
        else if(src == installButton) {
            installButton.setEnabled(false);
            InstallThread p = new InstallThread();
            p.setPriority(Thread.MIN_PRIORITY + 1);
            p.start();
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
            if(dirName.endsWith(getProperty("installDirName")))
                installationDir = dirName;
            else
                installationDir = dirName + File.separator + getProperty("installDirName");
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
     * Get the JavaFX directory from the user via a file selection
     * dialogue.
     */
    private void getJavaFxDirectory()
    {
        String dirName = getDirName("Select JavaFX directory");
        if(dirName != null) {
            javaFxPath = dirName;
            javaFxField.setText(javaFxPath);
            if (!isJavaFxPath(javaFxPath))
                javaFxPathProblem();
        }
    }

    
    
    /**
     * Install button action. This gets executed on a seperate thread.
     */
    public void doInstall()
    {
        readInputValues();
        if(! isJDKPath(javaPath)) {
            jdkPathProblem();
            return;
        }
        if (!isJavaFxPath(javaFxPath))
        {
            javaFxPathProblem();
            return;
        }

        try {
            if(!checkInstallDir(installationDir, true))
                return;

            unpackTo();

            // Write the scripts, if this is an application
            if (getProperty("exeName") != null) {

                if(osname == null) {    // if we don't know, write both
                    writeWindows();
                    writeUnix(false);
                }
                else if(osname.startsWith("Windows")) {
                    writeWindows();
                }
                else if(osname.startsWith("Mac")) {
                    writeUnix(true);
                }
                else
                    writeUnix(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            finish("Installation FAILED: ", e.getMessage());
            return;
        }

        if (getProperty("exeName") != null) {
            finish(getProperty("appName") + " has been installed to " + installationDir,
                   "To run it, execute \"" +
                   getProperty("exeName") + "\".");
        } else {
            finish("The package has been installed to "+installationDir, " ");
        }

    }

    /**
     * Read the values that the user selected into the appropriate variables.
     */
    private void readInputValues()
    {
        installationDir = directoryField.getText();
        javaPath = javaField.getText();
        javaFxPath = javaFxField.getText();
        //isJDK12 = jdk12Button.isSelected();
    }

    /**
     * Check that the current Java version is a full JDK. Warn if not.
     */
    public boolean isJDKPath(String path)
    {
        String jdkFilePath = path + jdkFile;
        if(new File(jdkFilePath).exists())
            return true;
        else {
            // room here for additional checks should jdk structure change
            return false;
        }
    }

    /**
     * Check that the JavaFX path has JavaFX in it
     */
    public boolean isJavaFxPath(String path)
    {
        String javaFxFilePath = path + javaFxFile;
        return new File(javaFxFilePath).exists();
    }

    /**
     * Update the status dialog. Called on the installer thread.
     */
    public void setStatus(final String text)
    {
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               textLabel1.setText(text);
               textLabel1.repaint();
           }
        });
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
           "JDK directory. It must contain the file \n" +
           jdkFile);
    }

    /**
     * Inform user of invalid jdk.
     */
    private void javaFxPathProblem()
    {
        notifyProblem(
            "JavaFX must be installed, via package manager or downloaded\n" +
            "from openjfx.io  The JavaFX directory you have specified\n" + 
            "is not a valid JavaFX directory. It must contain the file\n" +
            javaFxFile);
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
    private void notifyProblem(final String problem) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(Installer.this, problem);
                installButton.setEnabled(true);
            }
        });
    }


    /**
     * Create and show the main window
     */
    public void makeWindow()
    {
        Color backgroundColour = colorFromString(getProperty("color.background"));
        setBackground(backgroundColour);

        String title = getProperty("title");
        if(title != null)
            setTitle(title);

        JPanel mainPanel = (JPanel)getContentPane();
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        mainPanel.setBackground(backgroundColour);

        // insert logo
        URL logoUrl = ClassLoader.getSystemResource(getProperty("gif.logo"));
        Image img = getToolkit().createImage(logoUrl);
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

        Box javaFxDirPanel = new Box(BoxLayout.X_AXIS);
        javaFxDirPanel.add(Box.createHorizontalGlue());
        javaFxDirPanel.add(new JLabel("JavaFX directory:"));
        javaFxField = new JTextField("", 16);
        javaFxDirPanel.add(javaFxField);
        browseJavaFxButton = new JButton("Browse");
        browseJavaFxButton.addActionListener(this);
        javaFxDirPanel.add(browseJavaFxButton);
        centrePanel.add(javaFxDirPanel);

        // jdk selection radio buttons - currently not used

//         centrePanel.add(Box.createVerticalStrut(5));

//         Box jdkPanel = new Box(BoxLayout.X_AXIS);
//         jdkPanel.add(new JLabel("JDK version:", JLabel.LEFT));
//         jdkPanel.add(Box.createHorizontalStrut(20));
//         jdk12Button = new JRadioButton("jdk 1.2", isJDK12);
//         jdk13Button = new JRadioButton("jdk 1.3", !isJDK12);
//         jdk12Button.setBackground(backgroundColour);
//         jdk13Button.setBackground(backgroundColour);

//         ButtonGroup bGroup = new ButtonGroup();
//         {
//             bGroup.add(jdk12Button);
//             bGroup.add(jdk13Button);
//         }

//         jdkPanel.add(jdk12Button);
//         jdkPanel.add(jdk13Button);
//         jdkPanel.add(Box.createHorizontalGlue());
//         centrePanel.add(jdkPanel);

        centrePanel.add(Box.createVerticalStrut(24));

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

        String tagline = getProperty("tagline");
        if(tagline != null)
            textLabel2.setText(tagline);


        mainPanel.add(centrePanel, BorderLayout.CENTER);

        getRootPane().setDefaultButton(installButton);

        pack();
        setLocation(100,100);
        setVisible(true);

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
     * For JDK 1.4 and later
     */
    public void writeUnix(boolean isMacOS)
        throws IOException
    {
        File outputFile = new File(installationDir, getProperty("exeName"));
        FileWriter out = new FileWriter(outputFile.toString());
        out.write("#!/bin/sh\n");
        out.write("APPBASE=\"" + installationDir + "\"\n");
        out.write("JAVAPATH=\"" + javaPath + "\"\n");
        out.write("JAVAFXPATH=\"" + javaFxPath + "\"\n");
        String commands;
        String javaName = "$JAVAPATH/bin/java";
        if(isMacOS) {
            commands = getProperty("commands.mac").toString();
        }
        else {
            commands = getProperty("commands.unix").toString();
        }
        commands += "\n" + getProperty("javafx.classpath.unix").toString();

        if(commands != null) {
            out.write(commands);
            out.write("\n");
        }
        out.write("\"" + javaName + "\" " + getProperty("javaOpts.unix") + " " +
                  getProperty("mainClass") + " " +
                  getProperty("arguments.unix") + " \"$@\"\n");
        out.close();

        try {
            String cmdArray[] = { "chmod", "755", outputFile.toString() };
            Runtime.getRuntime().exec(cmdArray);
        } catch(Exception e) {
            // ignore it - might not be Unix
        }
    }

    /**
     * Write out an MSDOS style batch file to start the application.
     * (JDK 1.4 and later)
     */
    public void writeWindows() throws IOException
    {
        File outputFile = new File(installationDir,
                                   getProperty("exeName") + ".bat");

        FileWriter out = new FileWriter(outputFile.toString());
        out.write("@echo off\r\n");
        out.write("set APPBASE=\"" + installationDir + "\"\r\n");
        out.write("set JAVAFXPATH=\"" + javaFxPath + "\"\r\n");
        String commands = getProperty("commands.win").toString();
        if(commands != null) {
            commands = replace(commands, '~', "%APPBASE%");
            commands = replace(commands, '!', "\"" + javaPath + "\"");
            commands = replace(commands, '@', architecture);
            out.write(commands);
            out.write("\r\n");
        }
        out.write(getProperty("javafx.classpath.win").toString() + "\r\n");
        out.write("\"" + javaPath + "\\bin\\java\" " +
                  getProperty("javaOpts.win") + " " +
                  getProperty("mainClass") + " " +
                  getProperty("arguments.win") +
                  " %1 %2 %3 %4 %5 %6 %7 %8 %9\r\n");
        out.close();
    }

    // ===========================================================
    // File I/O (JAR extraction)
    // ===========================================================


    /**
     * Grab the jar data from the class file and unjar it into the
     * install directory.
     */
    public void unpackTo() {
        try {
            InputStream in = ClassLoader.getSystemResourceAsStream(getProperty("pkgJar"));
            dumpJar(installationDir, new ProgressTrackerStream(in));
        }
        catch (Exception e) {
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
    public void dumpJar(String dir, InputStream in)
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
            out.close();

            if (z.getTime() != -1) {
                File f = new File(name);
                f.setLastModified(z.getTime());
            }

            zip.closeEntry();
        }

        zip.close();
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
    public String getProperty(String key) {
        return (String) properties.get("install." + key);
    }

    private Color colorFromString(String value)
    {
        // It's a color
        StringTokenizer tok = new StringTokenizer(value," \t,",false);
        int r = (Integer.valueOf(tok.nextToken())).intValue();
        int g = (Integer.valueOf(tok.nextToken())).intValue();
        int b = (Integer.valueOf(tok.nextToken())).intValue();
        return new Color(r,g,b);
    }

    /*
     * Progress tracking
     */
    
    /**
     * Update the progress bar with a percentage value. This is called from
     * the installation thread.
     */
    private void updateProgress(int value)
    {
        progressUpdater.value = value;
        try {
            EventQueue.invokeAndWait(progressUpdater);
        }
        catch (Exception e) { e.printStackTrace(); }
    }
    
    /**
     * A stream which keeps track of progress through the underlying stream,
     * updating the progress bar at appropriate occassions.
     * 
     * @author Davin McCall
     */
    private class ProgressTrackerStream extends InputStream
    {
        InputStream underlying;
        long readCount = 0;
        long markerIncrement = (myTotalBytes / 100);
        long nextMarker = (myTotalBytes / 100);
        
        public ProgressTrackerStream(InputStream over)
        {
            underlying = over;
        }
        
        public int read() throws IOException
        {
            int r = underlying.read();
            if (r != -1)
                readCount++;
            
            if (readCount > nextMarker) {
                updateProgress((int) (readCount / markerIncrement));
                nextMarker = nextMarker + markerIncrement;
            }
            
            return r;
        }
        
        public int read(byte[] b, int off, int len) throws IOException
        {
            int r = underlying.read(b, off, len);
            readCount += r;

            if (readCount > nextMarker) {
                updateProgress((int) (readCount / markerIncrement));
                nextMarker = nextMarker + markerIncrement;
            }
            
            return r;
        }
    }
    
    /**
     * A runnable used to update the progress bar.
     * 
     * @author Davin McCall
     */
    private class UpdateProgress implements Runnable
    {
        int value;
        public void run()
        {
            progress.setValue(value);
        }
    }
    
} // End class install

