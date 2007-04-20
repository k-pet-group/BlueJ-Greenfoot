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
    private String selectedFunction;

    public ExportDialog(Frame parent, String scenarioName, List<String> worlds, File defaultExportDir)
    {
        super(parent, dialogTitle, true);
        
        panes = new HashMap<String, ExportPane>();
        panes.put(ExportPublishPane.FUNCTION, new ExportPublishPane(worlds, scenarioName));
        panes.put(ExportWebPagePane.FUNCTION, new ExportWebPagePane(worlds, scenarioName, defaultExportDir));
        panes.put(ExportAppPane.FUNCTION, new ExportAppPane(worlds, scenarioName, defaultExportDir));
        
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
     * Return the identifier for the specific export function selected.
     */
    public String getSelectedFunction()
    {
        return selectedFunction;
    }

    /**
     * Return the identifier for the specific export function selected.
     */
    public ExportPane getSelectedPane()
    {
        return selectedPane;
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
    public void tabSelected(String function)
    {
        showPane(function);
    }

    // === end of TabbedIconListener interface ===

    /** 
     * Called when the selection of the tabs changes.
     */
    public void showPane(String function)
    {
        ExportPane chosenPane = panes.get(function);
        if(chosenPane != selectedPane) {
            if(selectedPane != null)
                contentPane.remove(selectedPane);
            contentPane.add(chosenPane, BorderLayout.CENTER);
            selectedPane = chosenPane;
            selectedFunction = function;
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
        
        showPane(ExportPublishPane.FUNCTION);

        DialogManager.centreDialog(this);
    }

    /**
     * Set the preferred width for all tabs to the widest of the tabs.
     */
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
