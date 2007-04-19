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
import java.awt.Dimension;
import java.util.HashMap;

public class ExportDialog extends EscapeDialog
        implements TabbedIconPaneListener
{
    // Internationalisation
    private static final String dialogTitle ="Greenfoot: Export";

    private JPanel contentPane;
    private boolean ok;
    private HashMap<String, ExportPane> panes;
    private ExportPane selectedPane;

    public ExportDialog(Frame parent, List<String> worlds, File defaultExportDir)
    {
        super(parent, dialogTitle, true);
        
        panes = new HashMap<String, ExportPane>();
        panes.put(ExportPublishPane.NAME, new ExportPublishPane(worlds));
        panes.put(ExportWebPagePane.NAME, new ExportWebPagePane(worlds, defaultExportDir));
        panes.put(ExportAppPane.NAME, new ExportAppPane(worlds, defaultExportDir));
        
        fixSizes(panes);

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
        return new File(((ExportWebPagePane)selectedPane).getExportLocation());
    }

    /**
     * Return the name of the world class that should be instantiated.
     */
    public String getWorldClass()
    {
        return selectedPane.getWorldClassName();
    }

  
    /**
     * Return true if user wants to include the source.
     */
    public boolean includeExtraControls()
    {
        return selectedPane.includeExtraControls();
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

    // === TabbedIconPaneListener interface ===
    
    /** 
     * Called when the selection of the tabs changes.
     */
    public void tabSelected(String name)
    {
        showPane(name);
    }

    // === end of TabbedIconListener interface ===

    /** 
     * Called when the selection of the tabs changes.
     */
    public void showPane(String name)
    {
        ExportPane chosenPane = panes.get(name);
        if(chosenPane != selectedPane) {
            if(selectedPane != null)
                contentPane.remove(selectedPane);
            contentPane.add(chosenPane, BorderLayout.CENTER);
            selectedPane = chosenPane;
        }
        pack();
    }
    
    /**
     * Create the dialog interface.
     * @param defaultExportDir The default place to export to.
     * @param worlds List of possible worlds that can be instantiated.
     */
    private void makeDialog(List<String> worlds)
    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        contentPane = (JPanel) getContentPane();
        
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(null);
        
        TabbedIconPane tabbedPane = new TabbedIconPane();
        tabbedPane.setListener(this);
        contentPane.add(tabbedPane, BorderLayout.NORTH);

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
        
        showPane(ExportPublishPane.NAME);

        DialogManager.centreDialog(this);
    }

    private void fixSizes(HashMap<String, ExportPane> panes) 
    {
        int maxWidth = 0;
        
        for(ExportPane pane : panes.values()) {
            Dimension size = pane.getPreferredSize();
            maxWidth = Math.max(size.width, maxWidth);
        }
        
        for(ExportPane pane : panes.values()) {
            Dimension size = pane.getPreferredSize();
            size.width = maxWidth;
            pane.setPreferredSize(size);
        }
    }
}
