package org.bluej.extensions.submitter;

import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;


/**
 * This displays a JDialog panel that has a tree of the loaded stuff in it
 * So, basically, what this should do is to display a tree using the data model provided.
 */
class TreeDialog implements ActionListener
  {
  private Stat    stat;
  private JButton okButton, cancelButton;
  private JDialog thisDialog;
  private JTree   workJTree;

  /**
   * Constructor for the TreeDialog object
   * @param  sp  Description of the Parameter
   */
  public TreeDialog(Stat i_stat, JFrame thisParent)
    {
    stat = i_stat;

    workJTree = getJTree();
    JScrollPane scrollPane = new JScrollPane(workJTree);
    Dimension treeDim = new Dimension (300,200);
    scrollPane.setPreferredSize(treeDim);
    
    JPanel buttonPanel = new JPanel();
    okButton = new JButton(stat.bluej.getLabel("okay"));
    okButton.addActionListener(this);
    buttonPanel.add(okButton);

    cancelButton = new JButton(stat.bluej.getLabel("cancel"));
    cancelButton.addActionListener(this);
    buttonPanel.add(cancelButton);

    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(scrollPane,BorderLayout.CENTER);
    mainPanel.add(buttonPanel,BorderLayout.SOUTH);
  
    thisDialog = new JDialog(thisParent,stat.bluej.getLabel("message.selectscheme"),true);
    thisDialog.setContentPane(mainPanel);
    thisDialog.pack();
    thisDialog.setLocation(100,100);
    }


  /**
   * Utility
   */
  public void setVisible ( boolean status )
    {
    thisDialog.setVisible(status);
    }

  /**
   * This creates the tree with all the bits and pieces right
   */
  private JTree getJTree ()
    {
    JTree risul = new JTree(stat.treeData.getTreeModel());
    risul.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
    risul.setRootVisible(false);
    risul.putClientProperty("JTree.lineStyle", "Angled");
    risul.setExpandsSelectedPaths(true);

    return risul;
    } 

  /**
   *  This is on the two buttons
   */
  public void actionPerformed(ActionEvent event)
    {
    Object source = event.getSource();
    
    if (source == okButton) 
      {
      TreePath path = workJTree.getSelectionPath();
      if ( path == null ) return;

      String showPath=stat.treeData.getPathAsString(path);
      stat.submitDialog.schemeSelectedSet(showPath);
      thisDialog.dispose();
      return;
      }

    if ( source == cancelButton )
      {
      thisDialog.dispose();
      }
    
    }





}

