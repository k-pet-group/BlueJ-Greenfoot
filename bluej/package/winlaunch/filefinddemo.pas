unit Main32;

interface

uses
  Windows, Messages, SysUtils, Classes, Graphics, Controls, Forms, Dialogs,
  ComCtrls, StdCtrls, ExtCtrls, Spin, FindFile;

type
  TMainForm = class(TForm)
    FindButton: TButton;
    StopButton: TButton;
    FindFile: TFindFile;
    Animate: TAnimate;
    FoundFiles: TListView;
    StatusBar: TStatusBar;
    Threaded: TCheckBox;
    PageControl: TPageControl;
    TabSheet1: TTabSheet;
    Label1: TLabel;
    Label2: TLabel;
    Filename: TEdit;
    Location: TEdit;
    Subfolders: TCheckBox;
    BrowseButton: TButton;
    TabSheet2: TTabSheet;
    Attributes: TGroupBox;
    System: TCheckBox;
    Hidden: TCheckBox;
    Readonly: TCheckBox;
    Archive: TCheckBox;
    Directory: TCheckBox;
    ExactAttr: TCheckBox;
    TabSheet3: TTabSheet;
    BeforeDate: TDateTimePicker;
    DateRangeChoice: TRadioGroup;
    AfterDate: TDateTimePicker;
    Label3: TLabel;
    Containing: TEdit;
    BeforeTime: TDateTimePicker;
    AfterTime: TDateTimePicker;
    FileSize: TGroupBox;
    SizeMax: TSpinEdit;
    Label8: TLabel;
    SizeMin: TSpinEdit;
    Label9: TLabel;
    Label10: TLabel;
    Label11: TLabel;
    BD: TCheckBox;
    BT: TCheckBox;
    AD: TCheckBox;
    AT: TCheckBox;
    IgnoreCase: TCheckBox;
    procedure FindButtonClick(Sender: TObject);
    procedure StopButtonClick(Sender: TObject);
    procedure FindFileNewFolder(Sender: TObject; Folder: String;
      var IgnoreFolder: Boolean);
    procedure FindFileFound(Sender: TObject; Folder: String;
      var FileInfo: TSearchRec);
    procedure BrowseButtonClick(Sender: TObject);
    procedure FoundFilesColumnClick(Sender: TObject; Column: TListColumn);
    procedure FoundFilesCompare(Sender: TObject; Item1, Item2: TListItem;
      Data: Integer; var Compare: Integer);
    procedure FindFileComplete(Sender: TObject);
    procedure FoundFilesDblClick(Sender: TObject);
    procedure FormCreate(Sender: TObject);
    procedure BDClick(Sender: TObject);
    procedure BTClick(Sender: TObject);
    procedure ADClick(Sender: TObject);
    procedure ATClick(Sender: TObject);
  private
    Folders: Integer;
    StartTime: DWord;
    SortedColumn: Integer;
    Descending: Boolean;
  end;

var
  MainForm: TMainForm;

implementation

{$R *.DFM}

uses
  FileCtrl, ShellAPI;

procedure TMainForm.FindButtonClick(Sender: TObject);
begin
  Folders := 0;
  StartTime := GetTickCount;
  // Fills FileFile properties
  FindFile.Threaded := Threaded.Checked;
  // - Name & Location
  FindFile.Filename := Filename.Text;
  FindFile.Location := Location.Text;
  FindFile.Subfolders := Subfolders.Checked;
  // - Containing Text
  FindFile.Containing := Containing.Text;
  FindFile.IgnoreCase := IgnoreCase.Checked;
  // - Attributes
  FindFile.Attributes := [];
  if Archive.Checked then
    FindFile.Attributes := FindFile.Attributes + [ffArchive];
  if Readonly.Checked then
    FindFile.Attributes := FindFile.Attributes + [ffReadonly];
  if Hidden.Checked then
    FindFile.Attributes := FindFile.Attributes + [ffHidden];
  if System.Checked then
    FindFile.Attributes := FindFile.Attributes + [ffSystem];
  if Directory.Checked then
    FindFile.Attributes := FindFile.Attributes + [ffDirectory];
  FindFile.ExactAttribute := ExactAttr.Checked;
  // - Size ranges
  FindFile.SizeMin := SizeMin.Value * 1024; // KB -> byte
  FindFile.SizeMax := SizeMax.Value * 1024; // KB -> byte
  // - Date ranges
  FindFile.AccessedBefore := 0;
  FindFile.AccessedAfter := 0;
  FindFile.ModifiedBefore := 0;
  FindFile.ModifiedAfter := 0;
  FindFile.CreatedBefore := 0;
  FindFile.CreatedAfter := 0;
  case DateRangeChoice.ItemIndex of
    0: begin // Created on
         if BD.Checked then
           FindFile.CreatedBefore := BeforeDate.Date;
         if BT.Checked then
           FindFile.CreatedBefore := FindFile.CreatedBefore + BeforeTime.Time;
         if AD.Checked then
           FindFile.CreatedAfter := AfterDate.Date;
         if AT.Checked then
           FindFile.CreatedAfter := FindFile.CreatedAfter + AfterTime.Time;
       end;
    1: begin // Modified on
         if BD.Checked then
           FindFile.ModifiedBefore := BeforeDate.Date;
         if BT.Checked then
           FindFile.ModifiedBefore := FindFile.ModifiedBefore + BeforeTime.Time;
         if AD.Checked then
           FindFile.ModifiedAfter := AfterDate.Date;
         if AT.Checked then
           FindFile.ModifiedAfter := FindFile.ModifiedAfter + AfterTime.Time;
       end;
    2: begin // Last Accessed on
         if BD.Checked then
           FindFile.AccessedBefore := BeforeDate.Date;
         if BT.Checked then
           FindFile.AccessedBefore := FindFile.AccessedBefore + BeforeTime.Time;
         if AD.Checked then
           FindFile.AccessedAfter := AfterDate.Date;
         if AT.Checked then
           FindFile.AccessedAfter := FindFile.AccessedAfter + AfterTime.Time;
       end;
  end;
  // Updates visual controls
  SortedColumn := -1;
  FoundFiles.SortType := stNone;
  FoundFiles.Items.BeginUpdate;
  FoundFiles.Items.Clear;
  FoundFiles.Items.EndUpdate;
  FindButton.Enabled := False;
  StopButton.Enabled := True;
  Animate.Active := True;
  // Begins search
  FindFile.Execute;
