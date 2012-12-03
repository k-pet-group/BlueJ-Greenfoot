/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
package bluej.collect;

/**
 * Package-visible Enum with all the events that we might send to the server.
 * 
 *
 */
enum EventName
{
    BLUEJ_START("bluej_start"),
    BLUEJ_FINISH("bluej_finish"),
    PROJECT_OPENING("project_opening"),
    PROJECT_CLOSING("project_closing"),

    ADD("file_add"),
    DELETE("file_delete"),
    RENAME("rename"),
    MULTI_LINE_EDIT("multi_line_edit"),
    COMPILE("compile"),
    
    // Debugger: 
    DEBUGGER_OPEN("debugger_open"),
    DEBUGGER_CLOSE("debugger_close"),
    DEBUGGER_TERMINATE("debugger_terminate"),
    DEBUGGER_CONTINUE("debugger_continue"),
    DEBUGGER_HALT("debugger_halt"),
    DEBUGGER_STEP_INTO("debugger_stepinto"),
    DEBUGGER_STEP_OVER("debugger_stepover"),
    DEBUGGER_HIT_BREAKPOINT("debugger_hit_breakpoint"),
    
    DEBUGGER_BREAKPOINT_ADD("debugger_breakpoint_add"),
    DEBUGGER_BREAKPOINT_REMOVE("debugger_breakpoint_remove"),
    
    INVOKE_DEFAULT_CONSTRUCTOR("invoke_default_constructor"),
    INVOKE_METHOD("invoke_method"),
    
    REMOVE_OBJECT("remove_object"),
    
    CODEPAD_SUCCESS("codepad_success"),
    CODEPAD_ERROR("codepad_error"),
    CODEPAD_EXCEPTION("codepad_exception"),
    
    TERMINAL_OPEN("terminal_open"),
    TERMINAL_CLOSE("terminal_close"),
    
    VCS_COMMIT("vcs_commit"),
    VCS_HISTORY("vcs_history"),
    VCS_SHARE("vcs_share"),
    VCS_STATUS("vcs_status"),
    VCS_UPDATE("vcs_update"),
    
    RESETTING_VM("resetting_vm");
    
    private final String name;
    
    private EventName(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
    
}
