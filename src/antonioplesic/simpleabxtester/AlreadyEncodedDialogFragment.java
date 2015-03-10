package antonioplesic.simpleabxtester;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import antonioplesic.simpleabxtester.encoder.mp3EncoderDecoder;
import antonioplesic.simpleabxtester.settings.SettingsHelper;

public class AlreadyEncodedDialogFragment extends DialogFragment {

	//interface for communicating this dialog's button clicks back to the activity
	public interface AlreadyEncodedDialogListener{
		public void onAlreadyEncodedDialogNeutralClick(DialogFragment dialog);
		public void onAlreadyEncodedDialogPositiveClick(DialogFragment dialog);
		public void onAlreadyEncodedDialogNegativeClick(DialogFragment dialog);
	}
	
	AlreadyEncodedDialogListener mListener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		//Verify that the host activity implements the callback interface
		try{
			mListener = (AlreadyEncodedDialogListener) activity;
		} catch( ClassCastException e){
			//The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
			+ " must implement AlreadyEncodedDialogListener");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				
		SettingsHelper settingsHelper = new SettingsHelper(getActivity());
		
		String requiredBitrate = "" + settingsHelper.getBitrate(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		String requiredQuality = "" + settingsHelper.getQuality(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		
		//string representation of bitrate
		requiredBitrate = mp3EncoderDecoder.getStr(Integer.parseInt(requiredBitrate));
		
		String encoder = "Encoder: LAME";
		String decoder = "Decoder: LAME (mpglib)";
		String bitrate = "Bitrate: " + requiredBitrate;
		String quality = "Quality: " + requiredQuality;
		
		String message = "Required encodings/decodings for this file using settings:" + "\n" 
							+"\t\t" + encoder + "\n"
							+"\t\t" + decoder + "\n"
							+"\t\t" + bitrate + "\n"
							+"\t\t" + quality + "\n"
							+ "already exist.";
		
		builder.setMessage(message)
		//TODO: hardcoded text
		.setPositiveButton("Re-encode", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onAlreadyEncodedDialogPositiveClick(AlreadyEncodedDialogFragment.this);
				
			}
		})
		.setNeutralButton("Encoder settings", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onAlreadyEncodedDialogNeutralClick(AlreadyEncodedDialogFragment.this);
				
			}
		}).setNegativeButton("Do not re-encode", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onAlreadyEncodedDialogNegativeClick(AlreadyEncodedDialogFragment.this);
				
			}
		}).setTitle("Re-encode?");

		return builder.create();
	}
}

























