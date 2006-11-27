package bluej.groupwork.ui;

import bluej.BlueJTheme;
import bluej.Config;

import bluej.pkgmgr.Project;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.ScrollPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;


/**
 * @author fisker
 *
 */
public class ConflictsDialog extends JDialog
{
    private JLabel heading;
    private List bluejConflicts;
    private List nonBluejConflicts;
    private Project project;

    /**
     * @param project2
     * @param blueJconflicts
     * @param nonBlueJConflicts
     */
    public ConflictsDialog(Project project, List bluejConflicts,
        List nonBlueJConflicts)
    {
        super();
        this.project = project;
        this.bluejConflicts = bluejConflicts;
        this.nonBluejConflicts = nonBlueJConflicts;
        setTitle("Conflicts found");
        makeWindow();
    }

    private void makeWindow()
    {
        JPanel mainPanel = new JPanel();
        JPanel bluejConflictsPanel = makeConflictsPanel("The following classes had conflicts",
                bluejConflicts);
        JPanel nonBluejConflictsPanel = makeConflictsPanel("The following files had conflicts",
                nonBluejConflicts);
        JPanel buttonPanel = makeButtonPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BlueJTheme.generalBorder);
        mainPanel.add(bluejConflictsPanel);

        if (nonBluejConflicts.size() > 0) {
            mainPanel.add(nonBluejConflictsPanel);
        }

        mainPanel.add(buttonPanel);
        getContentPane().add(mainPanel);

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.teamwork.conflicts", getLocation());
                }
            });

        setLocation(Config.getLocation("bluej.teamwork.conflicts"));
        pack();
    }

    private JPanel makeConflictsPanel(String headline, List conflicts)
    {
        JPanel labelPanel = new JPanel();

        {
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
            labelPanel.setBorder(BlueJTheme.dialogBorder);

            /*labelPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Conflict"),
                    BlueJTheme.generalBorder));*/
            labelPanel.setAlignmentX(LEFT_ALIGNMENT);

            //heading
            heading = new JLabel(headline);

            Font smallFont = heading.getFont().deriveFont(Font.BOLD, 12.0f);
            heading.setFont(smallFont);
            labelPanel.add(heading);
            labelPanel.add(Box.createVerticalStrut(5));

            JPanel conflictsPanel = new JPanel();
            conflictsPanel.setLayout(new BoxLayout(conflictsPanel,
                    BoxLayout.Y_AXIS));
            conflictsPanel.setAlignmentX(LEFT_ALIGNMENT);

            //the conflicting files labels
            for (Iterator i = conflicts.iterator(); i.hasNext();) {
                String conflict = (String) i.next();
                conflictsPanel.add(new JLabel(conflict));
            }

            JScrollPane scrollPane = new JScrollPane(conflictsPanel);
            labelPanel.add(scrollPane);
        }

        return labelPanel;
    }

    /**
     * Create the button panel with a Resolve button and a close button
     * @return JPanel the buttonPanel
     */
    private JPanel makeButtonPanel()
    {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        {
            buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

            //close button
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt)
                    {
                        setVisible(false);
                    }
                });

            //resolve button
            JButton resolveButton = new JButton("Show conflicts");
            resolveButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt)
                    {
                        project.openEditorsForSelectedTargets();

                        // move to resolve button
                        dispose();
                    }
                });

            getRootPane().setDefaultButton(resolveButton);

            buttonPanel.add(resolveButton);
            buttonPanel.add(closeButton);
            resolveButton.setEnabled(bluejConflicts.size() > 0);
        }

        return buttonPanel;
    }
}