end;

procedure TMainForm.StopButtonClick(Sender: TObject);
begin
  FindFile.Abort;
end;

procedure TMainForm.FindFileComplete(Sender: TObject);
begin
  Animate.Active := False;
  StopButton.Enabled := False;
  FindButton.Enabled := True;
  StatusBar.SimpleText := Format('%d files found in %d folders (%d ms)',
    [FoundFiles.Items.Count, Folders, GetTickCount - StartTime]);
  if FindFile.Aborted then
    StatusBar.SimpleText := 'Search aborted - ' + StatusBar.SimpleText;
end;

procedure TMainForm.FindFileNewFolder(Sender: TObject; Folder: String;
  var IgnoreFolder: Boolean);
begin
  Inc(Folders);
  StatusBar.SimpleText := Folder;
  if not FindFile.Threaded then
    Application.ProcessMessages;
end;

procedure TMainForm.FindFileFound(Sender: TObject; Folder: String;
  var FileInfo: TSearchRec);
begin
  with FoundFiles.Items.Add do
  begin
    Caption := FileInfo.Name;
    SubItems.Add(Folder);
    if (FileInfo.Attr and faDirectory) <> 0 then
      SubItems.Add('Folder')
    else
      SubItems.Add(IntToStr((FileInfo.Size + 1023) div 1024) + 'KB');
    SubItems.Add(DateTimeToStr(FileDateToDateTime(FileInfo.Time)));
  end;
  if not FindFile.Threaded then
    Application.ProcessMessages;
end;

procedure TMainForm.BrowseButtonClick(Sender: TObject);
var
  Dir: String;
begin
  if Pos(';', Location.Text) = 0 then
    Dir := Location.Text;
  if SelectDirectory(Dir, [], 0) then
    Location.Text := Dir;
end;

procedure TMainForm.FoundFilesColumnClick(Sender: TObject; Column: TListColumn);
begin
  TListView(Sender).SortType := stNone;
  if Column.Index <> SortedColumn then
  begin
    SortedColumn := Column.Index;
    Descending := False;
  end
  else
    Descending := not Descending;
  TListView(Sender).SortType := stText;
end;

procedure TMainForm.FoundFilesCompare(Sender: TObject; Item1,
  Item2: TListItem; Data: Integer; var Compare: Integer);
begin
  if SortedColumn = 0 then
    Compare := CompareText(Item1.Caption, Item2.Caption)
  else if SortedColumn > 0 then
    Compare := CompareText(Item1.SubItems[SortedColumn-1],
                           Item2.SubItems[SortedColumn-1]);
  if Descending then Compare := -Compare;
end;

procedure TMainForm.FoundFilesDblClick(Sender: TObject);
begin
  if FoundFiles.Selected <> nil then
    with FoundFiles.Selected do
      ShellExecute(0, 'Open', PChar(Caption), nil, PChar(SubItems[0]), SW_NORMAL);
end;

procedure TMainForm.FormCreate(Sender: TObject);
begin
  BeforeDate.Date := Date;
  BeforeDate.Time := 0;
  AfterDate.Date := Date;
  AfterDate.Time := 0;
  BeforeTime.Time := Time;
  BeforeTime.Date := 0;
  AfterTime.Time := Time;
  AfterTime.Date := 0;
end;

procedure TMainForm.BDClick(Sender: TObject);
begin
  BeforeDate.Enabled := BD.Checked;
end;

procedure TMainForm.BTClick(Sender: TObject);
begin
  BeforeTime.Enabled := BT.Checked;
end;

procedure TMainForm.ADClick(Sender: TObject);
begin
  AfterDate.Enabled := AD.Checked;
end;

procedure TMainForm.ATClick(Sender: TObject);
begin
  AfterTime.Enabled := AT.Checked;
end;

end.
