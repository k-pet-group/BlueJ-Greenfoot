unit consoleapp;
interface
uses
{$IFDEF VER90}
  D2ImageHelp,
{$ENDIF}
  SysUtils,
  Classes,
  Windows;

{$WARN SYMBOL_DEPRECATED OFF}

{30/4/00
This unit demonstrates a GUI application spawning a
console application, and capturing the output of the
console application to display in the GUI application.

Any matters arising, questions, comments etc... contact

Martin Lafferty
martinl@prel.co.uk

Production Robots Engineering Ltd
Box 2290, Wimborne, Dorset, BH21 2YY, England.

Background
----------
This example is based on a similar thing I wrote some years
ago which worked not very well under Win95 and not at all under
Windows NT. If you are one of the many people who wrote to me
asking me about this, I am sorry it has taken me so long to sort
it out. I didn't have a need for it until now, and I have been
busy - you know how it is.

The Win32 SDK has a topic called

"Creating a Child process with redirected input and output". I tried to
use that as a basis for this work but found it very confusing and could
not really get it to do what I wanted. The code presented here is really
based on information from Richter ("Advanced Windows" ISBN 1-57231-548-2)
notably chapters 2 (Kernel Objects) and chapter 3 (Processes)

Running Real-mode Dos applications
----------------------------------
(Windows NT)
If you try to run a real mode dos application under ConsoleTest, it may be
that the application will execute OK but you will not see any output.
In fact, dos applications are not executed directly, but wrapped in a call
to the command processor cmd.exe. If you run the program DosTest.exe you
might notice that the command line passed to CreateProcess is actually
'cmd /cdostest' This seems to work OK if the program uses DOS calls for its
output (int 21). If however, you have a smarty-pants application which does
direct screen writes (for example anything compiled with Turbo Pascal using
the 'Crt' unit) then this will not work: the output from the program will
be lost. I expect this could be gotten around but I am not about to start
trying.

(Windows 98) (I have not tested on Windows95)
I cannot offer much help with this. If you try to run a realmode dos
application with ConsoleTest, the output will be captured OK, but when the
process terminates the pipe between the parent and child processes does not
seem to get broken (as it should when the child closes its output handle).
This means that ReadFile does not return and the program hangs.
I cannot see any reliable way of getting around this. If you are
absolutely desperate you could try checking so see whether the process
has completed (GetExitCodeProcess) at the end of each Read loop, and
breaking from the read loop if it has. Unfortunately, the time delay
between the last output of the child process and the process terminating
is likely to be significant, and you will still get caught. You could
put a delay in here, but the whole business becomes too horrible to
contemplate. Personally I don't run Windows 95 and I don't run MS-DOS
applications so I don't intend to spend much time on this. If you come
up with an answer, let me know. Don't forget that this problem only
applies to real-mode DOS applications: Win32 console apps are fine.

Don't bother trying to run the program via the Win98 command shell
(command.com). It doesn't work.

For now, any attempt to run an MSDOS application under 95/98 will fail. If
you try and circumvent this limitation by using a batch file, then the
consequences are down to you. I can tell you now: it won't work (your program
will hang).


