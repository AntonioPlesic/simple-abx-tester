//TODO: pimplmise










//*****************************************************************************
//*"Private" classes used by Mp3Frame parser, not meant for standalone usage***
//*****************************************************************************

//=============================================================================
// Mp3Octet
//=============================================================================
/* Mp3 file is comprised of some number of mp3 octets. This is actually nothing 
 * else but a wrapper around char used to faciliate easy printf debugging. 
 */

struct Mp3Octet{

	typedef unsigned char octet; //typedef not really needed, used to switch beetween signed/unsigned
	octet c;

	Mp3Octet(octet _c);
	Mp3Octet();

	bool operator==(const Mp3Octet& other);
	bool operator!=(const Mp3Octet& other);

};

std::ostream& operator<<(std::ostream& os, const Mp3Octet& octet);
std::istream& operator>>(std::istream& is, Mp3Octet& octet);


//=============================================================================
// Mp3Header
//=============================================================================
/* Each Mp3 frame starts with this kind of header, which consists of four bytes
 * (represented by Mp3Octets in this case).
 */

struct Mp3Header{

	//octets that comprise the mp3 header
	Mp3Octet first;
	Mp3Octet second;
	Mp3Octet third;
	Mp3Octet fourth;

	//position of first in the byte stream
	std::istream::pos_type position;

	Mp3Header();
	Mp3Header(std::istream::pos_type position, Mp3Octet first, Mp3Octet second, Mp3Octet third, Mp3Octet fourth);

	static const unsigned int MPEG_2_5 = 0;	// MPEG Version 2.5
	static const unsigned int MPEG_RESERVED = 1;	// reserved
	static const unsigned int MPEG_VERSION_2 = 2;	// -MPEG Version 2
	static const unsigned int MPEG_VERSION_1 = 3;	// -MPEG Version 1

	static const unsigned int LAYER_RESERVED = 0;	//00 - reserved
	static const unsigned int LAYER_III = 1;	//01 - Layer III
	static const unsigned int LAYER_II = 2;	//10 - Layer II
	static const unsigned int LAYER_I = 3;	// 11 - Layer I

	std::string getMpegVersionName() const;
	std::string getLayerName() const;
	unsigned int getBitrateValue() const;
	unsigned int getSamplingValue() const;
	long frameSizeNaive() const;
	std::ifstream::pos_type nextFramePosition() const;
	bool validSync();

private:

	unsigned char syncWord[2];	//whole first
								//second & mask,	mask = 0b11100000, 0xE0
	unsigned int
		mpeg,			//second & mask,	mask = 0b00011000, 0x18; >>3
		layer,			//second & mask,	mask = 0b00000110, 0x06; >>1
		protection,		//second & mask,	mask = 0b00000001, 0x01; >>0

		bitrate,		//f(mpegId, layer, third & mask),	mask = 0b11110000, 0xF0; >>4
		sampling,		//f(mpegId, third & mask),			mask = 0b00001100, 0x0C; >>2
		padding,		//third & mask,						mask = 0b00000010, 0x02; >>1
		privateBit,		//third & mask,						mask = 0b00000001, 0x01; >>0

		channelMode,	//fourth & mask,	mask = 0b11000000, 0xC0; >>6
		modeExtension,	//fourth & mask,	mask = 0b00110000, 0x30; >>4
		copyrightBit,	//fourth & mask,	mask = 0b00001000, 0x08; >>3
		originalBit,	//fourth & mask,	mask = 0b00000100, 0x04; >>2
		emphasis		//fourth & mask,	mask = 0b00000011, 0x03; >>0

		= -1; //XXX: sets to max value

	void init();

};

std::ostream& operator<<(std::ostream& os, const Mp3Header& header);


//=============================================================================
// Mp3 frame parser
//=============================================================================
/* Usage: */

#ifdef __cplusplus
extern "C" {
#endif

	typedef struct parser parser;

	parser* new_parser(char* path);
	void delete_parser(parser* d);
	void parser_alignToFirst(parser* d);
	int parser_getNextFrame(parser* d, char* buffer);
	int parser_getCurrentSampleRate(parser *d);

#ifdef __cplusplus
}
#endif

struct parser { };

class Mp3FrameParser : public parser{

	Mp3Header current;
	std::ifstream is;

public:
	Mp3FrameParser();
	Mp3FrameParser(std::string path);
	Mp3FrameParser(char* path);

	/* Aligns "reading head" to the first real audio frame.
	*
	* In other words, skips various frontal tags/garbage. Call almost guarantees that
	* if a real first audio frame exists, "reading head" will point to it, i.e. false
	* positives of starting frame are dissmised correctly. Degree of certainty can be
	* modified by specifying how many correct frames must be found after the candidate
	* first frame. Hardcoded default is 10, which should be more than enough (one could
	* design a structure for which this algorithm fails, but random occurence of a chain
	* 10 false positive frames is next to impossible).
	*
	* After this, succesive calls to getNextFrame read frame contents into the specified
	* buffer, until first invalid header (or eof) is reached, which indicates that all
	* audio headers have been read. (tags that come afterward are of no concern). */
	void alignToFirst();

	/* Returns true if current header is valid.
	* For now validity is based solely on syncword. */
	bool currentHeaderValid();

	/* Reads contents of frame to frameBuffer, returns bytes read.
	* Make sure frameBuffer is big enough to hold theoretical maximum mp3 frame size.
	* After alignToFirst is executed, succesive calls to getNextFrame read frame
	* contents into the specified buffer, until first invalid header (or eof) is
	* reached, which indicates that all audio headers have been read.
	* (tags that come afterward are of no concern). */
	int getNextFrame(char *frameBuffer);

	/* Returns sample rate of the currently aligned mp3 frame.  */
	int getCurrentSampleRate();

	/* For now, always returns true, placeholder for future modifications */
	bool hasNext();

private:
	void readHeader();
	void readFirstHeader();
	bool chainSearch(int requiredChainLength);
};



