/*
 * Blackbox Code Viewer sample
 * Copyright (c) 2012, Neil Brown
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


class DatabaseInterface
{

    private Connection dbConn;
    private PreparedStatement stmtGetUser;
    private PreparedStatement stmtGetProjects;
    private PreparedStatement stmtGetSourceFiles;
    private PreparedStatement stmtGetSourceHistories;


    DatabaseInterface() throws SQLException
    {
        String userName = "whitebox";
        String password = "ivorycube";
        String databaseName = "blackbox_production";
        String dbURL = "jdbc:mysql://localhost/" + databaseName + "?useUnicode=true&characterEncoding=UTF-8";
        dbConn = DriverManager.getConnection(dbURL, userName, password);
    }
    
    
    int getUserIdForUUID(String uuid) throws SQLException
    {
        if (stmtGetUser == null)
        {
            stmtGetUser = dbConn.prepareStatement("SELECT id FROM users WHERE uuid = ?");
        }
        
        stmtGetUser.setString(1, uuid);
        
        return getSingleInt(stmtGetUser.executeQuery());
    }
    
    
    ArrayList<IdName> getProjectsForUser(int userId) throws SQLException
    {
        if (stmtGetProjects == null)
        {
            stmtGetProjects = dbConn.prepareStatement("SELECT id, name FROM projects WHERE user_id = ?");
        }
        
        stmtGetProjects.setInt(1, userId);
        
        ResultSet rs = stmtGetProjects.executeQuery();
        
        ArrayList<IdName> r = new ArrayList<IdName>();
        while (rs.next())
        {
            IdName p = new IdName();
            p.id = rs.getInt(1);
            p.name = rs.getString(2);
            r.add(p);
        }
        
        rs.close();
        
        return r;
    }
    
    ArrayList<IdName> getSourceFilesForProject(int projectId) throws SQLException
    {
        System.out.println("Preparing source files statement");
        if (stmtGetSourceFiles == null)
        {
            stmtGetSourceFiles = dbConn.prepareStatement("SELECT id, name FROM source_files WHERE project_id = ?");
        }
        
        stmtGetSourceFiles.setInt(1, projectId);
        
        System.out.println("Executing query");
        
        ResultSet rs = stmtGetSourceFiles.executeQuery();
        
        System.out.println("Processing results");
        
        ArrayList<IdName> r = new ArrayList<IdName>();
        while (rs.next())
        {
            IdName p = new IdName();
            p.id = rs.getInt(1);
            p.name = rs.getString(2);
            r.add(p);
        }
        
        rs.close();
        
        System.out.println("Processed results");
        
        return r;
    }
    
    ArrayList<SourceHistory> getHistoriesForFile(int sourceFileId) throws SQLException
    {
        if (stmtGetSourceHistories == null)
        {
            stmtGetSourceHistories = dbConn.prepareStatement("SELECT source_history_type, content FROM source_histories WHERE source_file_id = ? ORDER BY id ASC");
        }
        
        stmtGetSourceHistories.setInt(1, sourceFileId);
        
        ResultSet rs = stmtGetSourceHistories.executeQuery();
        
        ArrayList<SourceHistory> r = new ArrayList<SourceHistory>();
        while (rs.next())
        {
            SourceHistory p = new SourceHistory();
            p.type = rs.getString(1);
            p.content = rs.getString(2);
            r.add(p);
        }
        
        rs.close();
        
        return r;
    }
    
    
    private static int getSingleInt(ResultSet rs) throws SQLException
    {
        if (rs.next())
        {
            int x = rs.getInt(1);
            rs.close();
            return x;
        }
        
        return -1;
    }
}
