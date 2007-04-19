package greenfoot.gui.export;

import greenfoot.gui.*;
import greenfoot.util.FileChoosers;
import java.awt.BorderLayout;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import bluej.BlueJTheme;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * Dialog for getting the settings to do an export of a greenfoot project.
 * 
 * @author Poul Henriksen
 *
 */
public class ExportDialog extends EscapeDialog
{
    // Internationalisation
    private static final String dialogTitle ="Greenfoot: Export";
    private static final String helpLine1 = "Create a stand alone version of this scenario that can be run outside of Greenfoot.";
    private static final String worldSelectLabelText = "World Class: ";

    private static final String exportLcoationLabelText = "Export location: ";
    private static final String extraControlsLabelText = "Allow speed change and 'Act'";
    private String mainClassName = "";

    private JCheckBox extraControls;
    
    private boolean ok;
    private String worldClass;
    private JComboBox worldSelect;
    private JTextField exportField;
    private JFileChooser fileChooser;

    public ExportDialog(Frame parent, List<String> worlds, File defaultExportDir)
    {
        super(parent, dialogTitle, true);
        exportField = new JTextField(defaultExportDir.toString(), 20);
        exportField.setEditable(false);
        makeDialog(worlds);
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
        ok = false;
        setVisible(true);  // returns after OK or Cancel, which set 'ok'
        return ok;
    }

    /**
     * Return the directory where the scenario should be exported.
     */
    public File getExportLocation()
    {
        return new File(exportField.getText());
    }

    /**
     * Return the name of the world class that should be instantiated.
     */
    public String getWorldClass()
    {
        return worldClass;
    }

  
    /**
     * Return true if user wants to include the source.
     */
    public boolean includeExtraControls()
    {
        return extraControls.isSelected();
    }
    
    
    /**
     * Close action when OK is pressed.
     */
    private void doOK()
    {
        worldClass = (String) worldSelect.getSelectedItem();
       
        ok = true;
        
        //TODO check if selection is valid? Or only allow selection via browse button
        setVisible(false);
    }

    /**
     * Close action when Cancel is pressed.
     */
    private void doCancel()
    {
        ok = false;
        setVisible(false);
    }

    /**
     * Create the dialog interface.
     * @param defaultExportDir The default place to export to.
     * @param worlds List of possible worlds that can be instantiated.
     */
    private void makeDialog(List<String> worlds)
    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        JPanel contentPane = (JPanel) getContentPane();
        
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(null);
        
        JPanel togglePane = new TabbedIconPane();
        contentPane.add(togglePane, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            JLabel helpText1 = new JLabel(helpLine1);
            mainPanel.add(helpText1);


            Font smallFont = helpText1.getFont().deriveFont(Font.ITALIC, 11.0f);
            helpText1.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            mainPanel.add(Box.createVerticalStrut(5));

            JPanel inputPanel = new JPanel();
            {
                inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
                inputPanel.setAlignmentX(LEFT_ALIGNMENT);
                
                if (worlds.size() > 1) {  // only if there is more than one world
                    JPanel mainClassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    {
                        worldSelect = makeWorldClassPopup(worlds);
                        JLabel classLabel = new JLabel(worldSelectLabelText);
                        mainClassPanel.add(classLabel);
                        mainClassPanel.add(worldSelect);                        
                    }
                    mainClassPanel.setAlignmentX(LEFT_ALIGNMENT);
                    inputPanel.add(mainClassPanel);
                }
                inputPanel.add(Box.createVerticalStrut(5));
                
                JPanel exportLocationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                {
                    JLabel exportLocationLabel = new JLabel(exportLcoationLabelText);
                    exportLocationPanel.add(exportLocationLabel);

                    exportLocationPanel.add(exportField);

                    JButton browse = new JButton("Browse");
                    exportLocationPanel.add(browse);
                    browse.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e)
                        {
                            File file = FileChoosers.getExportFile(ExportDialog.this);
                            if(file != null) {
                                exportField.setText(file.getPath());
                            }
                        }
                    });                    
                }
                exportLocationPanel.setAlignmentX(LEFT_ALIGNMENT);
                inputPanel.add(exportLocationPanel);
                inputPanel.add(Box.createVerticalStrut(5));
                
                extraControls = new JCheckBox(extraControlsLabelText, false);
                extraControls.setSelected(true);
                extraControls.setAlignmentX(LEFT_ALIGNMENT);
                inputPanel.add(extraControls);
                inputPanel.add(Box.createVerticalStrut(5));
            }

            mainPanel.add(inputPanel);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

        }
        
        contentPane.add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        {
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
            buttonPanel.setBorder(BlueJTheme.dialogBorder);

            JButton continueButton = new JButton("Export");
            continueButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) { doOK(); }                
            });

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) { doCancel(); }                
            });

            buttonPanel.add(continueButton);
            buttonPanel.add(cancelButton);

            getRootPane().setDefaultButton(continueButton);
        }

        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        
        pack();

        DialogManager.centreDialog(this);
    }

    

    
    /**
     * Fill the world class popup selector with all the worlds in the list.
     */
    private JComboBox makeWorldClassPopup(List<String> worlds)
    {
        JComboBox popup = new JComboBox();

        popup.setFont(PrefMgr.getPopupMenuFont());

        for (Iterator classes = worlds.iterator(); classes.hasNext();)
            popup.addItem(classes.next());
        
        return popup;
    }
    

}
