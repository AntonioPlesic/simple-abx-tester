package antonioplesic.simpleabxtester.encoder;

public class FlacInfo {

	private int sample_rate;
	private int bps;
	private int channels;
	
	public FlacInfo(int sample_rate, int bps, int channels){
		this.sample_rate = sample_rate;
		this.bps = bps;
		this.channels = channels;
	}

	public int getSampleRate() {
		return sample_rate;
	}

	public int getBps() {
		return bps;
	}

	public int getChannels() {
		return channels;
	}
	
}
