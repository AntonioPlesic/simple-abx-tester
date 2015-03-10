#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>
#include "include/FLAC/stream_decoder.h"

#define LOG_TAG "FLAC decoder"
#define LOGD(format, args...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ##args);

static JNIEXPORT FLAC__StreamDecoderWriteStatus JNICALL write_callback(const FLAC__StreamDecoder *decoder, const FLAC__Frame *frame, const FLAC__int32 * const buffer[], void *client_data);
static JNIEXPORT FLAC__StreamDecoderWriteStatus JNICALL write_callback_empty(const FLAC__StreamDecoder *decoder, const FLAC__Frame *frame, const FLAC__int32 * const buffer[], void *client_data);
static void metadata_callback(const FLAC__StreamDecoder *decoder, const FLAC__StreamMetadata *metadata, void *client_data);
static void error_callback(const FLAC__StreamDecoder *decoder, FLAC__StreamDecoderErrorStatus status, void *client_data);

static FLAC__bool write_little_endian_uint16(FILE *f, FLAC__uint16 x)
{
	return
		fputc(x, f) != EOF &&
		fputc(x >> 8, f) != EOF
	;
}

static FLAC__bool write_little_endian_int16(FILE *f, FLAC__int16 x)
{
	return write_little_endian_uint16(f, (FLAC__uint16)x);
}

static FLAC__uint64 total_samples = 0;
static unsigned sample_rate = 0;
static unsigned channels = 0;
static unsigned bps = 0;

unsigned iterationCounter = 0;
jfloat progress = 0;
unsigned decoded = 0;

JNIEnv *genv = NULL;

jclass klasa = NULL;
jmethodID metoda = NULL;
jobject callback = NULL;

jclass klasa2 = NULL;
jmethodID metoda2 = NULL;
jobject callback2 = NULL;

void resetGlobals(){
	(*genv)->DeleteLocalRef(genv,klasa);
	(*genv)->DeleteLocalRef(genv, metoda);
	(*genv)->DeleteLocalRef(genv, callback);
	(*genv)->DeleteLocalRef(genv, klasa2);
	(*genv)->DeleteLocalRef(genv, metoda2);
	(*genv)->DeleteLocalRef(genv, callback2);
	genv = NULL;

	total_samples = 0;
	sample_rate = 0;
	channels = 0;
	bps = 0;

	iterationCounter = 0;
	progress = 0;
	decoded = 0;

}

JNIEXPORT jobject JNICALL Java_antonioplesic_simpleabxtester_encoder_flacDecoder_getMetadata(
		JNIEnv *env, jobject jobj, jstring in_source_path){

	FLAC__bool ok = true;
	FLAC__StreamDecoder *decoder = 0;
	FLAC__StreamDecoderInitStatus init_status;

	jobject flacInfo;
	jmethodID constructor;
	jobject cls;

	cls = (*env)->FindClass(env, "antonioplesic/simpleabxtester/encoder/FlacInfo");
	constructor = (*env)->GetMethodID(env, cls, "<init>", "(III)V");

	const char *source_path;
	source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);

	if((decoder = FLAC__stream_decoder_new()) == NULL) {
		//LOGD("ERROR: allocating decoder\n");
		return (*env)->NewObject(env, cls, constructor, 0, 0, 0);
	}

	(void)FLAC__stream_decoder_set_md5_checking(decoder, true);

	init_status = FLAC__stream_decoder_init_file(decoder, source_path, write_callback_empty, metadata_callback, error_callback, /*client_data=*/NULL);
	if(init_status != FLAC__STREAM_DECODER_INIT_STATUS_OK) {
		//LOGD("ERROR: initializing decoder: %s\n", FLAC__StreamDecoderInitStatusString[init_status]);
		ok = false;
		return (*env)->NewObject(env, cls, constructor, 0, 0, 0);
	}

	if(ok){
		ok = FLAC__stream_decoder_process_until_end_of_metadata(decoder);
		//LOGD("decoding: %s\n", ok? "succeeded" : "FAILED");
		//LOGD("   state: %s\n", FLAC__StreamDecoderStateString[FLAC__stream_decoder_get_state(decoder)]);
	}

	FLAC__stream_decoder_delete(decoder);

	flacInfo = (*env)->NewObject(env, cls, constructor, sample_rate, bps, channels);

	return flacInfo;


}

JNIEXPORT void JNICALL Java_antonioplesic_simpleabxtester_encoder_flacDecoder_decodeFile(JNIEnv *env,
		jobject jobj, jstring in_source_path, jstring in_target_path, jobject progressCallbackObj, jobject stopExecutionCallbackObj) {

	FLAC__bool ok = true;
	FLAC__StreamDecoder *decoder = 0;
	FLAC__StreamDecoderInitStatus init_status;
	FILE *fout;

	const char *source_path, *target_path;

	source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);
	target_path = (*env)->GetStringUTFChars(env, in_target_path, NULL);
	//LOGD("%s",source_path);
	//LOGD("%s",target_path);

	if(progressCallbackObj != NULL){
		callback = progressCallbackObj;
		klasa = (*env)->GetObjectClass(env,callback);
		metoda = (*env)->GetMethodID(env,klasa,"updateProgress","(F)V");
		genv = env;

//		jfloat prog = 0.25f;
//		(*genv)->CallVoidMethod(genv,callback,metoda,prog);

	}
	else{
		//LOGD("Progress callback je null");
	}

	if(stopExecutionCallbackObj != NULL){
		callback2 = stopExecutionCallbackObj;
		klasa2 = (*env)->GetObjectClass(env,stopExecutionCallbackObj);
		metoda2 = (*env)->GetMethodID(env, klasa2, "stopExecution", "()Z");
		genv = env;
	}

