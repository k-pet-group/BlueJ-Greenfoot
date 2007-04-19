package greenfoot.gui.export;

import java.awt.BorderLayout;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import bluej.BlueJTheme;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

public class ExportDialog extends EscapeDialog
{
    // Internationalisation
    private static final String dialogTitle ="Greenfoot: Export";

    private boolean ok;
    private ExportPane[] panes;
    private int selectedPane;

    public ExportDialog(Frame parent, List<String> worlds, File defaultExportDir)
    {
        super(parent, dialogTitle, true);
        
        panes = new ExportPane[3];
        panes[0] = new ExportPublishPane(worlds);
        panes[1] = new ExportWebPagePane(worlds, defaultExportDir);
        panes[2] = new ExportAppPane(worlds, defaultExportDir);

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
        return new File(((ExportWebPagePane)panes[selectedPane]).getExportLocation());
    }

    /**
     * Return the name of the world class that should be instantiated.
     */
    public String getWorldClass()
    {
        return panes[selectedPane].getWorldClassName();
    }

  
    /**
     * Return true if user wants to include the source.
     */
    public boolean includeExtraControls()
    {
        return panes[selectedPane].includeExtraControls();
    }
    
    
    /**
     * Close action when OK is pressed.
     */
    private void doOK()
    {
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

        contentPane.add(panes[1], BorderLayout.CENTER);
        selectedPane = 1;

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

}
