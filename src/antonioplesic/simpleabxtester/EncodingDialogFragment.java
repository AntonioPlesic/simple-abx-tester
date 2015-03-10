package antonioplesic.simpleabxtester;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import antonioplesic.simpleabxtester.encoder.mp3EncoderDecoder;
import antonioplesic.simpleabxtester.settings.SettingsHelper;


public class EncodingDialogFragment extends DialogFragment {
	
	public interface EncodingDialogListener{
		public void onEncodingDialogNeutralClick(DialogFragment dialog);
		public void onEncodingDialogPositiveClick(DialogFragment dialog);
	}
	
	EncodingDialogListener mListener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		//Verify that the host activity implements the callback interface
		try{
			mListener = (EncodingDialogListener) activity;
		} catch( ClassCastException e){
			//The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement EncodingDialogListener");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				
		SettingsHelper settingsHelper = new SettingsHelper(getActivity());
		String requiredBitrate = "" + settingsHelper.getBitrate(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		String requiredQuality = "" + settingsHelper.getQuality(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		
		requiredBitrate = mp3EncoderDecoder.getStr(Integer.parseInt(requiredBitrate));
		
		String encoder = "Encoder: LAME";
		String decoder = "Decoder: LAME (mpglib)";
		String bitrate = "Bitrate: " + requiredBitrate;
		String quality = "Quality: " + requiredQuality;
		
		String message = "" + encoder + "\n"
							+ decoder + "\n"
							+ bitrate + "\n"
							+ quality;
		
		
		builder.setMessage(message)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onEncodingDialogPositiveClick(EncodingDialogFragment.this);
				
			}
		})
		.setNeutralButton("Encoder settings", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onEncodingDialogNeutralClick(EncodingDialogFragment.this);
				
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		
		return builder.create();
	}
}
