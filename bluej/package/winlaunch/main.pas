unit main;

interface

{$I-}
uses
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, StdCtrls, FindFile, ComCtrls, ConsoleApp, ExFile, REgistry,
  Buttons, ImgList, ExtCtrls, jpeg, StrUtils;

const
    bluejdefsproperty : string = 'bluej.windows.vm=';

    jdkregkey : string = '\Software\JavaSoft\Java Development Kit';
	ibmregkey : string = '\Software\IBM\Java Development Kit';
    bluejregkey : string = '\Software\BlueJ\BlueJ\1.3.5';

    searchingstartcaption : string = 'Search drives for all Java versions...';
    searchingstopcaption : string = 'Stop Search';

    foundjavacaption1 : string = 'BlueJ has found more than one Java version that can be used.';
    foundjavacaption2 : string = 'Please select one and select "Launch" to use it with BlueJ or';
    foundjavacaption3 : string = 'select "Advanced" if you wish to search for other Java versions';

    foundonejavacaption1 : string = 'BlueJ has found the following Java version that can be used.';
    foundonejavacaption2 : string = 'Please select "Launch" if you wish to use it with BlueJ or';
    foundonejavacaption3 : string = 'select "Advanced" if you wish to look for other Java versions';

    nojavacaption1 : string = 'BlueJ could not find any Java systems. A JDK/J2SDK must be';
    nojavacaption2 : string = 'installed to run BlueJ. If one is installed on your system,';
    nojavacaption3 : string = 'select "Advanced" and then browse to its installation directory';

    simplecaption : string = 'Simple';
    advancedcaption : string = 'Advanced';

type
  TMainForm = class(TForm)
    LaunchButton: TButton;
    SearchButton: TButton;
    GoodVM: TListView;
    BadVM: TListView;
    Label1: TLabel;
    StartMessage1: TLabel;
    StatusBar: TStatusBar;
    BrowseButton: TButton;
    OpenDialog1: TOpenDialog;
    AdvancedSimpleButton: TBitBtn;
    StartMessage2: TLabel;
    ImageList1: TImageList;
    StartMessage3: TLabel;
    Image1: TImage;
    LanguageComboBox: TComboBox;
    Label2: TLabel;
    procedure SearchButtonClick(Sender: TObject);
    procedure LaunchButtonClick(Sender: TObject);
    procedure BrowseButtonClick(Sender: TObject);
    procedure FormCreate(Sender: TObject);
    procedure GoodVMSelectItem(Sender: TObject; Item: TListItem;
      Selected: Boolean);
    procedure FormDestroy(Sender: TObject);
    procedure FormShow(Sender: TObject);
    procedure GoodVMExit(Sender: TObject);
    procedure AdvancedSimpleButtonClickToAdvanced(Sender: TObject);
    procedure AdvancedSimpleButtonClickToSimple(Sender: TObject);
  private
        ff : TFindFile;
        goodvmsfound : TStringList;
        { a string list with all parameters passed to the program
          that we have not dealt with and consumed ourselves }
        goodparams : TStringList;

        { directory when we start the application - used as the
          current directory for when we launch java }
        startingcurrentdir : string;

        { command line flag to tell bluej to display the VM selector
          no matter what }
        forcedialog : boolean;

        { flag indicating that we have already launched BlueJ, so
          lets not do it again }
        launched : boolean;

        procedure FindFileFound(Sender: TObject; Folder: String;
                                var FileInfo: TSearchRec);

        procedure NewFolder(Sender: TObject; Folder: String;
                                var IgnoreFolder: Boolean);

        procedure SearchDone(Sender: TObject);

        procedure AddGoodVM(jdkpath, ver : string);

        procedure GoodVMCheck;

        function LaunchBlueJ(jdkpath, ver : string) : boolean;

        function ParseBlueJDefs : string;

  public

  end;

var
  MainForm: TMainForm;

  simplewinheight : integer = 10;
  advancedwinheight : integer = 10;

implementation

uses
    javatest;

{$R *.dfm}

{
    The very first function that is called, when the
    form is first created.

    This function checks the parameters the user has
    passed in to see if they definately want to show
    the selection dialog. It also checks if there is
    a VM preference for this user stored in the registry
    and if so, launches with this VM.
}
procedure TMainForm.FormCreate(Sender: TObject);
var
    reg: TRegistry;
    i : integer;
    home, mode, ver : string;
