package antonioplesic.simpleabxtester.wavextractor;

public class WavInfoBetter {
	
	static public final int WAVE_FORMAT_PCM = 0x0001;
	static public final int WAVE_FORMAT_IEEE_FLOAT = 0x0003;
	static public final int WAVE_FORMAT_ALAW = 0x0006;
	static public final int WAVE_FORMAT_MULAW = 0x0007;
	static public final int WAVE_FORMAT_EXTENSIBLE = 0xFFFE;
	
//	int dataDescriptorStart;
	long dataLength;
	long dataStart;
	
	int format;
	int nChannels;
	int sampleRate;
	int bitsPerSample;
	
	public WavInfoBetter(long dataStart, long dataLength, int format, int nChannels, int sampleRate, int bitsPerSample){
		this.dataStart = dataStart;
		this.dataLength = dataLength;
		this.format = format;
		this.nChannels = nChannels;
		this.sampleRate = sampleRate;
		this.bitsPerSample = bitsPerSample;
	}
	
	public static String getFormatString(int format){
		
		switch (format) {
		case WAVE_FORMAT_PCM: return "PCM";
		case WAVE_FORMAT_IEEE_FLOAT: return "IEEE FLOAT";
		case WAVE_FORMAT_ALAW: return "ALAW";
		case WAVE_FORMAT_MULAW: return "MULAW";
		case WAVE_FORMAT_EXTENSIBLE: return "EXTENSIBLE";
		default: return "Unknown";
		}
		
	}
	
//	public WavInfoBetter(int dataDescriptorStart, int dataLength){
//		
//		if(dataDescriptorStart>=0){
//			this.dataDescriptorStart = dataDescriptorStart;
//			this.dataStart = dataDescriptorStart + 2*4;
//		}
//		
//		if(dataLength>=0){
//			this.dataLength = dataLength;
//		}
//	}

//	public int getDataDescriptorStart() {
//		return dataDescriptorStart;
//	}

	public long getDataLength() {
		return dataLength;
	}

	public long getDataStart() {
		return dataStart;
	}
	
	public int getFormat() {
		return format;
	}

	public int getnChannels() {
		return nChannels;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}



}
