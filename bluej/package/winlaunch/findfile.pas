{------------------------------------------------------------------------------}
{                                                                              }
{  TFindFile v2.22                                                             }
{  by Kambiz R. Khojasteh                                                      }
{                                                                              }
{  kambiz@delphiarea.com                                                       }
{  http://www.delphiarea.com                                                   }
{                                                                              }
{------------------------------------------------------------------------------}

unit findfile;

interface

uses
  {$IFDEF WIN32} Windows {$ELSE} WinTypes, WinProcs {$ENDIF}, Messages,
  SysUtils, Classes, Graphics, Controls, Forms, Dialogs;

type

  EFindFileError = class(Exception);

  TFoundEvent = procedure (Sender: TObject; Folder: String;
    var FileInfo: TSearchRec) of object;

  TNewFolderEvent = procedure (Sender: TObject; Folder: String;
    var IgnoreFolder: Boolean) of object;

  TFileAttribute = (ffArchive, ffReadonly, ffHidden, ffSystem, ffDirectory);
  TFileAttributes = set of TFileAttribute;

  {TSearchInfo holds all search parameters. This gives us ability to}
  {change TFinFile properties while it is searching.}
  TSearchInfo = packed record
     IncludeFiles: TStringList;
     ExcludeFiles: TStringList;
     IncSubfolders: Boolean;
     Attr: TFileAttributes;
     ExactAttr: Boolean;
     {$IFDEF WIN32}
     CreatedBefore: TDateTime;
     CreatedAfter: TDateTime;
     AccessedBefore: TDateTime;
     AccessedAfter: TDateTime;
     {$ENDIF}
     ModifiedBefore: TDateTime;
     ModifiedAfter: TDateTime;
     SizeMin: LongInt;
     SizeMax: LongInt;
     Text: PChar;
     IgnoreCase: Boolean;
  end;

  TFindFile = class(TComponent)
  private
    fFilename: String;
    fLocation: String;
    fIncludeFiles: TStringList;
    fExcludeFiles: TStringList;
    fSubfolders: Boolean;
    fModifiedBefore: TDateTime;
    fModifiedAfter: TDateTime;
    fSizeMin: LongInt;
    fSizeMax: LongInt;
    fAttributes: TFileAttributes;
    fExactAttr: Boolean;
    {$IFDEF WIN32}
    fCreatedBefore: TDateTime;
    fCreatedAfter: TDateTime;
    fAccessedBefore: TDateTime;
    fAccessedAfter: TDateTime;
    {$ENDIF}
    fContaining: String;
    fIgnoreCase: Boolean;
    fAborted: Boolean;
    fBusy: Boolean;
    fOnFound: TFoundEvent;
    fOnNewFolder: TNewFolderEvent;
    fOnComplete: TNotifyEvent;
    SI: TSearchInfo;
    {$IFDEF WIN32}
    fThread: TThread;
    fThreaded: Boolean;
    fThreadPriority: TThreadPriority;
    procedure ThreadTerminated(Sender: TObject);
    procedure SetThreadPriority(Value: TThreadPriority);
    {$ENDIF}
    procedure SetFilename(Value: String);
    procedure SetLocation(Value: String);
    procedure SetIncludeFiles(Value: TStringList);
    procedure SetExcludeFiles(Value: TStringList);
  protected
    {$IFDEF WIN32}
    procedure DoThreadedSearch; dynamic;
    {$ENDIF}
    procedure DoSearch; dynamic;
    procedure DoFound(Folder: String; var FileInfo: TSearchRec); virtual;
    function DoNewFolder(const Folder: String): Boolean; virtual;
    procedure DoComplete; virtual;
    function AcceptFile(const Folder: String; const SR: TSearchRec): Boolean;
  public
    constructor Create(AOwner: TComponent); override;
    destructor Destroy; override;
    procedure Execute;
    procedure Abort;
    property Busy: Boolean read fBusy;
    property Aborted: Boolean read fAborted;
  published
    property Filename: String read fFilename write SetFilename;
    property Location: String read fLocation write SetLocation;
    property IncludeFiles: TStringList read fIncludeFiles write SetIncludeFiles;
    property ExcludeFiles: TStringList read fExcludeFiles write SetExcludeFiles;
    property Subfolders: Boolean read fSubfolders write fSubfolders default True;
    property Attributes: TFileAttributes read fAttributes write fAttributes default
      [ffArchive, ffReadonly, ffHidden, ffSystem];
    property ExactAttribute: Boolean read fExactAttr write fExactAttr default False;
    property ModifiedBefore: TDateTime read fModifiedBefore write fModifiedBefore;
    property ModifiedAfter: TDateTime read fModifiedAfter write fModifiedAfter;
    {$IFDEF WIN32}
    property CreatedBefore: TDateTime read fCreatedBefore write fCreatedBefore;
    property CreatedAfter: TDateTime read fCreatedAfter write fCreatedAfter;
    property AccessedBefore: TDateTime read fAccessedBefore write fAccessedBefore;
    property AccessedAfter: TDateTime read fAccessedAfter write fAccessedAfter;
    {$ENDIF}
    property SizeMin: LongInt read fSizeMin write fSizeMin default 0;
    property SizeMax: LongInt read fSizeMax write fSizeMax default 0;
    property Containing: String read fContaining write fContaining;
    property IgnoreCase: Boolean read fIgnoreCase write fIgnoreCase default True;
    {$IFDEF WIN32}
    property Threaded: Boolean read fThreaded write fThreaded default False;
    property ThreadPriority: TThreadPriority read fThreadPriority write
      SetThreadPriority default tpNormal;
    {$ENDIF}
    property OnFound: TFoundEvent read fOnFound write fOnFound;
    property OnNewFolder: TNewFolderEvent read fOnNewFolder write fOnNewFolder;
    property OnComplete: TNotifyEvent read fOnComplete write fOnComplete;
  end;

procedure Register;

function RemoveTrailingBackslash(S: String): String;
function FileMatches(Path, Mask: String): Boolean;

implementation

{$IFDEF WIN32}
  {$R *.D32}
{$ELSE}
  {$R *.D16}
{$ENDIF}

{$WARN SYMBOL_PLATFORM OFF}

uses
  FileCtrl;

const
  BufferSize = 4096;
  PathDelimiter = ';';
  SearchAttr = faAnyFile and not faVolumeID;
  InvalidLocation = '%s' + #10#13 + 'location cannot to be contained wildcards';
  InvalidFilename = '%s' + #10#13 + 'Invalid filename specified';

{$IFDEF WIN32}
type
  TSearchThread = class(TThread)
  private
    Ignored: Boolean;
    Folder: String;
    FoundSR: TSearchRec;
    FindFile: TFindFile;
    procedure NewFolderEntered;
    procedure FileFound;
  protected
    constructor Create(AFindFile: TFindFile);
    procedure Execute; override;
  end;
{$ENDIF}

procedure Register;
begin
  RegisterComponents('Delphi Area', [TFindFile]);
end;

function RemoveTrailingBackslash(S: String): String;
begin
  if (Length(S) > 1) and (S[Length(S)] = '\') and (S[Length(S)-1] <> ':') then
    Result := Copy(S, 1, Length(S)-1)
  else
    Result := S;
end;

function FileMatches(Path, Mask: String): Boolean;
var
  Fi, Mi: Integer;
  FilePath, FileName: String;
  MaskPath, MaskName: String;
begin
  Result := False;
  FilePath := UpperCase(ExtractFilePath(Path));
  FileName := UpperCase(ExtractFileName(Path));
  MaskPath := UpperCase(ExtractFilePath(Mask));
  MaskName := UpperCase(ExtractFileName(Mask));
  {Checkes file path part, if mask contains path}
  if Length(MaskPath) > 0 then
  begin
    {Checkes drive, if mask contains drive}
    if (Length(MaskPath) >= 2) and (MaskPath[2] = ':') then
    begin
      if not (MaskPath[1] in ['*','?']) and (MaskPath[1] <> FilePath[1]) then
        Exit; {Not Matched}
      {Removes drive part from the path}
      MaskPath := Copy(MaskPath, 3, Length(MaskPath)-2);
    end;
    {Checkes directory}                  { |-> excludes drive length}
    if Length(MaskPath) > Length(FilePath)-2 then
      Exit {Not Matched, Mask is latger than file path}
    else
    begin
      {we check inner directories}
      FilePath := Copy(FilePath, Length(FilePath)-Length(MaskPath)+1, Length(MaskPath));
      if FilePath <> MaskPath then
        Exit {Not Matched}
    end;
  end;
  {Checkes file name part, if mask contains filename}
  if Length(MaskName) > 0 then
  begin
    Mi := 1;
    Fi := 1;
    while (Mi <= Length(MaskName)) and (Fi <= Length(FileName)) do
    begin
      if (FileName[Fi] = MaskName[Mi]) or ((FileName[Fi] <> '.') and (MaskName[Mi] = '?')) then
      begin
        Inc(Mi);
        Inc(Fi);
      end
      else if FileName[Fi] = '.' then
        if MaskName[Mi] = '?' then
          Inc(Mi)
        else if MaskName[Mi] = '*' then
          while (Mi <= Length(MaskName)) and (MaskName[Mi] <> '.') do
            Inc(Mi)
        else
          Exit {Not matched}
      else if MaskName[Mi] = '*' then
        Inc(Fi)
      else
        Exit; {Not matched}
    end;
    while (Mi <= Length(MaskName)) and (MaskName[Mi] in ['?', '*']) do
     Inc(Mi);
    if (Mi > Length(MaskName)) and (Fi > Length(FileName)) then
      Result := True; {Matched}
  end
  else
    Result := True; {Matched}
end;

constructor TFindFile.Create(AOwner: TComponent);
begin
  inherited Create(AOwner);
  fAttributes := [ffArchive, ffReadonly, ffHidden, ffSystem];
  fExactAttr := False;
  fFilename := '*.*';
  fIncludeFiles := TStringList.Create;
  fExcludeFiles := TStringList.Create;
  fSubfolders := True;
  fIgnoreCase := True;
  fBusy := False;
  {$IFDEF WIN32}
  fThread := nil;
  fThreaded := False;
  fThreadPriority := tpNormal;
  {$ENDIF}
end;

destructor TFindFile.Destroy;
begin
  {$IFDEF WIN32}
  if Assigned(fThread) then
  begin
    fThread.Terminate;
    fThread.WaitFor;
  end;
  {$ENDIF}
  fIncludeFiles.Free;
  fExcludeFiles.Free;
  inherited Destroy;
end;

procedure TFindFile.SetFilename(Value: String);
begin
  if ExtractFileName(Value) = Value then
    fFilename := Value
  else
    raise EFindFileError.CreateFmt(InvalidFilename, [Value]);
end;

procedure TFindFile.SetLocation(Value: String);
begin
  if (Pos('*', Value) = 0) and (Pos('?', Value) = 0) then
    fLocation := Value
  else
    raise EFindFileError.CreateFmt(InvalidLocation, [Value]);
end;

procedure TFindFile.SetIncludeFiles(Value: TStringList);
begin
  if Assigned(Value) then
    fIncludeFiles.Assign(Value)
  else
    fIncludeFiles.Clear;
end;

procedure TFindFile.SetExcludeFiles(Value: TStringList);
begin
  if Assigned(Value) then
    fExcludeFiles.Assign(Value)
  else
    fExcludeFiles.Clear;
end;

{$IFDEF WIN32}
procedure TFindFile.SetThreadPriority(Value: TThreadPriority);
begin
  fThreadPriority := Value;
  if Assigned(fThread) then
    fThread.Priority := Value;
end;
{$ENDIF}

procedure TFindFile.Execute;

  procedure AddToIncludeList(Loc, Filename: String);
  var
    Path: String;
    P, I: Integer;
    Duplicated: Boolean;
  begin
    if fFilename = '' then
      Filename := '*.*'
    else if Pos('.', Filename) = 0 then
      Filename := Filename + '.*';
    repeat
      P := Pos(PathDelimiter, Loc);
      if P > 0 then
      begin
        Path := Copy(Loc, 1, P-1);
        Delete(Loc, 1, P);
      end
      else
      begin
        Path := Loc;
        Loc := '';
      end;
      if Path = '' then
        Path := ExpandFileName('.')
      else
        Path := ExpandFileName(Path);
      if Path[Length(Path)] <> '\' then
        Path := Path + '\';
      Path := Path + Filename;
      Duplicated := False;
      for I := 0 to SI.IncludeFiles.Count-1 do
        if CompareText(SI.IncludeFiles[I], Path) = 0 then
        begin
          Duplicated := True;
          Break;
        end;
      if not Duplicated then
        SI.IncludeFiles.Add(Path);
    until (Length(Loc) = 0) or (Loc = PathDelimiter);
  end;

begin
  if fBusy then Exit;
  fBusy := True;
  {Sets file attributes}
  SI.Attr := fAttributes;
  SI.ExactAttr := fExactAttr;
  {Sets included files}
  SI.IncludeFiles := TStringList.Create;
  if fIncludeFiles.Count > 0 then
    SI.IncludeFiles.Assign(fIncludeFiles)
  else {Addes filename and location to included files}
    AddToIncludeList(fLocation, fFileName);
  SI.IncSubfolders := fSubfolders;
  {Sets excluded files}
  SI.ExcludeFiles := TStringList.Create;
  SI.ExcludeFiles.Assign(fExcludeFiles);
  {Sets date ranges}
  {$IFDEF WIN32}
  SI.AccessedBefore := fAccessedBefore;
  SI.AccessedAfter := fAccessedAfter;
  SI.CreatedBefore := fCreatedBefore;
  SI.CreatedAfter := fCreatedAfter;
  {$ENDIF}
  SI.ModifiedBefore := fModifiedBefore;
  SI.ModifiedAfter := fModifiedAfter;
  {Sets files size ranges}
  SI.SizeMin := fSizeMin;
  SI.SizeMax := fSizeMax;
  {Sets containing text}
  if fContaining <> '' then
  begin
    SI.IgnoreCase := fIgnoreCase;
    GetMem(SI.Text, Length(fContaining)+1);
    StrPCopy(SI.Text, fContaining);
  end;
  {Starts search}
  fAborted := False;
  {$IFDEF WIN32}
  if fThreaded then
    DoThreadedSearch
  else
  {$ENDIF}
    DoSearch;
end;

procedure TFindFile.Abort;
begin
  fAborted := True;
  {$IFDEF WIN32}
  if Assigned(fThread) then
  begin
    fThread.Terminate;
    repeat
      Application.ProcessMessages
    until not Busy;
  end;
  {$ENDIF}
end;

var
 Buffer: array[0..BufferSize-1] of Char;

function TFindFile.AcceptFile(const Folder: String; const SR: TSearchRec): Boolean;

  function ValidFileAttr: Boolean;
  var
    FileAttr: TFileAttributes;
  begin
    Result := False;
    FileAttr := [];
    if (SR.Attr and faArchive) <> 0 then Include(FileAttr, ffArchive);
    if (SR.Attr and faReadonly) <> 0 then Include(FileAttr, ffReadonly);
    if (SR.Attr and faHidden) <> 0 then Include(FileAttr, ffHidden);
    if (SR.Attr and faSysFile) <> 0 then Include(FileAttr, ffSystem);
    if (SR.Attr and faDirectory) <> 0 then Include(FileAttr, ffDirectory);
    if SI.ExactAttr then
    begin
      if FileAttr = SI.Attr then
        Result := True;
    end
    else if (FileAttr = []) or ((FileAttr * SI.Attr) <> []) then
      Result := True;
  end;

  function DateBetween(aDate, Before, After: TDateTime): Boolean;
  begin
    Result := True;
    if Before <> 0 then
      if Frac(Before) = 0 then      { Checks date only }
        Result := Result and (Int(aDate) <= Before)
      else if Int(Before) = 0 then  { Checks time only }
        Result := Result and (Frac(aDate) <= Before)
      else                          { Checks date and time }
        Result := Result and (aDate <= Before);
    if After <> 0 then
      if Frac(After) = 0 then       { Checks date only }
        Result := Result and (Int(aDate) >= After)
      else if Int(After) = 0 then   { Checks time only }
        Result := Result and (Frac(aDate) >= After)
      else                          { Checks date and time }
        Result := Result and (aDate >= After);
  end;

  function FileContainsText: Boolean;
  var
    Stream: TFileStream;
    I, TextLen, ReadLen: LongInt;
    {$IFDEF WIN32}
    Compare: function(const Str1, Str2: PChar; MaxLen: Cardinal): Integer;
    {$ELSE}
    Compare: function(Str1, Str2: PChar; MaxLen: Word): Integer;
    {$ENDIF}
  begin
    Result := False;
    {$IFDEF WIN32}
    if SI.IgnoreCase then Compare := @StrLIComp else Compare := @StrLComp;
    {$ELSE}
    if SI.IgnoreCase then Compare := StrLIComp else Compare := StrLComp;
    {$ENDIF}
    TextLen := StrLen(SI.Text);
    Stream := TFileStream.Create(Folder + SR.Name, fmOpenRead or fmShareDenyNone);
    try try
      repeat
        ReadLen := Stream.Read(Buffer, BufferSize);
        for I := 0 to ReadLen - TextLen do
          if Compare(SI.Text, @Buffer[I], TextLen) = 0 then
          begin
            Result := True;
            Break;
          end;
      until Stream.Position >= Stream.Size;
    except
      {Ignores exceptions}
    end;
    finally
      Stream.Free;
    end;
  end;

var
  I: Integer;
  TheDate: TDateTime;
  {$IFDEF WIN32}
  SystemTime: TSystemTime;
  FileTime: TFileTime;
  {$ENDIF}
begin
  Result := False;
  {Checkes file attributes}
  if not ValidFileAttr then Exit;
  {Checkes file size ranges}
  if (SR.Attr and faDirectory) = 0 then
  begin
    if (SI.SizeMin <> 0) and (SR.Size < SI.SizeMin) then Exit;
    if (SI.SizeMax <> 0) and (SR.Size > SI.SizeMax) then Exit;
  end;
  {Checkes file date ranges}
  {$IFDEF WIN32}
  if (SI.CreatedBefore <> 0) or (SI.CreatedAfter <> 0) then
  begin
    FileTimeToLocalFileTime(SR.FindData.ftCreationTime, FileTime);
    FileTimeToSystemTime(FileTime, SystemTime);
    TheDate := SystemTimeToDateTime(SystemTime);
    if not DateBetween(TheDate, SI.CreatedBefore, SI.CreatedAfter) then Exit;
  end;
  if (SI.ModifiedBefore <> 0) or (SI.ModifiedAfter <> 0) then
  begin
    FileTimeToLocalFileTime(SR.FindData.ftLastWriteTime, FileTime);
    FileTimeToSystemTime(FileTime, SystemTime);
    TheDate := SystemTimeToDateTime(SystemTime);
    if not DateBetween(TheDate, SI.ModifiedBefore, SI.ModifiedAfter) then Exit;
  end;
  if (SI.AccessedBefore <> 0) or (SI.AccessedAfter <> 0) then
  begin
    FileTimeToLocalFileTime(SR.FindData.ftLastAccessTime, FileTime);
    FileTimeToSystemTime(FileTime, SystemTime);
    TheDate := SystemTimeToDateTime(SystemTime);
    if not DateBetween(TheDate, SI.AccessedBefore, SI.AccessedAfter) then Exit;
  end;
  {$ELSE}
  if (SI.ModifiedBefore <> 0) or (SI.ModifiedAfter <> 0) then
  begin
    TheDate := FileDateToDateTime(SR.Time);
    if not DateBetween(TheDate, SI.ModifiedBefore, SI.ModifiedAfter) then Exit;
  end;
  {$ENDIF}
  {Checkes exclude file list}
  for I := 0 to SI.ExcludeFiles.Count-1 do
    if FileMatches(Folder + SR.Name, SI.ExcludeFiles[I]) then Exit;
  {Checkes containing text}
  if (SI.Text <> nil) and ((SR.Attr and faDirectory) = 0) then
    Result := FileContainsText
  else
    Result := True;
end;

procedure TFindFile.DoFound(Folder: String; var FileInfo: TSearchRec);
begin
  if Assigned(fOnFound) and not (csDestroying in ComponentState) then
    fOnFound(Self, RemoveTrailingBackslash(Folder), FileInfo);
end;

function TFindFile.DoNewFolder(const Folder: String): Boolean;
var
  Ignored: Boolean;
begin
  Ignored := False;
  if Assigned(fOnNewFolder) and not (csDestroying in ComponentState) then
    fOnNewFolder(Self, RemoveTrailingBackslash(Folder), Ignored);
  Result := not Ignored;
end;

procedure TFindFile.DoComplete;
begin
  if SI.IncludeFiles <> nil then
  begin
    SI.IncludeFiles.Free;
    SI.IncludeFiles := nil;
  end;
  if SI.ExcludeFiles <> nil then
  begin
    SI.ExcludeFiles.Free;
    SI.ExcludeFiles := nil;
  end;
  if SI.Text <> nil then
  begin
    FreeMem(SI.Text, StrLen(SI.Text)+1);
    SI.Text := nil;
  end;
  fBusy := False;
  if Assigned(fOnComplete) and not (csDestroying in ComponentState) then
    fOnComplete(Self);
end;

procedure TFindFile.DoSearch;

  procedure SearchIn(Loc: String; var FileMask: String);
  var
    SR: TSearchRec;
    Path: String;
  begin
    if not DoNewFolder(Loc) then Exit;
    Path := Loc + FileMask;
    {Searches current folder}
    if not fAborted and (FindFirst(Path, SearchAttr, SR) = 0) then
      repeat
        if (SR.Name[1] <> '.') and AcceptFile(Loc, SR) then
          DoFound(Loc, SR);
      until Aborted or (FindNext(SR) <> 0);
    FindClose(SR);
    {Scans sub folders}
    if not Aborted and SI.IncSubfolders then
    begin
      Path := Loc + '*.*';
      if not Aborted and (FindFirst(Path, SearchAttr, SR) = 0) then
        repeat
          if ((SR.Attr and faDirectory) <> 0) and (SR.Name[1] <> '.') then
            SearchIn(Loc + SR.Name + '\', FileMask);
        until Aborted or (FindNext(SR) <> 0);
      FindClose(SR);
    end;
  end;

var
  I: Integer;
  Folder, Filename: String;

begin
  try
    try
      for I := 0 to SI.IncludeFiles.Count-1 do
      begin
        Folder := ExpandFileName(ExtractFilePath(SI.IncludeFiles[I]));
        Filename := ExtractFileName(SI.IncludeFiles[I]);
        SearchIn(Folder, Filename);
        if fAborted then Break;
      end;
    except
      fAborted := True;
    end;
  finally
    DoComplete;
  end;
end;

{$IFDEF WIN32}

procedure TFindFile.DoThreadedSearch;
begin
  fThread := TSearchThread.Create(Self);
end;

procedure TFindFile.ThreadTerminated(Sender: TObject);
begin
  fThread := nil;
  DoComplete;
end;

constructor TSearchThread.Create(AFindFile: TFindFile);
begin
  FindFile := AFindFile;
  OnTerminate := FindFile.ThreadTerminated;
  FreeOnTerminate := True;
  inherited Create(False);
end;

procedure TSearchThread.NewFolderEntered;
begin
  Ignored := not FindFile.DoNewFolder(Folder);
end;

procedure TSearchThread.FileFound;
begin
  FindFile.DoFound(Folder, FoundSR);
end;

procedure TSearchThread.Execute;

  procedure SearchIn(Loc: String; var FileMask: String);
  var
    SR: TSearchRec;
    Path: String;
  begin
    Folder := Loc;
    Synchronize(NewFolderEntered);
    if Ignored then Exit;
    Path := Loc + FileMask;
    {Searches current folder}
    if not Terminated and (FindFirst(Path, SearchAttr, SR) = 0) then
      repeat
        if (SR.Name[1] <> '.') and FindFile.AcceptFile(Loc, SR) then
        begin
          Folder := Loc;
          FoundSR := SR;
          Synchronize(FileFound);
        end;
      until Terminated or (FindNext(SR) <> 0);
    FindClose(SR);
    {Scans sub folders}
    if not Terminated and FindFile.SI.IncSubfolders then
    begin
      Path := Loc + '*.*';
      if not Terminated and (FindFirst(Path, SearchAttr, SR) = 0) then
        repeat
          if ((SR.Attr and faDirectory) <> 0) and (SR.Name[1] <> '.') then
            SearchIn(Loc + SR.Name + '\', FileMask);
        until Terminated or (FindNext(SR) <> 0);
      FindClose(SR);
    end;
  end;

var
  I: Integer;
  Folder, Filename: String;

begin
  try
    for I := 0 to FindFile.SI.IncludeFiles.Count-1 do
    begin
      Folder := ExpandFileName(ExtractFilePath(FindFile.SI.IncludeFiles[I]));
      Filename := ExtractFileName(FindFile.SI.IncludeFiles[I]);
      SearchIn(Folder, Filename);
      if FindFile.fAborted then Break;
    end;
  except
    FindFile.fAborted := True;
  end;
end;

{$ENDIF}

end.

