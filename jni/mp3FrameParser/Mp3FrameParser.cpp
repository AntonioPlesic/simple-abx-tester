#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <limits>
#include "Mp3FrameParser_C++.h"
#include <android/log.h>

#define LOG_TAG "MP3 PARSER"
#define LOGD(format, args...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ##args);
#define LOGI(format, args...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, format, ##args);
#define LOGW(format, args...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, format, ##args);
#define LOGE(format, args...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, format, ##args);

using namespace std;

//=============================================================================
// Some stuff useful for debugging
//=============================================================================
char nibbleToHex(unsigned int bits){
	switch(bits){
		case 0: return '0';
		case 1: return '1';
		case 2: return '2';
		case 3: return '3';
		case 4: return '4';
		case 5: return '5';
		case 6: return '6';
		case 7: return '7';
		case 8: return '8';
		case 9: return '9';
		case 10: return 'A';
		case 11: return 'B';
		case 12: return 'C';
		case 13: return 'D';
		case 14: return 'E';
		case 15: return 'F';
		default: return 'Z';
	}
}

char firstNibble(Mp3Octet octet){
	unsigned int bits = octet.c>>4;
	return nibbleToHex(bits);
}

char secondNibble(Mp3Octet octet){
	unsigned int bits = (octet.c & 0x0f);
	return nibbleToHex(bits);
}
//=============================================================================


//TODO: make it a class method
bool sanityCheck(Mp3Header header){

	//maximum frame size
	if(header.frameSizeNaive() > 8065){
		return false;
	}

	//TODO: more sanity checking

	//maximum frame size when only possible combinations are considered

	//reserved bitrate

	//reserved sampling rate

	//allowed combinations

	//using crc

	return true;
}

//=============================================================================
// Mp3 octet
//=============================================================================
/* (regular octet, should really be raw char, but here in own struct to faciliate 
 * nice input and debug output) */

Mp3Octet::Mp3Octet(octet _c) : c(_c) {};
Mp3Octet::Mp3Octet() : Mp3Octet(0) {};

bool Mp3Octet::operator==(const Mp3Octet& other){
	return this->c == other.c;
}

bool Mp3Octet::operator!=(const Mp3Octet& other){
	return !(*this == other);
}

ostream& operator<<(std::ostream& os,const Mp3Octet& octet){
	return (os << std::hex << ((octet.c < 16) ? "0" : "") << (int)octet.c);
}

istream& operator>>(std::istream& is, Mp3Octet& octet){
	is >> octet.c;
	return is;
}



//=============================================================================
// Mp3 Header
//=============================================================================

Mp3Header::Mp3Header(){}

Mp3Header::Mp3Header(std::istream::pos_type position, Mp3Octet first, Mp3Octet second, Mp3Octet third, Mp3Octet fourth) 
{
	this->first = first;
	this->second = second;
	this->third = third;
	this->fourth = fourth;
	this->position = position;
	init();
}

void Mp3Header::init()
{
	this->mpeg	=	((this->second.c) & 0x18) >> 3;
	this->layer		=	((this->second.c) & 0x06) >> 1;
	this->protection =	((this->second.c) & 0x01) >> 0;

	this->bitrate =		((this->third.c) & 0xF0) >> 4;
	this->sampling =	((this->third.c) & 0x0C) >> 2;
	this->padding =		((this->third.c) & 0x02) >> 1;
}

string Mp3Header::getMpegVersionName() const
{
	switch (this->mpeg)
	{
	case MPEG_2_5: return "MPEG-2.5";
	case MPEG_RESERVED: return "reserved";
	case MPEG_VERSION_2: return "MPEG-2";
	case MPEG_VERSION_1: return "MPEG-1";
	default: return "Invalid";
	}
}

string Mp3Header::getLayerName() const
{
	switch (this->layer)
	{
	case LAYER_RESERVED: return "reserved";
	case LAYER_III: return "layer III";
	case LAYER_II: return "layer II";
	case LAYER_I: return "layer I";
	default: return "invalid";
	}
}

