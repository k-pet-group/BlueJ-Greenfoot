package org.bluej.extensions.submitter;

import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.text.html.*;

/**
 * DIsplay a result in a nice window with a OK button.
 * Really nothing special, just that it si cleaner to have an extra class doing this
 * As all windowing stuff this is initialized at the beginning and is the USED
 * when it is needed.
 */
public class ResultDialog implements ActionListener
  {
  private Stat      stat;
  private JButton   okButton;
  private JDialog   thisDialog;
  private JEditorPane resultArea;    

  /**
   * Constructor 
   * @param  i_stat The usual container for global objects
   * @param parentFrame the fram parent of this Dialog
   */
  public ResultDialog(Stat i_stat, Frame parentFrame)
    {
    stat = i_stat;

    resultArea = new JEditorPane();
    // We assume that the result will be in HTML format...
    HTMLEditorKit edKit = new HTMLEditorKit();
    resultArea.setEditorKit(edKit);
    resultArea.setEditable(false);

    JScrollPane scrollPane = new JScrollPane(resultArea);
    Dimension dimension = new Dimension (300,250);
    scrollPane.setMaximumSize(dimension);
    scrollPane.setPreferredSize(dimension);
    
    JPanel buttonPanel = new JPanel();
    okButton = new JButton(stat.bluej.getLabel("okay"));
    okButton.addActionListener(this);
    buttonPanel.add(okButton);

    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(scrollPane,BorderLayout.CENTER);
    mainPanel.add(buttonPanel,BorderLayout.SOUTH);
  
    thisDialog = new JDialog(parentFrame,stat.bluej.getLabel("message.result"));
    thisDialog.setContentPane(mainPanel);
    thisDialog.pack();
    thisDialog.setLocation(100,100);
    }


  /**
   * This is the handle that the caller uses to show the actual result.
   * We are quite tolerant on result values, we accept null as a valid value 
   * 
   * @param result The string to display, it can be null and nothing will be done.
   */
  void showResult ( String result )
    {
    if ( result == null ) return;

    resultArea.setText(result);
    thisDialog.setVisible(true);
    }


  /**
   *  This is on the two buttons
   */
  public void actionPerformed(ActionEvent event)
    {
    Object source = event.getSource();

    // The only thing I should do is to dispose the dialog.    
    thisDialog.dispose();
    }

  }