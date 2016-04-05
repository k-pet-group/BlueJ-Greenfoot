/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.groupwork.ui;

import bluej.groupwork.TeamStatusInfo;
import bluej.pkgmgr.Project;
import java.io.File;
import java.util.Set;

/**
 * A Swing based user interface to commit and push.
 * @author Fabio Heday
 */
public interface CommitAndPushInterface
{
    public void setVisible(boolean show);
    
    public String getComment();
    
    public void reset();
    
    /**
     * Get a list of the layout files to be committed
     */
    public Set<File> getChangedLayoutFiles();
    
    /**
     * Get a set of the layout files which have changed (with status info).
     */
    public Set<TeamStatusInfo> getChangedLayoutInfo();
    
    public boolean includeLayout();
    
    /**
     * Start the activity indicator.
     */
    public void startProgress();
    
    /**
     * Stop the activity indicator. Call from any thread.
     */
    public void stopProgress();
    
    public Project getProject();
    
    
}