unsigned int Mp3Header::getBitrateValue() const
{
	static unsigned int bitrateValues[5][16] = { 
		{ 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1 },	//mpeg1 layer1
		{ 0, 32, 48, 56,  64,  80,  96, 112, 128, 160, 192, 224, 256, 320, 384, -1 },	//mpeg1 layer2
		{ 0, 32, 40, 48,  56,  64,  80,  96, 112, 128, 160, 192, 224, 256, 320, -1 },	//mpeg1 layer3
		{ 0, 32, 48, 56,  64,  80,  96, 112, 128, 144, 160, 176, 192, 224, 256, -1 },	//mpeg2/2.5 layer1
		{ 0,  8, 16, 24,  32,  40,  48,  56,  64,  80,  96, 112, 128, 144, 160, -1 }	//mpeg2/2.5 layer2/3
	};			

	unsigned int mpeg = this->mpeg;
	unsigned int layer = this->layer;
	unsigned int bitrate = this->bitrate;

	if		(mpeg == MPEG_VERSION_1 && layer == LAYER_I)		{ return bitrateValues[0][bitrate]; }
	else if (mpeg == MPEG_VERSION_1 && layer == LAYER_II)		{ return bitrateValues[1][bitrate]; }
	else if (mpeg == MPEG_VERSION_1 && layer == LAYER_III)		{ return bitrateValues[2][bitrate]; }
	else if ((mpeg == MPEG_VERSION_2 || mpeg == MPEG_2_5) 
				&& layer == LAYER_I)							{ return bitrateValues[3][bitrate]; }
	else if ((mpeg == MPEG_VERSION_2 || mpeg == MPEG_2_5) 
				&& (layer == LAYER_II || layer == LAYER_III))	{ return bitrateValues[4][bitrate]; }
	else{
		return -1;//error
	}
}

unsigned int Mp3Header::getSamplingValue() const
{
	static unsigned int samplingValues[3][4] = {
		{ 44100, 48000, 32000, -1},					//mpeg1
		{ 22050, 24000, 16000, -1},					//mpeg2
		{ 11025, 12000,  8000, -1}					//mpeg2.5
	};

	switch (this->mpeg) {
	case MPEG_VERSION_1: 	return samplingValues[0][this->sampling];
	case MPEG_VERSION_2: 	return samplingValues[1][this->sampling];
	case MPEG_2_5: 			return samplingValues[2][this->sampling];
	default:				return -1; //error
	}



}

long Mp3Header::frameSizeNaive() const
{
	//TODO: URGENT FIX NEEDED, value is not valid for all cases
	static const int samplesPerFrame = 1152;
	//Frame Size = ( (Samples Per Frame / 8 * Bitrate) / Sampling Rate) + Padding Size
	return (long) ( ((((double)samplesPerFrame / 8)*getBitrateValue() * 1000) / getSamplingValue()) + padding);
}

std::ifstream::pos_type Mp3Header::nextFramePosition() const
{
	return position + ((std::ifstream::pos_type) frameSizeNaive());
}

/*returns true if sync part actually looks like required sync (11 high bits)
*/
bool Mp3Header::validSync()
{
	if (first.c == 0xff){
		return true;
	}
	return false;
}


ostream& operator<<(std::ostream& os, const Mp3Header& header)
{
	string version = header.getMpegVersionName();

	return (os << "Mp3Header consisting of: "
		<< header.first << " "
		<< header.second << " "
		<< header.third << " "
		<< header.fourth) << " "
		<< header.getMpegVersionName() << " "
		<< header.getLayerName() << " "
		<< dec										//bode u oci, jebem ti c++
		<< header.getBitrateValue() << " "
		<< header.getSamplingValue() << " "
		<< header.frameSizeNaive() << " "
		<< header.nextFramePosition() ; 
}


//=============================================================================
// Mp3 frame parser
//=============================================================================

