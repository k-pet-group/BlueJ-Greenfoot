/*
 * Blackbox Code Viewer sample
 * Copyright (c) 2012, Neil Brown
 */

import java.awt.EventQueue;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JOptionPane;

import bluej.Config;


public class Viewer
{
    // Warning -- the path below needs to be changed if not on Linux:
    // Change to the path that contains bluej.jar
    private static final String BLUEJ_INSTALL_PATH = "/usr/share/bluej";
    // e.g. for Mac: /Applications/BlueJ.app/Contents/Resources/Java
    
    public static void main(String[] args)
    {
        Viewer v = new Viewer();
        
        try
        {
            DatabaseInterface db = new DatabaseInterface();
        
            // Ask which file they are interested in viewing:
            
            String uuid = JOptionPane.showInputDialog("Enter user's UUID");
            
            int userId = db.getUserIdForUUID(uuid);
            
            IdName project = ListDialog.ask("Select project", db.getProjectsForUser(userId));
            if (project == null) return;
            IdName sourceFile = ListDialog.ask("Select source file", db.getSourceFilesForProject(project.id));
            if (sourceFile == null) return;
            
            final ArrayList<SourceHistory> history = db.getHistoriesForFile(sourceFile.id);
            
            final ArrayList<String> versions = SourceHistory.getAllVersions(history);
            
            // Show the history in a BlueJ editor:
            

            Config.initialise(new File(BLUEJ_INSTALL_PATH), new Properties(), false);
            
            MoeEditorParameters parameters = new MoeEditorParameters("Viewer", null, Config.moeUserProps, null, null);
            parameters.setCode(true);
            final MoeEditor moe = new MoeEditor(parameters);
            
            moe.getSourcePane().addMouseWheelListener(new MouseWheelListener() {
                private int curVersion = 0;                
                
                @Override
                public void mouseWheelMoved(MouseWheelEvent e)
                {
                    int prevVersion = curVersion;
                    if (e.getUnitsToScroll() < 0)
                    {
                        curVersion = Math.max(0, curVersion - 1);
                    }
                    else if (e.getUnitsToScroll() > 0)
                    {
                        curVersion = Math.min(history.size() - 1, curVersion + 1);
                    }
                    e.consume();
                    
                    if (prevVersion != curVersion)
                    {
                        MoeSyntaxDocument doc = moe.getSourceDocument();
                        try {
                            // This replacement does have the disadvantage of always pinging the cursor to the end of the file,
                            // and causing a flicker and re-parse, but this is only a quick demonstration...
                            doc.remove(0, doc.getLength());
                            doc.insertString(0, versions.get(curVersion), null);
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        moe.writeMessage("Version: " + curVersion);
                    }
                }
            });
            
            moe.setReadOnly(true);
            moe.setVisible(true);
            
            EventQueue.invokeLater(new Runnable() { public void run() {
                // Start with first version:
                try {
                    moe.getSourceDocument().insertString(0, versions.get(0), null);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                moe.getSourceDocument().enableParser(true);
                moe.writeMessage("Version: 0");           
                
                moe.getSourceDocument().flushReparseQueue();
            }});
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
