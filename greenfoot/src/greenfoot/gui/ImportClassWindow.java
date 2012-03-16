/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.gui;

import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassButton;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.record.InteractionListener;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * The window showing a library of supplied classes
 * (with associated image and javadoc), from which
 * you can select one to import into the current project.
 *  
 * @author neil
 */
public class ImportClassWindow extends JFrame
{
    // How much to indent each sub-folder in the class list 
    private static final int INDENT_HIERARCHY = 20;
    
    private JComponent classList;
    private JEditorPane htmlPane;
    private File curSelection;
    private File curSelectionImage;
    private ButtonGroup buttonGroup;
    private JLabel classPicture;
    private JLabel classLabel;
    private GreenfootFrame gfFrame;
    private InteractionListener interactionListener;

    public ImportClassWindow(GreenfootFrame gfFrame, InteractionListener interactionListener)
    {
        this.gfFrame = gfFrame;
        this.interactionListener = interactionListener;
        buttonGroup = new ButtonGroup();
        buildUI();
    }
    
    /**
     * The ok action for this dialog, which sets the final selection
     */
    private class OkAction extends AbstractAction
    {
        public OkAction()
        {
            super(Config.getString("import.import"));
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            setVisible(false);
            importClass(curSelection, curSelectionImage);
        }
    }
    
