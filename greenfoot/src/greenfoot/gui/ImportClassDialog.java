package greenfoot.gui;

import greenfoot.gui.classbrowser.ClassButton;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * The dialog showing a library of supplied classes
 * (with associated image and javadoc), from whic
 * you can select one to import into the current project.
 *  
 * @author neil
 */
public class ImportClassDialog extends EscapeDialog
{
    // How much to indent each sub-folder in the class list 
    private static final int INDENT_HIERARCHY = 20;
    
    private JComponent classList;
    private JEditorPane htmlPane;
    private File curSelection;
    private File curSelectionImage;
    private File finalSelection;
    private File finalSelectionImage;
    private ButtonGroup buttonGroup;
    private JLabel classPicture;
    private JLabel classLabel;

    public ImportClassDialog(Frame parent)
    {
        super(parent, true);
        buttonGroup = new ButtonGroup();
        buildUI();
    }
    
    /**
     * Gets the final selected file (which may be .java or .class)
     * null if the user pressed cancel (or otherwise did not select one)
     */
    public File getFinalSelection()
    {
        return finalSelection;
    }
    
    /**
     * Gets the final selected file image (which may be .java or .class)
     * null if the user pressed cancel (or otherwise did not select one)
     */
    public File getFinalSelectionImageFile()
    {
        return finalSelectionImage;
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
            finalSelection = curSelection;
            finalSelectionImage = curSelectionImage;
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
               .replace("./resources/inherit.gif", new File(Config.getGreenfootLibDir(), "inherit.gif").toURI().toURL().toString())
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
            classPicture.setIcon(img == null ? null : new ImageIcon(img.getAbsolutePath()));
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

}