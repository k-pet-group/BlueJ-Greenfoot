package bluej.extmgr;

import bluej.*;
import bluej.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;

/**
 *  This class can display info on a particular extension. It is not really
 *  bound to the HelpDialog and may be useful in the future.
 */
class HelpDetailDialog extends JDialog implements ActionListener
{
    private final String extensionsTag = Config.getString("extmgr.extensions");
    private final String detailsTag = Config.getString("extmgr.details");
    private final String installedString = Config.getString("extmgr.installed");
    private final String projectString = Config.getString("extmgr.project");
    private final String statusTag = Config.getString("extmgr.details.status");
    private final String nameTag = Config.getString("extmgr.details.name");
    private final String locationTag = Config.getString("extmgr.details.location");
    private final String typeTag = Config.getString("extmgr.details.type");
    private final String versionTag = Config.getString("extmgr.details.version");
    private final String dateTag = Config.getString("extmgr.details.date");
    private final String urlTag = Config.getString("extmgr.details.url");
    private final String preferencesTag = Config.getString("extmgr.details.preferences");
    private final String descriptionTag = Config.getString("extmgr.details.description");

    private JLabel statusField, nameField, locationField, typeField, versionField;
    private JLabel dateField, urlField;
    private JTextArea descriptionField;
    private URL url;
    private JButton closeButton;

    /**
     * Constructor
     */
    HelpDetailDialog(Dialog owner)
    {
        super(owner, "Details");

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints tag = new GridBagConstraints();
        tag.gridx = 0;
        tag.anchor = GridBagConstraints.NORTHEAST;

        GridBagConstraints value = new GridBagConstraints();
        value.gridx = 1;
        value.anchor = GridBagConstraints.NORTHWEST;
        value.gridwidth = 3;

        panel.add(new JLabel(statusTag + ":  "), tag);
        panel.add(new JLabel(nameTag + ":  "), tag);
        panel.add(new JLabel(locationTag + ":  "), tag);
        panel.add(new JLabel(typeTag + ":  "), tag);
        panel.add(new JLabel(versionTag + ":  "), tag);
        panel.add(new JLabel(dateTag + ":  "), tag);
        panel.add(new JLabel(urlTag + ":  "), tag);
        panel.add(new JLabel(descriptionTag + ":  "), tag);

        statusField = new JLabel();
        nameField = new JLabel();
        locationField = new JLabel();
        typeField = new JLabel();
        versionField = new JLabel();
        dateField = new JLabel();
        dateField.setToolTipText("yyyy/mm/dd hh:mm:ss");

        urlField = new JLabel();
        urlField.setCursor(new Cursor(Cursor.HAND_CURSOR));
        urlField.addMouseListener(
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    openURL();
                }
            });

        descriptionField = new JTextArea(4, 40);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        descriptionField.setEnabled(false);
        descriptionField.setDisabledTextColor(Color.black);
        descriptionField.setBackground(dateField.getBackground());
        // I need to put into a scrollbar in case the text is too much
        JScrollPane descriptionScroller = new JScrollPane(descriptionField);
        descriptionScroller.setBorder(null);

        panel.add(statusField, value);
        panel.add(nameField, value);
        panel.add(locationField, value);
        panel.add(typeField, value);
        panel.add(versionField, value);
        panel.add(dateField, value);
        panel.add(urlField, value);
        panel.add(descriptionScroller, value);

        // I need to put this beast into a scroll pane
        JScrollPane detailScroll = new JScrollPane(panel);

        // The close button goes into a panel, to make it nice...
        JPanel buttonPanel = new JPanel();
        closeButton = new JButton(Config.getString("close"));
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);

        // TIme to put the two main panels into the root pane
        JPanel rootPane = (JPanel) getContentPane();
        rootPane.setLayout(new BorderLayout());
        rootPane.setBorder(Config.dialogBorder);

        rootPane.add(detailScroll, BorderLayout.CENTER);
        rootPane.add(buttonPanel, BorderLayout.SOUTH);

        // save position when window is moved
        addComponentListener(
            new ComponentAdapter()
            {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.extmgr.helpdialog.details", getLocation());
                }
            });

        setLocation(Config.getLocation("bluej.extmgr.helpdialog.details"));
        pack();
        // Do not call the setVisible
    }


    /**
     *  Called when the button is pressed
     *
     * @param  evt  Description of the Parameter
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object src = evt.getSource();
        if (src == null) return;

        if (src == closeButton) setVisible(false);
    }


    /**
     *  Utility, to make code clean. Concatenate a series of strings separated
     *  by, We are just tryng to be quick, not that it really matters but still
     *  better nd clearer
     *
     * @param  list  Description of the Parameter
     * @return       Description of the Return Value
     */
    private String commaList(String[] list)
    {
        if (list == null)
            return "";
        if (list.length < 1)
            return "";

        StringBuffer commaList = new StringBuffer(200);

        int lastIndex = list.length - 1;
        // We know that there is at least one element
        for (int index = 0; ; index++) {
            commaList.append(list[index]);
            if (index >= lastIndex)
                break;
            // There is another element in the list, add a comma
            commaList.append(",");
        }

        return commaList.toString();
    }


    /**
     * When a different extension is shown you call this one.
     */
    void updateInfo(ExtensionWrapper wrapper)
    {
        if (wrapper == null) return;

        statusField.setText(wrapper.getExtensionStatus());
        statusField.setForeground(wrapper.isValid() ? Color.black : Color.red);

        nameField.setText(wrapper.safeGetExtensionName());
        locationField.setText(wrapper.getExtensionFileName());
        typeField.setText((wrapper.getProject() != null) ? projectString : installedString);
        versionField.setText(wrapper.safeGetExtensionVersion());
        dateField.setText(wrapper.getExtensionModifiedDate());

        url = wrapper.safeGetURL();
        if (url == null)
            urlField.setText(null);
        else {
            urlField.setText(url.toExternalForm());
            urlField.setForeground(Color.blue);
        }

        descriptionField.setText(wrapper.safeGetExtensionDescription());
        descriptionField.setCaretPosition(0);

        validate();
        pack();
        setVisible(true);
    }


    /**
     * Description of the Method
     */
    private void openURL()
    {
        if (url == null)
            return;
        Utility.openWebBrowser(url.toExternalForm());
    }
}