//	FILE *input_file;
//	input_file = fopen(source_path, "rb");
	fout = fopen(target_path, "wb");

	if((decoder = FLAC__stream_decoder_new()) == NULL) {
		//LOGD("ERROR: allocating decoder\n");
		fclose(fout);
		return;
	}

	(void)FLAC__stream_decoder_set_md5_checking(decoder, true);

	init_status = FLAC__stream_decoder_init_file(decoder, source_path, write_callback, metadata_callback, error_callback, /*client_data=*/fout);
	if(init_status != FLAC__STREAM_DECODER_INIT_STATUS_OK) {
		//LOGD("ERROR: initializing decoder: %s\n", FLAC__StreamDecoderInitStatusString[init_status]);
		ok = false;
	}

	if(ok) {
		ok = FLAC__stream_decoder_process_until_end_of_stream(decoder);
		//LOGD("decoding: %s\n", ok? "succeeded" : "FAILED");
		//LOGD("   state: %s\n", FLAC__StreamDecoderStateString[FLAC__stream_decoder_get_state(decoder)]);
	}

	FLAC__stream_decoder_delete(decoder);
	fclose(fout);

	//LOGD("vracam se iz flac-a, \"oslobadjam resurse\"");
	resetGlobals();



}

//for case when only reading metadata, and not actually decoding anything
JNIEXPORT FLAC__StreamDecoderWriteStatus JNICALL write_callback_empty(const FLAC__StreamDecoder *decoder, const FLAC__Frame *frame, const FLAC__int32 * const buffer[], void *client_data){
	return FLAC__STREAM_DECODER_WRITE_STATUS_CONTINUE;
}

JNIEXPORT FLAC__StreamDecoderWriteStatus JNICALL write_callback(const FLAC__StreamDecoder *decoder, const FLAC__Frame *frame, const FLAC__int32 * const buffer[], void *client_data){

	FILE *f = (FILE*)client_data;
	const FLAC__uint32 total_size = (FLAC__uint32)(total_samples * channels * (bps/8));
	size_t i;

	unsigned frameSize = frame->header.blocksize;

	(void)decoder;

	if(total_samples == 0) {
		//LOGD("ERROR: this example only works for FLAC files that have a total_samples count in STREAMINFO\n");
		return FLAC__STREAM_DECODER_WRITE_STATUS_ABORT;
	}
	if(channels != 2 || bps != 16) {
		//LOGD("ERROR: this example only supports 16bit stereo streams\n");
		return FLAC__STREAM_DECODER_WRITE_STATUS_ABORT;
	}

	/* write decoded PCM samples */
		for(i = 0; i < frame->header.blocksize; i++) {
			if(
				!write_little_endian_int16(f, (FLAC__int16)buffer[0][i]) ||  /* left channel */
				!write_little_endian_int16(f, (FLAC__int16)buffer[1][i])     /* right channel */
			) {
				fprintf(stderr, "ERROR: write error\n");
				return FLAC__STREAM_DECODER_WRITE_STATUS_ABORT;
			}
		}

//		jfloat prog = 0.50f;
//		(*genv)->CallVoidMethod(genv,callback,metoda,prog);

		iterationCounter = iterationCounter + 1;
		decoded = decoded + frameSize;

		if((callback != NULL) && (iterationCounter%100 == 0)){

			progress = ( (float) decoded ) / total_samples;
			//LOGD("trebao bi callbackati, progress:%f",progress);

			(*genv)->CallVoidMethod(genv,callback,metoda,progress);
		}

		if((callback2 != NULL) && (iterationCounter%100 == 0)){
			jboolean stop = (*genv)->CallBooleanMethod(genv,callback2,metoda2);
			if(stop){
				return FLAC__STREAM_DECODER_WRITE_STATUS_ABORT;
			}
		}


		return FLAC__STREAM_DECODER_WRITE_STATUS_CONTINUE;

}

void metadata_callback(const FLAC__StreamDecoder *decoder, const FLAC__StreamMetadata *metadata, void *client_data){

	(void)decoder, (void)client_data;

	// print some stats
	if(metadata->type == FLAC__METADATA_TYPE_STREAMINFO){
		//save for later
		total_samples = metadata->data.stream_info.total_samples;
		sample_rate = metadata->data.stream_info.sample_rate;
		channels = metadata->data.stream_info.channels;
		bps = metadata->data.stream_info.bits_per_sample;

		//LOGD("sample rate    : %u Hz\n", sample_rate);
		//LOGD("channels       : %u\n", channels);
		//LOGD("bits per sample: %u\n", bps);
#ifdef _MSC_VER
		LOGD("total samples  : %I64u\n", total_samples);
#else
		LOGD("total samples  : %llu\n", total_samples);
#endif

	}
}

void error_callback(const FLAC__StreamDecoder *decoder, FLAC__StreamDecoderErrorStatus status, void *client_data){

	(void)decoder, (void)client_data;

	//LOGD("Got error callback: %s\n", FLAC__StreamDecoderErrorStatusString[status]);
}


