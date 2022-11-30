/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2015,2016,2017  Michael Kolling and John Rosenberg
 
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
    PACKAGE_OPENING("package_opening"),
    PACKAGE_CLOSING("package_closing"),

    ADD("file_add"),
    DELETE("file_delete"),
    CONVERT_STRIDE_TO_JAVA("convert_stride_to_java"),
    CONVERT_JAVA_TO_STRIDE("convert_java_to_stride"),
    RENAME("rename"),
    EDIT("edit"),
    COMPILE("compile"),
    FILE_OPEN("file_open"),
    FILE_SELECT("file_select"),
    FILE_CLOSE("file_close"),

    SHOWN_ERROR_INDICATOR("shown_error_indicator"),
    SHOWN_ERROR_MESSAGE("shown_error_message"),
    FIX_EXECUTED("fix_executed"),

    UNKNOWN_FRAME_COMMAND("unknown_frame_command"),
    FRAME_CATALOGUE_SHOWING("frame_catalogue_showing"),
    VIEW_MODE_CHANGE("view_mode_change"),

    CODE_COMPLETION_STARTED("code_completion_started"),
    CODE_COMPLETION_ENDED("code_completion_ended"),

    GREENFOOT_WINDOW_ACTIVATED("greenfoot_window_activated"),
    GREENFOOT_WORLD_RESET("greenfoot_world_reset"),
    GREENFOOT_WORLD_ACT("greenfoot_world_act"),
    GREENFOOT_WORLD_RUN("greenfoot_world_run"),
    GREENFOOT_WORLD_PAUSE("greenfoot_world_pause"),
    
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
    
    INVOKE_METHOD("invoke_method"),
    
    BENCH_GET("bench_get"),    
    REMOVE_OBJECT("remove_object"),
    BENCH_TO_FIXTURE("bench_to_fixture"),
    FIXTURE_TO_BENCH("fixture_to_bench"),
    
    INSPECTOR_SHOW("inspector_show"),
    INSPECTOR_HIDE("inspector_hide"),
    
    CODEPAD("codepad"),
    
    TERMINAL_OPEN("terminal_open"),
    TERMINAL_CLOSE("terminal_close"),
    
    START_TEST("start_test"),
    CANCEL_TEST("cancel_test"),
    END_TEST("end_test"),
    RUN_TEST("run_test"),
    ASSERTION("assertion"),
    
    VCS_COMMIT("vcs_commit"),
    VCS_HISTORY("vcs_history"),
    VCS_PUSH("vcs_push"),
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
