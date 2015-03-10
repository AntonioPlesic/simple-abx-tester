package antonioplesic.simpleabxtester.wavextractor;

public class WavInfo {
	
	int dataDescriptorStart;
	int dataLength;
	int dataStart;
	
	public WavInfo(int dataDescriptorStart, int dataLength){
		
		if(dataDescriptorStart>=0){
			this.dataDescriptorStart = dataDescriptorStart;
			this.dataStart = dataDescriptorStart + 2*4;
		}
		
		if(dataLength>=0){
			this.dataLength = dataLength;
		}
	}

	public int getDataDescriptorStart() {
		return dataDescriptorStart;
	}

	public int getDataLength() {
		return dataLength;
	}

	public int getDataStart() {
		return dataStart;
	}



}