Possible Bug
------------
Here is an interesting thing that might be bug (but I don't think so)

Try this on NT:
Open TestApp.dpr (simple console app, supplied) and compile
Open ConsoleTest.dpr in the Delphi IDE
Enter TestApp as command line.
You should get an output - testapp should return 0.

Now without closing down Delphi close ConsoleTest.dpr and reopen TestApp.dpr.
Try to compile and you will get a 'Cannot create output file' error - which
normally indicates that the EXE image is still loaded, but if you check the
process list using the NT Task manager there is no sign of Testapp.exe.

If you close Delphi, and restart it, you can compile OK.


It would be reasonable to assume that a bug in ConsoleTest.dpr was failing to
allow TestApp to terminate properly. I have looked for such a bug, and cannot
find anything. If you run ConsoleTest direct from NT (not in the IDE) then the
problem is not present. You can compile TestApp.dpr quite happily in the IDE
after running the EXE via ConsoleTest running outside the IDE. I am not too
sure what is going on here but it seems to be only a problem when TestApp is
running as a grandchild of Delphi. If you find out more, let me know.


Defines
-------
DEBUG
This checks the number of read loops and also reports the execution time
of the child process

5/08/00
Fixed problem with w98 - I was using a function (GetBinaryType) which
is not supported in Win98.

Also, I was throwing out all NE format files as not suitable, thus
refusing to run 16 bit DPMI apps which may have been OK. I now
delve into the NE header a bit to find out whether the program is
DPMI or Windows.

I should make the point that it is not necessary to call
GetExecutableInfo if you are prepared to do without the protection
it offers

I also check that the file is not a DLL, although these are generally
linked as GUI anyway.

12/09/00
Fixed bug whereby attempting to read new exe header on dos program raises
IO exception. (Phil Scadden (DSIR) noticed this)

It may be that the code that calls ExecConsoleApp may want information
about it, or to terminate it. I have therefore decided to pass the process
handle out with OnNewLine, as well as the new line. The AppOutput parameter
is now optional, as clients may wish to entirely handle the process with
OnNewLine
}

type
  TConsoleEvent = procedure(Process: THandle; const OutputLine: String) of object;