Mp3FrameParser::Mp3FrameParser() :is("C:\\Users\\Antonio\\Desktop\\double blind audio tester fajlovi\\Taylor Swift - Today Was a Fairytale.mp3", std::ios::binary){}
Mp3FrameParser::Mp3FrameParser(std::string path) : is(path, std::ios::binary) { is>>std::noskipws; }
Mp3FrameParser::Mp3FrameParser(char* path) : is(path, std::ios::binary) { is>>std::noskipws; }

bool Mp3FrameParser::currentHeaderValid()
{
	//there more to it than just this

	//compare other fields too, especially to previous frames (if any)

	return current.validSync();
}

int Mp3FrameParser::getNextFrame(char *frameBuffer)
{
	//LOGD("Position: %06x, Length %d:, Next at: %06x",(int) current.position, current.frameSizeNaive(), (int) current.nextFramePosition());
	//LOGD("%c%c %c%c %c%c %c%c",firstNibble(current.first),secondNibble(current.first),firstNibble(current.second),secondNibble(current.second),firstNibble(current.third),secondNibble(current.third),firstNibble(current.fourth),secondNibble(current.fourth));
	//LOGD("%s, %s, Sample rate: %d, Bitrate: %d",current.getMpegVersionName().c_str(),current.getLayerName().c_str(),current.getSamplingValue(),current.getBitrateValue());

	/* Check if header is valid.
	 * Actual first frame/header will always be valid (due to appropriate method that ensures that such frame/header is found). 
	 * All other audio frames from that point on should be valid.
	 * If a frame is suddenly invalid, it means that end of data has been reached (no more audio frames remain),
	 * stream only contains tags or garbage from this point on, both of which are of no interest. */
	if (!currentHeaderValid()){
		//LOGD("Header invalid: header in question: %06x",(int) current.position);
		return 0;
	}

	//if stream in bad or eof state return, hasNext = false
	if (is.bad() || is.eof()){
		cout << "EOF||BAD" << endl;
	}

	//go to the beggining of current frame
	is.seekg(current.position-is.tellg(), std::ios::cur);

	is.read(frameBuffer, current.frameSizeNaive());

	//position head to the next frame
	//read its header
		//check if really a header

	is.seekg(current.nextFramePosition() - is.tellg(), std::ios::cur);
	readHeader();

	return is.gcount();
}

void Mp3FrameParser::readHeader()
{
	Mp3Octet c1, c2, c3, c4;
	is >> c1 >> c2 >> c3 >> c4;
	current = Mp3Header(current.nextFramePosition(), c1, c2, c3, c4);
}

void Mp3FrameParser::readFirstHeader()
{
	Mp3Octet c1, c2, c3, c4;
	ios::pos_type position = is.tellg();
	is >> c1 >> c2 >> c3 >> c4;
	current = Mp3Header(position, c1, c2, c3, c4);
}

void Mp3FrameParser::alignToFirst()
{
	is.seekg(0, std::ios::beg);
	//LOGD("should be 0: %d",(int) is.tellg());

	Mp3Octet startCandidate;
	istream::pos_type starCandidatePosition;

	while (is.good()){
		
		starCandidatePosition = is.tellg();
		is >> startCandidate;


		if (startCandidate == 0xFF){
			//LOGI("syncword encountered, possible first header at %06x",(int) starCandidatePosition);


			if (chainSearch(10)){
				//LOGD("chainsearch vratio true");
				//LOGE("start at %08x",(int) starCandidatePosition);
				is.seekg(starCandidatePosition, ios::beg);
				readFirstHeader();
				break;
			}
			else{
				is.seekg((int) starCandidatePosition +  1 );
			}
		}
	}
}

int Mp3FrameParser::getCurrentSampleRate(){
	return current.getSamplingValue();
}