begin
    { initialise our global variables }
	advancedwinheight := ClientHeight;
	simplewinheight := LaunchButton.Height + LaunchButton.Top + 8;

    startingcurrentdir := GetCurrentDir;

    forcedialog := false;
    launched := false;

    { simulate clicking on the button to make the interface
      simple }
	AdvancedSimpleButtonClickToSimple(Sender);

    ff := TFindFile.Create(MainForm);

    goodvmsfound := TStringList.Create;
    goodvmsfound.CaseSensitive := false;
    goodvmsfound.Sorted := true;
    goodvmsfound.Duplicates := dupError;

    { store all paramters passed to the program that are not
      relevant to us in 'goodparams' }
    goodparams := TStringList.Create;
    goodparams.Delimiter := ' ';
    goodparams.QuoteChar := '"';

    for i := 1 to ParamCount do
    begin
        { deal with the parameter /select and /javaw or
          else add it this param the list of parameters passed onto
          the java process }
        if LowerCase(ParamStr(i)) = '/select' then
            forcedialog := true
        else if LowerCase(ParamStr(i)) = '/javaw' then
            // ignore
        else
       	    goodparams.Add(ParamStr(i));
    end;

    { look for a definition of a VM in the bluej.defs file }
    home := ParseBlueJDefs;

    { if we find one and we are not forced to display ourselves,
      launch BlueJ }
    if home <> '' then
    begin
        if testjdkpath(home, ver)  then
        begin
            if (not forcedialog) then
            begin
	            LaunchBlueJ(home, ver);
                Application.Terminate;
                Exit;
            end
            else
            begin
                AddGoodVM(home, ver);
            end;

        end;
    end;


    reg := TRegistry.Create;
    try
        reg.RootKey := HKEY_CURRENT_USER;

        if reg.OpenKey(bluejregkey, false) then
        begin
            home := reg.ReadString('CurrentVM');
            mode := reg.ReadString('UserMode');

            reg.CloseKey;

			// if forcedialog is true then the user has indicated
            // on the command line to always show the vm selection
            // dialog
			if (not forcedialog) and (home <> '') then
            begin
                if testjdkpath(home, ver) then
                begin
	                LaunchBlueJ(home, ver);
                    Application.Terminate;
                end;
			end;
        end;
    finally
        reg.Free;
    end;
end;

{
    This form is called after all the user-interface elements
    have been created.

    This function checks known registry locations for VM's
    and adds them to a ListBox displaying the VM's.
}
procedure TMainForm.FormShow(Sender: TObject);
var
    reg: TRegistry;
    subkeys : TStrings;
    i : integer;
    home, ver : string;