    private void buildUI()
    {
        setTitle(Config.getString("import.dialogTitle"));
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout(10, 0));
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.add(p);
        main.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        setContentPane(main);
        
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
              setVisible(false);
            }
        };
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        main.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        {
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

            JButton okButton = new JButton(new OkAction());
            getRootPane().setDefaultButton(okButton);

            JButton cancelButton = new JButton(new AbstractAction(Config.getString("greenfoot.cancel")) {
                public void actionPerformed(ActionEvent e)
                {
                    setVisible(false);                                                
                }
            });

            DialogManager.addOKCancelButtons(buttonPanel, okButton, cancelButton);
        }
        main.add(buttonPanel);

        classList = new JPanel();
        classList.setLayout(new BoxLayout(classList, BoxLayout.Y_AXIS));
        classList.setBackground(java.awt.Color.WHITE);
        classList.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        findAddImportableClasses(new File(Config.getGreenfootLibDir(), "common"), 0);
                    
        JScrollPane classScrollPane = new JScrollPane(classList) {
            public Dimension getPreferredSize()
            {
                Dimension size = super.getPreferredSize();
                // Always leave room for the vertical scroll bar to appear
                // This stops a horizontal scroll bar getting added when the vertical one appears
                size.width += getVerticalScrollBar().getWidth();
                return size;
            }
            
            public Dimension getMaximumSize()
            {
                return getPreferredSize();
            }
            
        };
        //setScrollIncrements(classScrollPane);
        classScrollPane.setOpaque(false);
        classScrollPane.getViewport().setOpaque(false);
        classScrollPane.setBorder(BorderFactory.createEtchedBorder());
        classScrollPane.setSize(classScrollPane.getPreferredSize());
        p.add(classScrollPane, BorderLayout.WEST);
                   
        
        htmlPane = new JEditorPane();
        htmlPane.setDocument(new HTMLDocument());
        htmlPane.setEditorKit(new HTMLEditorKit());
        htmlPane.setEditable(false);
        
        JPanel rightPane = new JPanel();
        rightPane.setLayout(new BorderLayout());
        JPanel classInfo = new JPanel();
        classInfo.setBorder(new EmptyBorder(10, 5, 10, 5));
        classInfo.setLayout(new GridLayout(1, 2));
        classLabel = new JLabel("", JLabel.CENTER);
        classLabel.setFont(classLabel.getFont().deriveFont(24.0f));
        classInfo.add(classLabel);
        classPicture = new JLabel((String)null, JLabel.CENTER);
        classInfo.add(classPicture);
        rightPane.add(classInfo, BorderLayout.NORTH);
        rightPane.add(new JScrollPane(htmlPane), BorderLayout.CENTER);
        p.add(rightPane, BorderLayout.CENTER);

        pack();
        setSize(700, 550);
        ((ImportableClassButton)buttonGroup.getElements().nextElement()).select();
        ((ImportableClassButton)buttonGroup.getElements().nextElement()).setSelected(true);
        
        setLocation(gfFrame.getX() + 40, gfFrame.getY() + 40);
    }
   
    /**
     * Searches for the HTML file associated with the given class-name (aka file stem)
     * 
     * If found, shows it in htmlPane.
     */
    private void showHTML(String stem)
    {
        if (stem == null) {
            htmlPane.setText("");
            return;
        }
        
        File htmlFile = new File(stem + ".html");
        
        if (!htmlFile.exists()) {
            Debug.message("No HTML file found for class " + stem + "; looked for: " + htmlFile.getAbsolutePath());
            htmlPane.setText("");
            return;
        }
        
        // We process the contents to replace references to ./resources/inherit.gif
        // to our own copy, to avoid having to make lots of resources sub-directories in the
        // common classes directory.
        try {
            BufferedReader br = new BufferedReader(new FileReader(htmlFile));
            char[] buffer = new char[1024];
            StringBuilder s = new StringBuilder();
            int n;
            do
            {
                n = br.read(buffer);
                if (n != -1)
                    s.append(buffer, 0, n);
            }
            while (n != -1);
            
            String processedContents = s.toString()
               .replace("./resources/inherit.gif", new File(Config.getGreenfootLibDir(), "common/inherit.gif").toURI().toURL().toString())
               //And, while I'm at it, fix that damn missing space:
               .replace("</B><DT>extends", "</B><DT> extends")
               ;
            
            
            htmlPane.setText(processedContents);
            htmlPane.setCaretPosition(0);
            
            //((HTMLDocument)htmlPane.getDocument()).setBase(new URL(fullURL));
        }
        catch (IOException e) {
            Debug.reportError("Problem showing HTML for importable class " + stem, e);
            htmlPane.setText("");
        }
    }
    
    private class ImportableClassButton extends ClassButton
    {
        private File file;
        private String name;
        
        public ImportableClassButton(File file)
        {
            this.file = file;
            name = GreenfootUtil.removeExtension(file.getName());
            setText(name);
            initUI();
        }
        
        protected boolean isValidClass()
        {
            return true;
        }

        protected boolean isUncompiled()
        {
            return false;
        }

        @Override
        protected void doubleClick()
        {
        }

        @Override
        public void select()
        {
            curSelection = file;
            classLabel.setText(name);
            File img = findImage(file);
            curSelectionImage = img;
            ImageIcon icon;
            if (img == null)
            {
                icon = null;
            }
            else
            {
                icon = new ImageIcon(img.getAbsolutePath());
                final int maxDim = 60;
                if (Math.max(icon.getIconHeight(), icon.getIconWidth()) > maxDim)
                {
                    double scale = (double)maxDim / (double)Math.max(icon.getIconHeight(), icon.getIconWidth());
                    icon.setImage(icon.getImage().getScaledInstance((int)(scale * icon.getIconWidth()), (int)(scale * icon.getIconHeight()), Image.SCALE_SMOOTH));
                }
            }
            classPicture.setIcon(icon);
            showHTML(GreenfootUtil.removeExtension(file.getAbsolutePath()));
        }

        @Override
        public boolean deselect()
        {
            curSelection = null;
            curSelectionImage = null;
            classLabel.setText("");
            classPicture.setIcon(null);
            showHTML(null);
            return false;
        }

        @Override
        protected void maybeShowPopup(MouseEvent e)
        {          
        }
    }
    
    //File dir = new File(Config.getGreenfootLibDir(), "common");
    private void findAddImportableClasses(File dir, int indent)
    {
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname)
            {
                return pathname.getAbsolutePath().endsWith(".class") || pathname.getAbsolutePath().endsWith(".java") || pathname.isDirectory();
            }
        });
        
        if (files == null) //Problem finding classes
            return;

        // List all files before all directories:
        
        boolean hasAnyFiles = false;
        for (File file : files) {
            if (file.isFile()) {
                ImportableClassButton button = new ImportableClassButton(file);
                addWithIndent(indent, button);
                buttonGroup.add(button);
                hasAnyFiles = true;
            }
        }
        
        // Only indent if there is a class in the current category to be distinguished from:
        if (hasAnyFiles)
            indent += INDENT_HIERARCHY;
        
        for (File file : files) {
            if (file.isDirectory()) {
                JLabel label = new JLabel(file.getName());
                label.setFont(label.getFont().deriveFont(16.0f));
                addWithIndent(indent, label);
                // Recurse to process sub-directories:
                findAddImportableClasses(file, indent);
            }
        }
    }
    
    /**
     * Adds the given component to classList, with the given horizontal indent
     */
    private void addWithIndent(int indent, JComponent comp)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalStrut(indent));
        panel.add(comp);
        panel.add(Box.createHorizontalGlue());
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(panel.getMaximumSize().width, panel.getPreferredSize().height));
        classList.add(panel);
    }
    
    /**
     * Looks for an image that might be associated with the given class.
     * 
     * So given /foo/Crab.java or /foo/Crab.class, it looks (case insensitive) for /foo/crab.png, /foo/Crab.jpg, etc
     */
    private static File findImage(File classFile)
    {
        String[] extensions = ImageIO.getReaderFileSuffixes();
        
        File directory = classFile.getAbsoluteFile().getParentFile();
        String stemName = GreenfootUtil.removeExtension(classFile.getAbsoluteFile().getName());
        
        File[] allFiles = directory.listFiles();
        
        if (allFiles == null)
            return null;

        for (File f : allFiles) {
            for (String ext : extensions) {
                if (f.getName().equalsIgnoreCase(stemName + "." + ext)) {
                    return f;
                }
            }
        }
                
        return null;
    }
    
    private void importClass(File srcFile, File srcImage)
    {
        if (srcFile != null) {
            String className = GreenfootUtil.removeExtension(srcFile.getName());
            
            ClassBrowser classBrowser = gfFrame.getClassBrowser();
            GProject project = classBrowser.getProject();
            
            // Check if a class of the same name already exists in the project.
            // Renaming would be too tricky, so just issue error and stop in that case:
            for (GClass preexist : project.getDefaultPackage().getClasses(false)) {
                if (preexist.getQualifiedName().equals(className)) {
                    JOptionPane.showMessageDialog(gfFrame, "The current project already contains a class named " + className);
                    return;
                }
            }
            File destImage = null;
            if (srcImage != null) {
                destImage = new File(project.getImageDir(), srcImage.getName());
                if (destImage.exists()) {
                    JOptionPane.showMessageDialog(gfFrame, "The current project already contains an image file named " + srcImage.getName() + "; this file will NOT be replaced.");
                }
            }
            
            // Copy the java/class file cross:
            File destFile = new File(project.getDir(), srcFile.getName());
            GreenfootUtil.copyFile(srcFile, destFile);
            
            // We must reload the package to be able to access the GClass object:
            project.getDefaultPackage().reload();
            GClass gclass = project.getDefaultPackage().getClass(className);
            
            if (gclass == null) {
                //TODO give an error
                return;
            }
            
            // Copy the image across and set it as the class image:
            if (srcImage != null && destImage != null && !destImage.exists()) {
                GreenfootUtil.copyFile(srcImage, destImage);
                gclass.setClassProperty("image", destImage.getName());
            }
            
            //Finally, update the class browser:
            classBrowser.addClass(new ClassView(classBrowser, gclass, interactionListener));
            classBrowser.updateLayout();
        }
    }

}