bool Mp3FrameParser::chainSearch(int requiredChainLength)
{
	Mp3Octet a1, a2, a3, a4;
	Mp3Header a;
	istream::pos_type aPosition;

	int chainedFrames = 0;

	is.seekg(-1, ios::cur);

	while (is.good()){

		aPosition = is.tellg();

		is >> a1 >> a2 >> a3 >> a4; //read rest of potential header
		a = Mp3Header(aPosition, a1, a2, a3, a4);
		//LOGD("Position: %06x, Chain: %d, Length %d:, Next at: %06x",(int) a.position,chainedFrames,a.frameSizeNaive(),(int) a.nextFramePosition());

		if (a.validSync() 
			&& (a.position < a.nextFramePosition()) 
			&& (a.position>=0)
			&& (a.nextFramePosition()>0)
			&& (a.frameSizeNaive() > 0)
			&& (sanityCheck(a))
			){

			//LOGD("valid sync at %06x, length %06x",(int) aPosition,(int) a.frameSizeNaive());
			//LOGD("  next chain member at %06x",(int) a.nextFramePosition());

			chainedFrames++;
			//LOGD("chained frames %d",chainedFrames);
			is.seekg(a.nextFramePosition(), std::ios::beg); //go to the header pointed by current header

			if (chainedFrames == requiredChainLength){
				//LOGD("GOTOVA POTRAGA chained frames %d",chainedFrames);
				return true;
			}
		}
		else{
			if( !(a.nextFramePosition()>0) ){
				//LOGI("    Chain broken, next frame position negative")
			}
			if( !(a.position < a.nextFramePosition())){
				//LOGI("    Chain broken, wrong direction of next frame");
			}
			if( !a.validSync()){
				//LOGI("    Chain broken, invalid sync");
			}
			if( !(a.frameSizeNaive()>0)){
				//LOGI("    Chain broken, negative frame size");
			}
			if( !(sanityCheck(a))){
				//LOGI("    Failed sanity check");
			}

			//LOGW("Chain broken");


			return false;
		}

	}
	//LOGE("stream not good, returning false");
	is.clear();
	return false;
}

bool Mp3FrameParser::hasNext()
{
	return true;
}

//=============================================================================
//Mp3FrameParser C api implementation
//=============================================================================

inline Mp3FrameParser* real(parser* d) { return static_cast<Mp3FrameParser*>(d); }

parser* new_parser(char* path) { return new Mp3FrameParser(path); }
void delete_parser(parser* d) { delete real(d); }
void parser_alignToFirst(parser* d) { real(d)->alignToFirst(); }
int parser_getNextFrame(parser* d, char* buffer) { return real(d)->getNextFrame(buffer); }
int parser_getCurrentSampleRate(parser* d) { return real(d)->getCurrentSampleRate(); }








//----------------------End of class implementations---------------------------




//void dumpBuffer(char frameBuffer[], size_t bufferSize)
//{
//	for (int i = 0; i != bufferSize; i++){
//		if ((i+1) % 16 == 0){
//			cout << Mp3Octet(frameBuffer[i]) << "\n";
//		}
//		else{
//			cout << Mp3Octet(frameBuffer[i]) << " ";
//		}
//	}
//	cout << endl;
//}

//int main(){
//
//	//some visual studio specifics
//	if (setvbuf(stdout, 0, _IOLBF, 4096) != 0) {
//		abort();
//	}
//	//cout.sync_with_stdio(false);
//
//	//Main starts here
//
//	char buffer[2000]; //TODO: make it corret maximum frame size
//	size_t bytesRead;
//
//	//char adresa[] = "C:\\Users\\Antonio\\Desktop\\double blind audio tester fajlovi\\Taylor Swift - Today Was a Fairytale.mp3";
//	//Mp3FrameParser parser(adresa);
//	Mp3FrameParser parser("C:\\Users\\Antonio\\Desktop\\double blind audio tester fajlovi\\Taylor Swift - Today Was a Fairytale.mp3");
//	parser.alignToFirst();
//
//	int headerNo = 0;
//	do{
//		bytesRead = parser.getNextFrame(buffer);
//		//cout << headerNo++ << ": bytes read:" << count <<"\n";
//		dumpBuffer(buffer, bytesRead);
//		//cout << "\n";
//		headerNo++;
//	} while (bytesRead > 0);
//
//	cout << dec;
//	cout << "Dostignut kraj: " << headerNo << "\n\n" << endl;
//
//	return 0;
//}
