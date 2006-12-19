unit exfile;
{* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
Author:       Bill BEAM

Description:  The TExFile component is used to launch one or more processes.
              A single instance of the component can keep track of multiple
              processes and provides a variety of useful properties and methods.
              You can launch one or more processes in the background or cause
              the main application to wait for a process to terminate or time-
              out.  You can use the event handlers to make something happen when
              a process terminates or fails to launch.

Creation:     December 29, 1998

Version:      1.05

EMail:        billb@catoctinsoftware.com
              wilbeam@erols.com            http://www.catoctinsoftware.com

Support:      Use comments/suggestions at website or email. I am interested
              in any comments or questions you may have.

Legal issues: Copyright (C) 1998, 1999 by Bill Beam
              6714 Fish Hatchery Road
              Frederick, MD 21702 (USA)

              This software is provided 'as-is', without any express or
              implied warranty.  In no event will the author be held liable
              for any  damages arising from the use of this software.

              Permission is granted to anyone to use this software for any
              purpose, including commercial applications, and to alter it
              and redistribute it freely, subject to the following
              restrictions:

              1. The origin of this software must not be misrepresented,
                 you must not claim that you wrote the original software.
                 If you use this software in a product, an acknowledgment
                 in the product documentation would be appreciated but is
                 not required.

              2. Altered source versions must be plainly marked as such, and
                 must not be misrepresented as being the original software.

              3. This notice may not be removed or altered from any source
                 distribution.

Updates:

Jul 07, 1999 V1.04
     1. Added ProcCurrentDir Property. Suggested by Lani.
     2. Expanded Priority Property. Suggested by Lani.

Jul 10, 1999 V1.05
     1. Removed hThread from GetProcInfo method. IMO better off
        being closed before starting new thread.
     2. Added WaitForInputIdle for ThreadedWait should someone
        want to find a slow starting window just after launch.

{* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *}
interface

uses
  Windows, SysUtils, Classes, Messages;

type
  TProcCompleted = Procedure (sender : Tobject;
                              evFileName : String;
                              evIdentifier : String;
                              evRetValue : Integer;
                              evTimedOut : Boolean) of Object;
  TLaunchFailed = Procedure (sender : Tobject;
                             evFileName : String;
                             evIdentifier : String;
                             evErrorCode : Integer;
                             evErrorMessage : String) of Object;
  TWindowType = (wtNorm, wtMinimize, wtMaximize, wtHide,
                wtMinNoActivate, wtShowNoActivate);
  TWindowTypes = Array[TWindowType] of Word;
  TErrMsg = (emZero, emDuplicateProc, emOnlyOneMeth, emTimedOut,
             emInValidDir, emUnknown);
  TErrMsgs = Array[TErrMsg] of String [55];
  TUseEvent = (ueAll, ueOnLaunchFailed, ueOnProcCompleted, ueNone);
  TPriorityClass = (prNormal, prIdle, prHigh, prRealTime);
  TPriorityClasses = Array[TPriorityClass] of Word;
  TStartType = (NonThreadedWait, ThreadedWait, Independent);
  TVersion = string;
  TProcInfo =(PrhProcess, PrDwProcessId, PrHWND);
  PProcList = ^AThreadRecord;
  AThreadRecord = record
      PrName : String;
      PrProcIdentifier : String;
      PrhProcess : THandle;
      PrDwProcessId : Dword;
      PrhThread : THandle;
      PrHWND : HWND;
      PrStartType : TStartType;
  end;
  TExFile = class(TComponent)
  private
   FOnLaunchFailed: TLaunchFailed;
   FOnProcCompleted : TProcCompleted;
   FProcFileName: String;
   FProcFileNamelc : String;
   FFParams: String;
   FProcIdentifier : String;
   FProcCurrentDir : String;
   FWindowType: TWindowType;
   FWaitUntilDone : Boolean;
   FPriorityClass: TPriorityClass;
   StartUpInfo: TStartUpInfo;
   ProcessInfo: TProcessInformation;
   hEventHandle: THandle;
   hMutex : THandle;
   FErrorCode : Integer;
   FExitCode : Integer;
   FUseEvent : TUseEvent;
   FTimeOutSec : Integer;
   FTimedOut : Boolean;
   FMilliSeconds : DWORD;
  protected
   HandLst : TList;
   AProcList : PProcList;
   FVersion : TVersion;
   PCurDir : PChar;
   procedure SetVersion(Value: TVersion);
   Procedure SetWindowType(Value: TWindowType);
   procedure SetPriorityClass(Value: TPriorityClass);
   procedure SetTimeOutSec(Value : Integer);
   procedure SetProcFileName(Value: String);
   procedure ListMaint;
   procedure AddToList(StartType : TStartType);
   function GethProcess(StatProcName, StatProcIdentifier : String; Var Hidx : Integer): Boolean;
   function GetExitCode(hProcess : THandle) : Boolean;
   function DuplicateProc : Boolean;
   function StartProcess(StartType : TStartType) : Boolean;
   Procedure ErrorEvent(efError : integer; efMessage : String);
   function AlreadyRunning(GRT : TStartType) : Boolean;
   function AssignCurrentDir : Boolean;
  Public
   function Execute: Boolean;
   function CloseProcess : Boolean;
   function GetProcStatus : Boolean;
   function CloseThreads : Boolean;
   function GetErrorCode : Integer;
   function GetReturnCode : Integer;
   function ExErrorMessage(ExErrorCode : Integer) : String;
   procedure ResetProps;
   function GetProcInfo(GPIType : TProcInfo; Var GPIReturn : Integer) : Boolean;
   constructor Create(Aowner: TComponent); Override;
   destructor Destroy; override;
  published
   property Version: TVersion read FVersion write SetVersion stored False;
   property ProcFileName: String read FProcFileName write SetProcFileName;
   property ProcParameters: String read FFParams write FFParams;
   property ProcIdentifier: String read FProcIdentifier write FProcIdentifier;
   property ProcCurrentDir: String read FProcCurrentDir write FProcCurrentDir;
   property OnProcCompleted : TProcCompleted read FOnProcCompleted write FonProcCompleted;
   property OnLaunchFailed : TLaunchFailed read FOnLaunchFailed write FOnLaunchFailed;
   property WindowType: TWindowType read FWindowType write SetWindowType;
   property WaitUntilDone: Boolean read FWaitUntilDone write FWaitUntilDone;
   property UseEvent : TUseEvent read FUseEvent write FUseEvent;
   property Priority: TPriorityClass read FPriorityClass write SetPriorityClass;
   property TimeOutSec : Integer read FTimeOutSec write SetTimeOutSec;
end;

type
  TProcThread = class(TThread)
  Private
   thArray : array[0..1] of THandle;
   thFileName : String;
   thIdentifier : String;
   thRetVal : DWord;
   FOnThreadDone: TProcCompleted;
   thMutex : THandle;
   thUseEvent : TUseEvent;
   thMilliseconds : DWORD;
   thRetType : Boolean;
  protected
   procedure Execute; override;
   procedure CallOnTerminate;
   Constructor Create(vProcHandle: THandle;
                     vProcEventHandle : THandle;
                     vFileName : String;
                     vProcIdentifier : String;
                     vDoneMethod: TProcCompleted;
                     vMutex : THandle;
                     vUseEvent : TUseEvent;
                     vMilliseconds : DWORD);
end;

   procedure Register;

implementation

const
  cWindowType : TWindowTypes = (SW_SHOWNORMAL, SW_SHOWMINIMIZED,
       SW_SHOWMAXIMIZED, SW_HIDE, SW_SHOWMINNOACTIVE, SW_SHOWNA);
  cPriorityClass : TPriorityClasses = (NORMAL_PRIORITY_CLASS,
       IDLE_PRIORITY_CLASS, HIGH_PRIORITY_CLASS, REALTIME_PRIORITY_CLASS);
  cErrMsg : TErrMsgs = ('Zero',
           'Another Process with the same Name and ID is executing.',
           'Cannot mix ''WaitUntilDone'' types.',
           'Process timed out',
           'Current Directory Invalid',
           'Unknown Error Code');
  {Thread array locations}
  ChPROCESS = 0;
  ChEvent = 1;
  {Timeout Constants}
  MAX_ALLOWED : DWORD = 3600000;
  MAX_IDLE : DWORD = 3000;
  {$IFDEF VER90}
  CompVer = 'D2';
  {$ENDIF}
  {$IFDEF VER100}
  CompVer = 'D3';
  {$ENDIF}
  {$IFDEF VER120}
  CompVer = 'D4';
  {$ENDIF}
  cVersion : TVersion = ('1.05 (' + 'dontknow' + ')'); { Current version number }

constructor TExFile.Create(Aowner: TComponent);
begin
  inherited Create(AOwner);
  HandLst := TList.create;
  Fversion := Cversion;
  hEventHandle := CreateEvent(nil, True, False, nil);
  hMutex := CreateMutex(nil, false, nil);
end;

destructor TExFile.Destroy;
var i : Integer;
begin
{signal and wait for waiting threads to release. hProcess handles
are closed as well}
  PulseEvent(hEventHandle);
  for i := 0 to (HandLst.Count - 1) do begin
    AProcList := HandLst.Items[i];
    if AProcList^.PrStartType = NonThreadedWait then begin
      try
        CloseHandle(AProcList^.PrhProcess);
      except
      end;
      try
        CloseHandle(AProcList^.PrhThread);
      except
      end;
    end;
    Dispose(AProcList);
  end; {for}
  HandLst.Free;
  CloseHandle(hEventHandle);
  CloseHandle(hMutex);
  inherited Destroy;
end;

constructor TProcThread.Create(vProcHandle: THandle;
                               vProcEventHandle : THandle;
                               vFileName : String;
                               vProcIdentifier : String;
                               vDoneMethod: TProcCompleted;
                               vMutex : THandle;
                               vUseEvent : TUseEvent;
                               vMilliSeconds : DWORD);
begin
  Inherited Create(True);
  thArray[ChPROCESS] := vProcHandle;
  thArray[ChEvent] := vProcEventHandle;
  thFileName := vFileName;
  thIdentifier := vProcIdentifier;
  FreeOnTerminate := True;
  FonThreadDone := vDoneMethod;
  thMutex := vMutex;
  thUseEvent := vUseEvent;
  thMilliseconds := vMilliSeconds;
  Resume;
end;

procedure TProcThread.Execute;
var Signaled : Integer;
begin
  Signaled := WaitForMultipleObjects(2, @thArray, False, thMilliseconds);
  if Signaled <> WAIT_OBJECT_0 + ChEvent then   //Event not signaled
    begin
      if Signaled = WAIT_OBJECT_0 + ChPROCESS then  //hProcess signaled
        GetExitCodeProcess(thArray[ChPROCESS], thRetVal)
      else
        thRetVal := thMilliseconds div 1000;  //WAIT_TIMEOUT or WAIT_ABANDONED
      thRetType := (Signaled <> WAIT_OBJECT_0 + ChPROCESS);
      if assigned(FOnThreadDone) and
        ((thUseEvent = ueAll) or (thUseEvent = ueOnProcCompleted)) then
        begin
          WaitForSingleObject(thMutex, INFINITE);
          Synchronize(CallOnTerminate);
          ReleaseMutex(thMutex);
        end;
     end;
   CloseHandle(thArray[ChPROCESS]); //Close hProcess. hThread is already closed.
   Terminate;
end;

procedure TProcThread.CallOnTerminate;
begin
  FOnThreadDone(Self, thFileName, thIdentifier, thRetVal, thRetType);
end;

procedure TExFile.SetWindowType(Value : TWindowType);
begin
  FWindowType := Value;
end;

procedure TexFile.ResetProps;
begin
  ProcFileName := '';
  ProcParameters := '';
  ProcIdentifier := '';
  WindowType := wtNorm;
  Priority := prNormal;
  UseEvent := ueAll;
  WaitUntilDone := false;
  TimeOutSec := 0;
  ProcCurrentDir := '';
end;

procedure TExFile.SetPriorityClass(Value : TPriorityClass);
begin
  FPriorityClass := Value;
end;

procedure TExFile.SetVersion(Value: TVersion);
begin
  {Do Nothing.  Set by create}
end;

procedure TExFile.SetProcFileName(Value : String);
begin
  FProcFileNamelc := LowerCase(Value);
  FProcFileName := Value;
end;

Procedure TExFile.SetTimeOutSec(Value : Integer);
begin
  FTimeOutSec := Value;
  if Value = 0 then
    FMilliSeconds := INFINITE
  else
    if Value > 3600 then
      FMilliSeconds := MAX_ALLOWED
    else
      FMilliSeconds := Value * 1000;
end;

function TExFile.Execute: Boolean;
var
  WaitStatus : Integer;
begin
  Result := False;
  FErrorCode := 0;

  if FWaitUntilDone then
    begin
      Result := StartProcess(NonThreadedWait);
      if Result then
        begin
          WaitStatus := WaitforSingleObject(ProcessInfo.hProcess, FMilliSeconds);
          if WaitStatus = WAIT_OBJECT_0 + ChProcess then
            GetExitCodeProcess(ProcessInfo.hProcess, DWord(FExitCode))
          else
            begin
              FExitCode := FTimeOutSec;
              FErrorCode := ord(emTimedOut) * -1;
            end;
          FTimedOut := (WaitStatus <> WAIT_OBJECT_0 + ChProcess);
          CloseHandle(ProcessInfo.hProcess);
          CloseHandle(ProcessInfo.hThread);
          if assigned(FonProcCompleted) and
            ((FUseEvent = ueAll) or (FUseEvent = ueOnProcCompleted)) then
            FonProcCompleted(Self, FProcFileName, FProcIdentifier,
                             FExitCode, FTimedOut);
        end;
    end
  else
    begin
      result := StartProcess(ThreadedWait);
      if result then
        begin
          CloseHandle(ProcessInfo.hThread);
          TProcThread.create(ProcessInfo.hProcess,
                             hEventHandle,
                             FProcFileName,
                             FProcIdentifier,
                             FonProcCompleted,
                             hMutex,
                             FUseEvent,
                             FMilliseconds);
        end;
    end;
end;

Procedure TExFile.AddToList(StartType : TStartType);
begin
  new(AProcList);
  with AProcList^ do begin
    PrName := FProcFileNamelc;
    PrProcIdentifier := FProcIdentifier;
    PrhProcess := ProcessInfo.hProcess;
    PrDwProcessId := ProcessInfo.dwProcessId;
    PrhThread := ProcessInfo.hThread;
    PrStartType := StartType;
    HandLst.add(AProcList);
  end;
end;

Function TExFile.StartProcess(StartType : TStartType) : boolean;
var
  vProcFileName: array of char;
  vIdle : DWORD;
  commandString : String;
begin
  result := false;
  ListMaint;
{sets pointer to lpCurrentDirectory}
  if not AssignCurrentDir then
    exit;
{see if trying to start a NonThreadedWait while another process is running}
  if StartType = NonThreadedWait then
    if AlreadyRunning(ThreadedWait) then
       exit;
{see if trying to start a duplicate process}
  if DuplicateProc then
    exit;

{Start the process}

  commandString := FProcFileName  + ' ' + FFParams;
  SetLength(vProcFileName, (Length(commandString) + 1));
  StrPCopy(Addr(vProcFileName[0]), commandString);
  FillChar(Startupinfo, SizeOf(TstartupInfo), #0);
  with StartupInfo do
    begin
      cb := SizeOf(TStartupInfo);
      dwFlags := STARTF_USESHOWWINDOW;
      wShowWindow := cWindowType[FWindowType];
    end;
  result := CreateProcess(nil,
                          Addr(VProcFileName[0]),
                          nil,
                          nil,
                          False,
                          CREATE_NEW_CONSOLE or cPriorityClass[FPriorityClass],
                          nil,
                          pCurDir,
                          StartupInfo,
                          ProcessInfo);
{Wait no longer than MAX_IDLE for initialization. For ThreadedWait types only}
  if (result) and (StartType = ThreadedWait) then
    begin
      vIdle := WaitForInputIdle(ProcessInfo.hProcess, MAX_IDLE);
      result := ((vIdle = 0) or (vIdle = WAIT_TIMEOUT));
    end;
  if result then
    AddToList(StartType)
  else
    ErrorEvent(GetLastError, SysErrorMessage(GetLastError));
end;

Procedure TExFile.ErrorEvent(efError : integer; efMessage : String);
begin
  FErrorCode := efError;
  if Assigned(FOnLaunchFailed) and
    ((FUseEvent = ueAll) or (FUseEvent = ueOnLaunchFailed)) then
      FOnLaunchFailed(Self, FProcFileName, FProcIdentifier, efError, efMessage);
end;

Function TExFile.DuplicateProc : Boolean;  //returns true if duplicate found
var x : integer;
begin
  result := GethProcess(FProcFileNamelc, FProcIdentifier, x);
  if result then
    ErrorEvent(ord(emDuplicateProc) * -1, cErrMsg[emDuplicateProc]);
end;

Function TExFile.AssignCurrentDir : Boolean;
var Code : Integer;
begin
  Result := true;
  PCurDir := nil;
  if FProcCurrentDir <> '' then
    begin {avoid bringing in the FileCtrl unit for the DirectoryExists function}
      Code := GetFileAttributes(PChar(FProcCurrentDir));
      Result := (Code <> -1) and (FILE_ATTRIBUTE_DIRECTORY and Code <> 0);
      if result then
        PCurDir := PChar(FprocCurrentDir)
      else
        ErrorEvent(ord(emInvalidDir) * -1, cErrMsg[emInValidDir]);
    end;
end;

Function TExFile.GetErrorCode : integer;
begin
  result := FErrorCode;
end;

Procedure TExFile.ListMaint;
var
  I : Integer;
begin
  For I := HandLst.count - 1 downto 0 do
    if not GetExitCode(PProcList(HandLst.Items[I]).PrhProcess) then
       begin
         AProcList := HandLst.Items[I];
         Dispose(AProcList);
         HandLst.delete(I);
       end;
end;

Function TExFile.GetProcStatus : Boolean;
var i : integer;
Begin
  result := GethProcess(FProcFileNamelc, FProcIdentifier, i);
  if result then
     result := GetExitCode(PProcList(HandLst.Items[I]).PrhProcess);
end;

Function TExFile.GethProcess(StatProcName, StatProcIdentifier : String;
                             var Hidx : Integer): Boolean;
var i : integer;
Begin
  result := false;
  Hidx := -1;
  For i := 0 to HandLst.count - 1 do
    if (PProcList(HandLst.Items[I]).PrName = StatProcName) and
       (PProcList(HandLst.Items[I]).PrProcIdentifier = StatProcIdentifier) then
      begin
        result := true;
        Hidx := i;
        break;
      end;
end;

Function TExFile.AlreadyRunning(GRT : TStartType) : Boolean;
var I : integer;
Begin
  result := false;
  For I := 0 to HandLst.count - 1 do
    if (PProcList(HandLst.Items[I]).PrStartType = GRT) then begin
        result := true;
        break;
    end;
  if result then
    ErrorEvent(ord(emOnlyOneMeth) * -1, cErrMsg[emOnlyOneMeth]);
end;

Function TExFile.GetExitCode(hProcess : THandle) : Boolean;
var vExitCode : Dword;
begin
  result := false;
  if GetExitCodeProcess(hProcess, vExitCode) then
     result := (vExitCode = STILL_ACTIVE);
end;

Function TExFile.GetReturnCode : Integer;
begin
  result := FExitCode;
end;

function EnumCallBack(hWindow : HWND; pHArray : Integer): Bool stdcall;
var
  ProcessIdx : DWORD;
begin
  result := Bool(1);
  GetWindowThreadProcessId(hWindow, @ProcessIdx);
  if PProcList(pHArray).PrDwProcessId = processidx then begin
    PProcList(pHArray).PrHWND := hWindow;
    result := False; //stop enumeration
  end;
end;

function TExFile.GetProcInfo(GPIType : TProcInfo; Var GPIReturn : Integer) : Boolean;
var i : Integer;
Begin
  result := GethProcess(FProcFileNamelc, FProcIdentifier, i);
  if result then
    case GPIType of
      PrhProcess     : GPIReturn := PProcList(HandLst.Items[i]).PrhProcess;
      PrdwProcessId  : GPIReturn := PProcList(HandLst.Items[i]).PrdwProcessid;
      PrHWND         :
        begin
          result := (EnumWindows(@EnumCallBack, Integer(HandLst.Items[i])) = false);
          if result then
            GPIReturn := PProcList(HandLst.Items[i]).PrHWND;
        end;
    end;
end;

function TExFile.CloseProcess : Boolean;
var i : Integer;
Begin
  Result := false;
  if GethProcess(FProcFileNamelc, FProcIdentifier, i) then
     result := (EnumWindows(@EnumCallBack, Integer(HandLst.Items[i])) = false);
  if result then
     SendMessage(PProcList(HandLst.Items[i]).PrHWND, WM_CLOSE, 0, 0);
end;

function TExFile.ExErrorMessage(ExErrorCode : Integer) : String;
begin
  if ExErrorCode < 0 then
    if abs(ExErrorCode) < abs(ord(emUnknown)) then
      result := cErrMsg[TErrMsg(abs(ExErrorCode))]
    else
      result := cErrMsg[emUnknown]
  else
    result := SysErrorMessage(ExErrorCode);
end;

Function TExFile.CloseThreads : Boolean;
Begin
result := PulseEvent(hEventHandle);
end;

procedure Register;
begin
  RegisterComponents('Samples', [TExFile]);
end;
end.
