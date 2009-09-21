/* stream for reading from a file, using Windows API */

#include <windows.h>

#include <streambuf>

class WinFileBuf : public std::basic_streambuf<char>
{
	private:
	HANDLE myHandle;
	char myBuf[4096];

	protected:
	traits_type::int_type underflow()
	{
		DWORD bytesRead;
		BOOL rres = ReadFile(myHandle, myBuf, 4096, &bytesRead, NULL);
		if (rres == 0 || bytesRead == 0) {
			// Failure
			return traits_type::eof();
		}
		else {
			setg(myBuf, myBuf, myBuf + bytesRead);
			return myBuf[0];
		}
	}

	public:
	WinFileBuf(HANDLE fileHandle)
	{
		myHandle = fileHandle;
		setg(myBuf, myBuf + 4096, myBuf + 4096);  // first read causes underflow
	}
};