begin
    { we may get here even though we have already decided to
      launch bluej in FormCreate. If we are in the process of
      terminating, do nothing here }
    if Application.Terminated then
        Exit;

    subkeys := TStringList.Create;
    reg := TRegistry.Create;

	{ look for Sun JDK's }
    try
        reg.RootKey := HKEY_LOCAL_MACHINE;
        if reg.OpenKey(jdkregkey, false) then
        begin
            reg.GetKeyNames(subkeys);
            reg.CloseKey;
        end;

        for i := 0 to subkeys.Count-1 do
        begin
            if reg.OpenKeyReadOnly(jdkregkey + '\' + subkeys[i]) then
            begin
                home := reg.ReadString('JavaHome');

                if home <> '' then
                begin
                    if testjdkpath(home, ver)  then
                    begin
                        AddGoodVM(home, ver);
                    end;
                end;
                reg.CloseKey;
            end;
        end;
    finally
        reg.Free;
    end;

    reg := TRegistry.Create;

	{ look for IBM JDK's }
    try
        subkeys.Clear;

        reg.RootKey := HKEY_LOCAL_MACHINE;
        if reg.OpenKey(ibmregkey, false) then
        begin
            reg.GetKeyNames(subkeys);
            reg.CloseKey;
        end;

        for i := 0 to subkeys.Count-1 do
        begin
            if reg.OpenKeyReadOnly(ibmregkey + '\' + subkeys[i]) then
            begin
                home := reg.ReadString('JavaHome');

                if home <> '' then
                begin
                    if testjdkpath(home, ver)  then
                    begin
                        AddGoodVM(home, ver);
                    end;
                end;
                reg.CloseKey;
            end;
        end;
    finally
        reg.Free;
    end;

	{ change opening message depending on how many VM's we find }
    case goodvmsfound.Count of
     0: begin
	        StartMessage1.Caption := nojavacaption1;
      	    StartMessage2.Caption := nojavacaption2;
	        StartMessage3.Caption := nojavacaption3;
        end;
     1:
        begin
	        StartMessage1.Caption := foundonejavacaption1;
        	StartMessage2.Caption := foundonejavacaption2;
	        StartMessage3.Caption := foundonejavacaption3;

            GoodVM.ItemIndex := 0;

            { if not forced to show the selection dialog, and we
              have found only one VM, we mind as well launch }
            if (not forcedialog) then
            begin
	            LaunchBlueJ(GoodVM.Items[0].Caption, GoodVM.Items[0].SubItems[0] );
                Close;
            end;
        end;
    else
        begin
	        StartMessage1.Caption := foundjavacaption1;
        	StartMessage2.Caption := foundjavacaption2;
	        StartMessage3.Caption := foundjavacaption3;
        end;
     end;
end;

function DelphiIsRunning : boolean;
begin
	Result := DebugHook <> 0;
end;

procedure TMainForm.SearchButtonClick(Sender: TObject);

    { a local function that finds the bitmask of all
      logical drives and builds a string representing
      the drive letters for those drives }
    function BuildDriveStrings : string;
    var
        bitmask,i  : integer;
    begin
        bitmask := GetLogicalDrives;

        for i := 0 to 31 do
        begin
            if (bitmask and (1 shl i)) > 0 then
                result := result + Chr(Ord('A') + i) + ':\;';
        end;

    end;

begin
    if ff.Busy then
        ff.Abort
    else
    begin
        // Fills FileFile properties
        ff.Threaded := true;
        // - Name & Location
        ff.Filename := 'java.exe';
        ff.Location := BuildDriveStrings;
        ff.Subfolders := true;

        ff.OnFound := FindFileFound;
        ff.OnNewFolder := NewFolder;
        ff.OnComplete := SearchDone;

        ff.Execute;

        SearchButton.Caption := searchingstopcaption;
    end;
end;

{ Update a message to show user that progress is being made through
  the folders on the computer }
procedure TMainForm.NewFolder(Sender: TObject; Folder: String;
                                var IgnoreFolder: Boolean);
begin
        StatusBar.SimpleText := 'Searching ' + Folder + '...';
        GoodVMCheck;
end;

{ When we find a file called java.exe, check if its a valid VM and
  add it to the list if it is }
procedure TMainForm.FindFileFound(Sender: TObject; Folder: String;
					var FileInfo: TSearchRec);
var
        javapath, reason : string;
begin
    javapath := Folder + '\' + FileInfo.Name;
    reason := '';

    StatusBar.SimpleText := 'Found VM at ' + javapath;

    if testjavapath(javapath, reason) then
    begin
        AddGoodVM(javapath, reason);
    end
    else
    begin
        with BadVM.Items.Add do
        begin
            Caption := javapath;
            SubItems.Add(reason)
        end;
    end;

    if not ff.Threaded then
        Application.ProcessMessages;
end;

procedure TMainForm.AddGoodVM(jdkpath, ver : string);
begin
    try
        goodvmsfound.Add(jdkpath);

        with GoodVM.Items.Add do
        begin
            Caption := jdkpath;
            SubItems.Add(ver);
        end;

        GoodVMCheck;

    except on EStringListError do
        ;
    end;
end;

procedure TMainForm.SearchDone(Sender: TObject);
begin
        StatusBar.SimpleText := 'Searching complete';
        SearchButton.Caption := searchingstartcaption;

        GoodVMCheck;
end;

{
    Launch BlueJ using 'jdkpath' as the location of the
    VM to use.
}
function TMainForm.LaunchBlueJ(jdkpath, ver : string) : boolean;
var
        appdir, appdirlib, vmfilename,
          bluejjarfilename  : string;
        exfile : TExFile;
begin
    { lets never launch BlueJ twice }
    if launched then
    begin
        result := false;
        Exit;
    end;

    appdir := ExtractFilePath(Application.ExeName);

	// vmfilename is automatically wrapped in quotes by ExecConsoleApp so
	// there is no need for us to do it
//    if usejavaw then
//    else
    if StrLIComp(PChar(ver), '"1.4', 4) >= 0 then
     	vmfilename := ExcludeTrailingPathDelimiter(jdkpath) + '\bin\javaw.exe'
    else
     	vmfilename := ExcludeTrailingPathDelimiter(jdkpath) + '\bin\java.exe';

    appdirlib := '"' + appdir + 'lib\';

    bluejjarfilename := appdirlib + 'bluej.jar' + '"';

//    if (LanguageComboBox.ItemIndex <> 0) then
//        goodparams.Add('-Dbluej.language=' + LanguageComboBox.Items[LanguageComboBox.ItemIndex]);
        
    exfile := TExFile.Create(MainForm);

    exfile.WaitUntilDone := false;
    exfile.WindowType := wtMinimize;
    exfile.ProcFileName := vmfilename;
    exfile.ProcParameters := '-jar ' + bluejjarfilename + ' ' + goodparams.DelimitedText;
    exfile.ProcCurrentDir := startingcurrentdir;

    result := exfile.Execute;

    launched := true;
end;

{
    Launch the currently selected VM and save the users
    preference for this VM into the registry
}
procedure TMainForm.LaunchButtonClick(Sender: TObject);
var
	reg : TRegistry;
begin
    if GoodVM.Selected = nil then
        Exit;

	LaunchBlueJ(GoodVM.Selected.Caption, GoodVM.Selected.SubItems[0]);

    reg := TRegistry.Create;
    try
        reg.RootKey := HKEY_CURRENT_USER;

        if reg.OpenKey(bluejregkey, true) then
        begin
            reg.WriteString('CurrentVM', GoodVM.Selected.Caption);
        end;
	finally
        reg.Free;
    end;

    Close;
end;

{
    Open a dialog to let the user search for a particular
    java.exe
}
procedure TMainForm.BrowseButtonClick(Sender: TObject);
var
    javapath, reason : string;
begin
    OpenDialog1.Execute;

        javapath := OpenDialog1.FileName;

        if javapath = '' then
        	Exit;

	reason := '';

        if  testjavapath(javapath, reason) then
      	        AddGoodVM(javapath, reason)
        else
	        with BadVM.Items.Add do
                begin
                        Caption := javapath;
                        SubItems.Add(reason)
                end;

        GoodVMCheck;
end;

procedure TMainForm.FormDestroy(Sender: TObject);
begin
    goodvmsfound.Free;

    ff.Abort;
    ff.Free;
end;

procedure TMainForm.GoodVMSelectItem(Sender: TObject; Item: TListItem;
                                        Selected: Boolean);
begin
    LaunchButton.Enabled := selected;
end;

procedure TMainForm.GoodVMExit(Sender: TObject);
begin
    GoodVMCheck;
end;

procedure TMainForm.GoodVMCheck;
begin
    if GoodVM.ItemIndex = -1 then
        LaunchButton.Enabled := false;
end;

procedure TMainForm.AdvancedSimpleButtonClickToAdvanced(Sender: TObject);
begin
    AdvancedSimpleButton.Caption := simplecaption;
    AdvancedSimpleButton.Glyph := nil;
	AdvancedSimpleButton.OnClick := AdvancedSimpleButtonClickToSimple;

    ImageList1.GetBitmap(0, AdvancedSimpleButton.Glyph);

    StatusBar.Visible := True;
    ClientHeight := advancedwinheight;
end;

procedure TMainForm.AdvancedSimpleButtonClickToSimple(Sender: TObject);
begin
    AdvancedSimpleButton.Caption := advancedcaption;
    AdvancedSimpleButton.Glyph := nil;
	AdvancedSimpleButton.OnClick := AdvancedSimpleButtonClickToAdvanced;

    ImageList1.GetBitmap(1, AdvancedSimpleButton.Glyph);

    StatusBar.Visible := False;
 	ClientHeight := simplewinheight;
end;

{


}
function TMainForm.ParseBlueJDefs : string;
var
    f : TextFile;
    defsfile : string;
    matchline : string;
    vmline : string;
    i : integer;
    gotbackslash : boolean;
begin
    ParseBlueJDefs := '';

    defsfile := ExtractFilePath(Application.ExeName) + 'lib\bluej.defs';

    AssignFile(f, defsfile);

    Reset(f);

    while not Eof(f) do
    begin
        Readln(f, matchline);

        matchline := Trim(matchline);
        if AnsiStartsStr(bluejdefsproperty, matchline) then
        begin
            matchline := Copy(matchline, Length(bluejdefsproperty)+1, 999);

            gotbackslash := false;

            for i := 1 to Length(matchline) do
            begin
                if not gotbackslash and (matchline[i] = '\') then
                    gotbackslash := true
                else
                begin
                    vmline := vmline + matchline[i];
                    gotbackslash := false;
                end;
            end;
            ParseBlueJDefs := vmline;
        end;
    end;

    CloseFile(f);
end;

end.
