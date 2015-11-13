/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.groupwork.git;

import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommand;
import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.Git;

/**
 *
 * @author Fabio Hedayioglu
 */
public class GitRepository implements Repository {

    private final File projectPath;
    private final String protocol; // Only for data collection
    private final String reposUrl;

    private final Git client;
    private final String userName;
    private String password;

    public GitRepository(File projectPath, String protocol, String reposUrl, Git client, String userName, String password) {
        this.projectPath = projectPath;
        this.protocol = protocol;
        this.reposUrl = reposUrl;
        this.client = client;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void setPassword(TeamSettings newSettings) {
        this.password = newSettings.getPassword();
    }

    @Override
    public boolean versionsDirectories() {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public TeamworkCommand checkout(File projectPath) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files, String commitComment) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public TeamworkCommand shareProject() {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public TeamworkCommand getStatus(StatusListener listener, FileFilter filter, boolean includeRemote) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public TeamworkCommand getModules(List<String> modules) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public TeamworkCommand getLogHistory(LogHistoryListener listener) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public boolean prepareDeleteDir(File dir) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public void prepareCreateDir(File dir) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public FileFilter getMetadataFilter() {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public void getAllLocallyDeletedFiles(Set<File> files) {
        throw new UnsupportedOperationException("Not implemented yet."); 
    }

    @Override
    public String getVCSType() {
        return "Git";
    }

    @Override
    public String getVCSProtocol() {
        return protocol;
    }

}
