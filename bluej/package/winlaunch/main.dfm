object MainForm: TMainForm
  Left = 388
  Top = 269
  ActiveControl = GoodVM
  BorderStyle = bsDialog
  Caption = 'BlueJ Launcher'
  ClientHeight = 356
  ClientWidth = 442
  Color = clBtnFace
  Font.Charset = DEFAULT_CHARSET
  Font.Color = clWindowText
  Font.Height = -11
  Font.Name = 'MS Sans Serif'
  Font.Style = []
  Icon.Data = {
    0000010001002020100000000000E80200001600000028000000200000004000
    0000010004000000000080020000000000000000000010000000000000000000
    0000000080000080000000808000800000008000800080800000C0C0C0008080
    80000000FF0000FF000000FFFF00FF000000FF00FF00FFFF0000FFFFFF00FFFF
    FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
    FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
    FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFCFFFFFFFFFFFFFFFFFFFFFFFFFFF
    FFFFBCFFFFFFF8FFFFFFFFFFFFFFFFFFFFFFF6FFFFFFF8667FFFFFFFFFFFFFFF
    FFFFF6FFFFFFFF2622687FFFFFFFFFFFFFFFFBFFFFFFFF86666626687FFFFFFF
    FFFFF6FFFFCFFF76666666666CFFFFFFFFFFF6FFFFC0CFF666666666C6FFFFFF
    FFFFF0FFFFF600B66666666CCFFFFFFFFFFF66FFFFFF600C6666666CFFFFFF00
    000006FFFFFFF400666666CBFFFFFFF000000BFFFFFFFB00C66666C6FFFFFFFF
    D000BF6CCFFFFF0006C6660FFFFFFFFFFFC0F6006DCFFF400C6660FFFFFFFFFF
    FFF0C0CFF400FFC000C6C6FFFFFFFFFFFFFB00FFFF4CFF0000060FFFFFFFFFFF
    FFFFDBFFFFFFFF000000BFFFFFFFFFFFFFFFFCFFBB7BBBC00000FFFFFFFFFFFF
    FFFFFCC8666666660006FFFFFFFFFFFFFFFFFFC066266666240066FFFFFFFFFF
    FFFFFFFF06666666666C04FFFFFFFFFFFFFFFFFFF64C66266624CFFFFFFFFFFF
    FFFFFFFFFFFF6007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
    FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
    FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000
    0000000000000000000000000000000000000000000000000000000000000000
    0000000000000000000000000000000000000000000000000000000000000000
    0000000000000000000000000000000000000000000000000000000000000000
    000000000000000000000000000000000000000000000000000000000000}
  OldCreateOrder = False
  Position = poDesktopCenter
  OnCreate = FormCreate
  OnDestroy = FormDestroy
  OnShow = FormShow
  PixelsPerInch = 96
  TextHeight = 13
  object Label1: TLabel
    Left = 8
    Top = 216
    Width = 101
    Height = 13
    Caption = 'Invalid Java VM'#39's'
    Font.Charset = DEFAULT_CHARSET
    Font.Color = clWindowText
    Font.Height = -11
    Font.Name = 'MS Sans Serif'
    Font.Style = [fsBold]
    ParentFont = False
  end
  object Label2: TLabel
    Left = 8
    Top = 8
    Width = 91
    Height = 13
    Caption = 'Valid Java VM'#39's'
    Font.Charset = DEFAULT_CHARSET
    Font.Color = clWindowText
    Font.Height = -11
    Font.Name = 'MS Sans Serif'
    Font.Style = [fsBold]
    ParentFont = False
  end
  object LaunchButton: TButton
    Left = 304
    Top = 144
    Width = 129
    Height = 25
    Caption = 'Launch BlueJ'
    Default = True
    Enabled = False
    TabOrder = 0
    OnClick = LaunchButtonClick
  end
  object SearchButton: TButton
    Left = 144
    Top = 184
    Width = 129
    Height = 25
    Caption = 'Search drives for a VM...'
    TabOrder = 1
    OnClick = SearchButtonClick
  end
  object GoodVM: TListView
    Left = 8
    Top = 24
    Width = 425
    Height = 113
    Columns = <
      item
        Caption = 'Path'
        MinWidth = 200
        Width = 300
      end
      item
        Caption = 'VM Version'
        MinWidth = 120
        Width = 120
      end>
    ReadOnly = True
    RowSelect = True
    TabOrder = 2
    ViewStyle = vsReport
    OnExit = GoodVMExit
    OnSelectItem = GoodVMSelectItem
  end
  object BadVM: TListView
    Left = 7
    Top = 232
    Width = 426
    Height = 97
    Columns = <
      item
        Caption = 'Path'
        MinWidth = 200
        Width = 200
      end
      item
        Caption = 'Reason'
        MinWidth = 200
        Width = 200
      end>
    SortType = stText
    TabOrder = 3
    ViewStyle = vsReport
  end
  object StatusBar: TStatusBar
    Left = 0
    Top = 335
    Width = 442
    Height = 19
    Align = alNone
    Panels = <>
    SimplePanel = True
  end
  object BrowseButton: TButton
    Left = 8
    Top = 184
    Width = 129
    Height = 25
    Caption = 'Browse for a VM...'
    TabOrder = 5
    OnClick = BrowseButtonClick
  end
  object BitBtn1: TBitBtn
    Left = 8
    Top = 144
    Width = 129
    Height = 25
    Caption = 'Advanced'
    TabOrder = 6
    TabStop = False
    OnClick = BitBtn1Click
    Style = bsNew
  end
  object OpenDialog1: TOpenDialog
    Filter = 'Must be a java executable|java.exe'
    Left = 400
    Top = 192
  end
end