function ExecConsoleApp(const ApplicationName,
                        Parameters: String;
                        AppOutput: TStrings;     {will receive output of child process}
                        OnNewLine: TConsoleEvent  {if assigned called on each new line}
                        ): DWORD;
{Parameters
  ApplicationName.
  This is passed seperate from Parameters so that the the filetype can be checked
  to ensure it is a suitable application. Under Windows NT you may pass a Win32
  console app, or a DOS app. The function assumes an extension of EXE if one is
  not present and does not search for other extensions. If you want to run a COM
  file then you will have to pass the application name with the extension on.
  ApplicationName does not need to be a full path - the function will round up
  the usual suspects - CurrentDir, SystemDir, SearchPath etc.. See API help for
  SearchPath function.

  If you want to run a BAT or CMD you will have to pass the extension.
  ExecConsoleApp does NOT attempt to verify the contents of these files - it
  will just pass them to CreateProcess verbatim. When imbedded in a batch file
  MS-DOS programs seem to work correctly (NT ONLY! don't try this under W9x).
  GUI apps in Batch files will just do their thing normally but they
  will block ExecConsoleApp from returning until closed. You probably don't
  want to do this.

  Don't try and pass .lnk or .pif files to the function.
  Pifs will be rejected because I just don't like them. (only kidding - I can't
  get them to work properly) and .lnks will be rejected because I would need to
  use the shell to resolve them and I figure you can do that for yourself.

  The Application name is always wrapped in double quotes before being passed on
  to the system so you don't have to do this.

  Parameters.
  These are passed on the command line to your child process.

  AppOutput.
  This is an initialised TStrings variable which will receive the output from
  the child process.
  Note that ExecConsoleApp approximately simulates the behaviour of a real
  console in that if the output has a CR  without a linefeeds, the output
  will all be continued over the top of the last line. This is only
  a rough simulation (LF without CR are ignored, for example) but seems OK
  for most processes. I don't attempt to simulate output tricks like
  backspacing on a line, nor do I attempt to use an OEM character set.
  (this parameter now optional 12/09/00)

  OnNewLine
  The function ExecConsoleApp does not return until the child process has
  finished executing. You may want to update your display while the process
  is running however, so each time there is a newline in the output stream
  this event is called. You might use it to do an update on AppOutput, so the
  user can see the application running.


NOTE CAREFULLY
  In the case of ExecConsoleApp being unable to run the application it will raise
  an exception of type EInOutError. EInOutError is not really that relevant, I know,
  but I get fed up with creating new exception classes for every little thing, and
  it is convenient to me to use this one. (It will be raised if there is an IO error
  when reading the exe header.

BUG
  ExecConsoleApp will always fail if the application being spawned is already
  running. This is not good, but I doubt that it will be a problem very often
  in practice, as the sort of apps being run are not interactive.
  If this is a problem to you write to me and I might fix it.

}

{this function is used by ExecConsoleApp but might be useful for other purposes}
procedure GetExecutableInfo( const Filename: String; var BinaryType, Subsystem: DWORD);
{Binary Type may return:

  The following constants are defined by Windows for the GetBinaryType function
  which doesn't work very well under NT and doesn't work at all under w98/w95

  SCS_32BIT_BINARY = 0;  A Win32-based application
  SCS_DOS_BINARY = 1;    An MS-DOS - based application
  SCS_WOW_BINARY = 2;    A 16-bit Windows-based application
  SCS_PIF_BINARY = 3;    A PIF file that executes an MS-DOS - based application
  SCS_POSIX_BINARY = 4;  A POSIX - based application
  SCS_OS216_BINARY = 5;  A 16-bit OS/2-based application (NE, not LE (mgl))

  I need some more}
const
  SCS_VXD_BINARY = 6;  {linear executable. Could be OS/2. NT thinks DOS!}
  SCS_WIN32_DLL = 7;
  SCS_DPMI_BINARY = 8; {guessing a bit here. Based on NE header loader flags}

{ Subsystem May return:

  IMAGE_SUBSYSTEM_UNKNOWN = 0;      Unknown subsystem
  IMAGE_SUBSYSTEM_NATIVE = 1;       Image doesn't require a subsystem. Probably
                                     a kernel mode device driver
  IMAGE_SUBSYSTEM_WINDOWS_GUI = 2;  Image runs in the Windows GUI subsystem.
  IMAGE_SUBSYSTEM_WINDOWS_CUI = 3;  Image runs in the Windows character subsystem.
  IMAGE_SUBSYSTEM_OS2_CUI = 5;      Image runs in the OS/2 character subsystem.
  IMAGE_SUBSYSTEM_POSIX_CUI = 7;    Image runs in the Posix character subsystem.
  IMAGE_SUBSYSTEM_RESERVED8 = 8;    Image runs in the 8 subsystem.

}


implementation


procedure GetExecutableInfo( const Filename: String; var BinaryType, Subsystem: DWORD);
var
  f: File;
  ImageDosHeader: IMAGE_DOS_HEADER;
  ImageFileHeader: IMAGE_FILE_HEADER;
  ImageOptionalHeader: IMAGE_OPTIONAL_HEADER;
  Signature: DWORD;
  NEType: Byte;

begin
  AssignFile(f, Filename);
  Reset(f, 1); {note that this will fail if file is open. this is a bug really,
                but not a big one. Use Api File calls to work around}
  try
    BlockRead(f, ImageDosHeader, Sizeof(ImageDosHeader));
    if (ImageDosHeader.e_magic <> IMAGE_DOS_SIGNATURE) then {not executable}
      raise EInOutError.Create('Dos signature not present');
    try   {16 bit dos program might not have new header}
      Seek(f, ImageDosHeader._lfanew);
      BlockRead(f, Signature, SizeOf(Signature));
      Signature:= Signature and $FFFF;
    except
      on EInOutError do
        Signature:= 0
    end;
    case Signature of
      IMAGE_OS2_SIGNATURE: {New Executable}
      begin
        Seek(f, FilePos(f) + $32); {loader flags are $36 bytes into NE header, but we
                                    have already read 4 bytes for PE signature}
        BlockRead(f, NEType, SizeOf(NEType));
        case NEType of
          1: BinaryType:= SCS_DPMI_BINARY;  {guessing a bit here}
          2: BinaryType:= SCS_WOW_BINARY;
        else
          BinaryType:= SCS_OS216_BINARY; {presumably. I don't have one to check the loader flags!}
        end
      end;
      IMAGE_OS2_SIGNATURE_LE: BinaryType:= SCS_VXD_BINARY;
      IMAGE_NT_SIGNATURE: BinaryType:= SCS_32BIT_BINARY;
    else
      BinaryType:= SCS_DOS_BINARY;
    end;
    Subsystem:= IMAGE_SUBSYSTEM_UNKNOWN;
    if (BinaryType = SCS_32BIT_BINARY)then
    begin
      BlockRead(f, ImageFileHeader, SizeOf(ImageFileHeader));
      if (ImageFileHeader.Characteristics and IMAGE_FILE_EXECUTABLE_IMAGE) = 0 then
        raise EInOutError.Create('File is not executable');  {could be COFF obj}
      if (ImageFileHeader.Characteristics and IMAGE_FILE_DLL) = IMAGE_FILE_DLL then
      begin
        BinaryType:= SCS_WIN32_DLL
      end else
      begin
        BlockRead(f, ImageOptionalHeader, SizeOf(ImageOptionalHeader));
        Subsystem:= ImageOptionalHeader.Subsystem
      end
    end
  finally
    CloseFile(f)
  end
end;

function ExecConsoleApp(const ApplicationName, Parameters: String;
                        AppOutput: TStrings;     {will receive output of child process}
                        OnNewLine: TConsoleEvent  {if assigned called on each new line}
                        ): DWORD;

{we assume that child process requires no input. I have not thought about the
possible consequences of this assumption. I expect we could come up with some
sort of tricky console IO thingy - but we would need to either run an auxilliary
thread or process windows messages somewhere.

This function returns exit code of child process (normally 0 for no error)

If the function returns STILL_ACTIVE ($00000103) then the ReadLoop
has terminated before the app has finished executing. See comments in body
of function
}

const
  CR = #$0D;
  LF = #$0A;
  TerminationWaitTime = 5000;
  ExeExt = '.EXE';
  ComExt = '.COM'; {the original dot com}

var
  StartupInfo:TStartupInfo;
  ProcessInfo:TProcessInformation;
  SecurityAttributes: TSecurityAttributes;

  TempHandle,
  WriteHandle,
  ReadHandle: THandle;
  ReadBuf: array[0..$100] of Char;
{$IFDEF VER90}
  BytesRead: Integer;
{$ELSE}
  BytesRead: Cardinal;
{$ENDIF}
  LineBuf: array[0..$100] of Char;
  LineBufPtr: Integer;
  Newline: Boolean;
  i: Integer;
//  BinType, SubSyst: DWORD;

  Ext, CommandLine: String;
  AppNameBuf: array[0..MAX_PATH] of Char;
  ExeName: PChar;

{$IFDEF DEBUG}
  ReadCount: Integer;
  StartExec,
  EndExec,
  PerfFreq: Int64;
{$ENDIF}

procedure OutputLine;
begin
  LineBuf[LineBufPtr]:= #0;
  if Assigned(AppOutput) then
  with AppOutput do
  begin
    if Newline then
      Add(LineBuf)
    else
      Strings[Count-1]:= LineBuf  {should never happen with count = 0}
  end;
  Newline:= false;
  LineBufPtr:= 0;
  if Assigned(OnNewLine) then
    OnNewLine(ProcessInfo.hProcess, LineBuf)
end;

begin
  {Find out about app}
  Ext:= UpperCase(ExtractFileExt(ApplicationName));
(*
   MODIFIED BY AJP
   To aviod calling GetExecutableInfo which often ends up with a sharing violation
   we are assuming we will always be executing executables

  if (Ext = '.BAT') or ((Win32Platform = VER_PLATFORM_WIN32_NT) and (Ext = '.CMD')) then
  begin {just have a bash}
    FmtStr(CommandLine, '"%s" %s', [ApplicationName, Parameters])
  end else
  if (Ext = '') or (Ext = ExeExt) or (Ext = ComExt) then  {locate and test the application}
  begin
    if SearchPath(nil, PChar(ApplicationName), ExeExt, SizeOf(AppNameBuf), AppNameBuf, ExeName) = 0 then
      raise EInOutError.CreateFmt('Could not find file %s', [ApplicationName]);
    if Ext = ComExt then
      BinType:= SCS_DOS_BINARY
      {in fact, there is no way of telling, but we will just try to run the program. NT is
      equally ignorant and will blindly run anything with a .COM extension}
    else
      GetExecutableInfo(AppNameBuf, BinType, SubSyst);
    if ((BinType = SCS_DOS_BINARY) or (BinType = SCS_DPMI_BINARY)) and
        (Win32Platform = VER_PLATFORM_WIN32_NT) then
      FmtStr(CommandLine, 'cmd /c""%s" %s"', [AppNameBuf, Parameters])
    else
    if (BinType = SCS_32BIT_BINARY) and (SubSyst = IMAGE_SUBSYSTEM_WINDOWS_CUI) then
      FmtStr(CommandLine, '"%s" %s', [AppNameBuf, Parameters])
    else
      raise EInOutError.Create('Executable image is not a supported type')
            {Supported types are Win32 Console or MSDOS under Windows NT only}
  end else
  begin
    raise EInOutError.CreateFmt('%s has invalid file extension', [ApplicationName])
  end;
*)
  if SearchPath(nil, PChar(ApplicationName), ExeExt, SizeOf(AppNameBuf), AppNameBuf, ExeName) = 0 then
      raise EInOutError.CreateFmt('Could not find file %s', [ApplicationName]);

  FmtStr(CommandLine, '"%s" %s', [AppNameBuf, Parameters]);

  FillChar(StartupInfo,SizeOf(StartupInfo), 0);
  FillChar(ReadBuf, SizeOf(ReadBuf), 0);
  FillChar(SecurityAttributes, SizeOf(SecurityAttributes), 0);
{$IFDEF DEBUG}
  ReadCount:= 0;
  if QueryPerformanceFrequency(PerfFreq) then
    QueryPerformanceCounter(StartExec);
{$ENDIF}
  LineBufPtr:= 0;
  Newline:= true;
  with SecurityAttributes do
  begin
    nLength:= Sizeof(SecurityAttributes);
    bInheritHandle:= true
  end;
  if not CreatePipe(ReadHandle, WriteHandle, @SecurityAttributes, 0) then
    RaiseLastWin32Error;
  {create a pipe to act as StdOut for the child. The write end will need
   to be inherited by the child process}
  try
    {Read end should not be inherited by child process}
    if Win32Platform = VER_PLATFORM_WIN32_NT then
    begin
      if not SetHandleInformation(ReadHandle, HANDLE_FLAG_INHERIT, 0) then
        RaiseLastWin32Error
    end else
    begin
      {SetHandleInformation does not work under Window95, so we
      have to make a copy then close the original}
      if not DuplicateHandle(GetCurrentProcess, ReadHandle,
        GetCurrentProcess, @TempHandle, 0, True, DUPLICATE_SAME_ACCESS) then
        RaiseLastWin32Error;
      CloseHandle(ReadHandle);
      ReadHandle:= TempHandle
    end;

    with StartupInfo do
    begin
      cb:= SizeOf(StartupInfo);
      dwFlags:= STARTF_USESTDHANDLES or STARTF_USESHOWWINDOW;
      wShowWindow:= SW_HIDE;
      hStdError:= WriteHandle;
      hStdOutput:= WriteHandle
    end;
    {StartupInfo provides additional parameters to CreateProcess.
    I suspect that it is only safe to pass WriteHandle as hStdOutput
    because we are going to make sure that the child inherits it.
    This is not documented anywhere, but I am reasonably sure it is
    correct. It is (mildly) interesting to note that the example
    given in Win32.hlp "Creating a Child process with redirected
    input and output" does not set the 'StdHandle' fields of StartupInfo.
    Instead the parent process sets its own StdInput and StdOutput
    handles prior to creating the child process - Apparently, the child
    process will then use these values. It all seems a bit odd to me,
    given that a much simpler mechanism (Handle fields of StartupInfo)
    seems to have been provided. Anyway, this alternative approach does
    not seem to work when the parent process is GUI-based. Perhaps Windows
    ignores SetStdHandle for a GUI app.

    We should not have to use STARTF_USESHOWWINDOW and
    wShowWindow:= SW_HIDE as we are going to tell CreateProcess not to
    bother with an output window, but it would appear that Windows 95
    ignores the CREATE_NO_WINDOW flag. Fair enough - it is not in the SDK
    documentation (I got it out of Richter). CREATE_NO_WINDOW actually makes
    virtually no difference to the execution time of my 'hello world' test
    program, but it seems the correct thing to do.

    I shouldn't bother with the DETACHED_PROCESS flag. I suspect that it is
    only relevant when the calling process is a console app.
    }

    if not CreateProcess(nil, PChar(CommandLine),
       nil, nil,
       true,                   {inherit kernel object handles from parent}
       CREATE_NO_WINDOW,
       nil,
       nil,
       StartupInfo,
       ProcessInfo) then
     RaiseLastWin32Error;

    CloseHandle(ProcessInfo.hThread);
    {not interested in threadhandle - close it}

    CloseHandle(WriteHandle);
    {close our copy of Write handle - Child has its own copy now. It is important
    to close ours, otherwise ReadFile may not return when child closes its
    StdOutput - this is the mechanism by which the following loop detects the
    termination of the child process: it does not poll GetExitCodeProcess.

    The clue to this behaviour is in the 'Anonymous Pipes' topic of Win32.hlp - quote

    "To read from the pipe, a process uses the read handle in a call to the
    ReadFile function. When a write operation of any number of bytes completes,
    the ReadFile call returns. The ReadFile call also returns when all handles
    to the write end of the pipe have been closed or if any errors occur before
    the read operation completes normally."

    On this basis (and going somewhat beyond that stated above) I have assumed that
    ReadFile will return TRUE when a write is completed at the other end of the pipe
    and will return FALSE when the write handle is closed at the other end.

    I have also assumed that ReadFile will return when its output buffer is full
    regardless of the size of the write at the other end.

    I have tested all these assumptions as best I can (under NT 4)}

    try
      while ReadFile(ReadHandle, ReadBuf, SizeOf(ReadBuf), BytesRead, nil) do
      begin
        {There are much more efficient ways of doing this: we don't really
        need two buffers, but we do need to scan for CR & LF &&&}
{$IFDEF Debug}
        Inc(ReadCount);
{$ENDIF}
        for  i:= 0 to BytesRead - 1 do
        begin
          if (ReadBuf[i] = LF) then
          begin
            Newline:= true;
            OutputLine
          end else
          if (ReadBuf[i] = CR) then
          begin
            OutputLine
          end else
          begin
            LineBuf[LineBufPtr]:= ReadBuf[i];
            Inc(LineBufPtr);
            if LineBufPtr >= (SizeOf(LineBuf) - 1) then {line too long - force a break}
            begin
              Newline:= true;
              OutputLine
            end
          end
        end
      end;
      WaitForSingleObject(ProcessInfo.hProcess, TerminationWaitTime);
      {The child process may have closed its StdOutput handle but not yet
      terminated, so will wait for up to five seconds to give it a chance to
      terminate. If it has not done so after this time, then we will end
      up returning STILL_ACTIVE ($103)

      If you don't care about the exit code of the process, then you don't
      need this wait: having said that, unless the child process has a
      particularly longwinded cleanup routine, the wait will be very short
      in any event.
      I recommend you leave this wait in place unless you have an intimate
      understanding of the child process you are spawining and are sure you
      don't want to wait for it}

      GetExitCodeProcess(ProcessInfo.hProcess, Result);
      OutputLine {flush the line buffer}

{$IFDEF DEBUG} ;  {that's how much I dislike null statements!
                   Is there a nobel prize for pedantry?}
      if (PerfFreq > 0) and Assigned(AppOutput) then
      begin
        QueryPerformanceCounter(EndExec);
        AppOutput.Add(Format('Debug: (readcount = %d), ExecTime = %.3f ms',
            [ReadCount, ((EndExec - StartExec)*1000.0)/PerfFreq]))
      end else
      begin
        AppOutput.Add(Format('Debug: (readcount = %d)', [ReadCount]))
      end
{$ENDIF}
    finally
      CloseHandle(ProcessInfo.hProcess)
    end
  finally
    CloseHandle(ReadHandle)
  end
end;


end.
