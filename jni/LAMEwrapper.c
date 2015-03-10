#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>
#include "libmp3lame/lame.h"
#include "mp3FrameParser/Mp3FrameParser_C.h"

#define LOG_TAG "LAME ENCODER"
#define LOGD(format, args...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ##args);
#define BUFFER_SIZE 30000
#define be_short(s) ((short) ((unsigned short) (s) << 8) | ((unsigned short) (s) >> 8))

lame_t lame;
hip_t hipt;

JNIEXPORT void JNICALL Java_antonioplesic_simpleabxtester_StartScreenActivity_nativeMethod(JNIEnv *env,
		jobject jobj){

	//LOGD("usao u c metodu");

	jclass thisClass = (*env)->GetObjectClass(env,jobj);

	jmethodID midCallBack = (*env)->GetMethodID(env,thisClass,"callback","(F)V");
	if(NULL == midCallBack) return;

	int i = 0;
	int j = 0;

	for(i=0;i<100;i++){
		for(j=0;j<100;j++){

		}
		(*env)->CallVoidMethod(env,jobj,midCallBack,(jfloat) i);
	}

}

JNIEXPORT void JNICALL Java_antonioplesic_simpleabxtester_StartScreenActivity_nativeMethod2(JNIEnv *env,
		jobject jobj, jobject listener){

	//LOGD("usao u c metodu");

	jclass listenerClass = (*env)->GetObjectClass(env,listener);
	jmethodID midCallBack = (*env)->GetMethodID(env,listenerClass,"updateProgress","(F)V");
	if(NULL == midCallBack) return;

	int i = 0;
		int j = 0;

		for(i=0;i<100;i++){
			for(j=0;j<100;j++){

			}
			(*env)->CallVoidMethod(env,listener,midCallBack,(jfloat) i);
		}

}


JNIEXPORT jobject JNICALL Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_getInfo(
		JNIEnv *env, jobject jobj, jstring in_source_path){

	//used to construct Mp3Info object
	jobject mp3Info;
	jmethodID constructor;
	jobject cls;
	cls = (*env)->FindClass(env,"antonioplesic/simpleabxtester/encoder/Mp3Info");
	constructor = (*env)->GetMethodID(env, cls, "<init>", "(I)V");

	const char *source_path;
	source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);

	//parse mp3 file for needed properties
	parser* myParser = new_parser(source_path);
	parser_alignToFirst(myParser);
	int sampleRate = parser_getCurrentSampleRate(myParser);
	delete_parser(myParser);

	//create and return Mp3Info
	mp3Info = (*env)->NewObject(env, cls, constructor, sampleRate);

	return mp3Info;


}

JNIEXPORT void JNICALL Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_encodeFile(JNIEnv *env,
		jobject jobj, jstring in_source_path, jstring in_target_path, jobject progressCallbackObj, jobject stopExecutionCallbackObj) {

	jclass klasa;
	jmethodID metoda;

	jclass klasa2;
	jmethodID metoda2;

	if(progressCallbackObj != NULL){
		klasa = (*env)->GetObjectClass(env,progressCallbackObj);
		metoda = (*env)->GetMethodID(env, klasa, "updateProgress", "(F)V");
	}
	else{
		//LOGD("Progress callback je null");
	}

	if(stopExecutionCallbackObj != NULL){
		klasa2 = (*env)->GetObjectClass(env,stopExecutionCallbackObj);
		metoda2 = (*env)->GetMethodID(env, klasa2, "stopExecution", "()Z");

	}

	const char *source_path, *target_path;
	source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);
	target_path = (*env)->GetStringUTFChars(env, in_target_path, NULL);

	FILE *input_file, *output_file;
	input_file = fopen(source_path, "rb");
	output_file = fopen(target_path, "wb");

	int read;
	int write;

	long fileSize;
	fseek(input_file, 0L, SEEK_END);
	fileSize = ftell(input_file);
	fseek(input_file, 0L, SEEK_SET);
	//LOGD("file size %d",fileSize);

	int readTotal = 0;
	int counter = 0;

//	const int PCM_SIZE = 8192;
//	const int MP3_SIZE = 8192;

	short int pcm_buffer[BUFFER_SIZE*2];
	unsigned char mp3_buffer[BUFFER_SIZE];

	do{
		read = fread(pcm_buffer,2*sizeof(short int),BUFFER_SIZE,input_file);
		if(read==0){
			write = lame_encode_flush(lame,mp3_buffer,BUFFER_SIZE);
			fwrite(mp3_buffer,write,1,output_file);		//??
			fflush(output_file);						//??
		}
		else{
			write = lame_encode_buffer_interleaved(lame, pcm_buffer, read, mp3_buffer, BUFFER_SIZE);
			fwrite(mp3_buffer,write,1,output_file);
		}

		readTotal+=(read*2*sizeof(short int));
		//LOGD("Read total: %d",readTotal);
		//LOGD("Encoding... %d, %g\n",counter,((float) readTotal)/fileSize);

		jfloat progress = ((jfloat) readTotal)/fileSize;
		if(progressCallbackObj != NULL){
			(*env)->CallVoidMethod(env,progressCallbackObj,metoda,progress);
		}

		if((stopExecutionCallbackObj != NULL) && (counter%10 == 0)){
			jboolean stop = (*env)->CallBooleanMethod(env,stopExecutionCallbackObj,metoda2);
			if(stop){
				break;
			}
		}

		counter++;

	} while(read != 0);
	//LOGD("Encoding complete");

	lame_close(lame);
	fclose(input_file);
	fclose(output_file);

}

int read_samples(FILE *input_file, short *input, short *left, short *right) {
	int nb_read;
	nb_read = fread(input, 1, 2*sizeof(short), input_file) / sizeof(short);

	//WTF?
//	int i = 0;
//	while (i < nb_read) {
//		input[i] = be_short(input[i]);
//		i++;
//	}

	int i = 0;
	while (i < nb_read){
		left[i] = input[i];
		right[i] = input[i+1];
		i+=1;
	}

	return nb_read;
}
//        antonioplesic.simpleabxtester.encoder.mp3EncoderDecoder.initEncoder
void Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_initEncoderCBR(JNIEnv *env,
		jobject jobj, jint in_num_channels, jint in_samplerate, jint in_brate,
		jint in_mode, jint in_quality, jint out_samplerate) {

	lame = lame_init();

	//LOGD("Init parameters:");
	lame_set_num_channels(lame, in_num_channels);
	//LOGD("Number of channels: %d", in_num_channels);
	lame_set_in_samplerate(lame, in_samplerate);
	//LOGD("Sample rate: %d", in_samplerate);
	lame_set_mode(lame, in_mode);
	//LOGD("Mode: %d", in_mode);


	//izvorno ponasanje, vrati nakon experimentiranja
	lame_set_brate(lame, in_brate);
	//LOGD("Bitrate: %d", in_brate);
	lame_set_quality(lame, in_quality);
	//LOGD("Quality: %d", in_quality);
	lame_set_out_samplerate(lame, out_samplerate);
	//LOGD("Output sample rate: %d", out_samplerate);


	//experimentiranje s presetima
//
//	lame_set_VBR(lame,vbr_default);
//	lame_set_preset(lame,V0);

	int res = lame_init_params(lame);
	//LOGD("Init returned: %d", res);
}

void Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_initEncoderPreset(JNIEnv *env,
		jobject jobj, jint in_num_channels, jint in_samplerate, jint in_mode, jint preset){

	lame = lame_init();

	//LOGD("Init parameters:");
	lame_set_num_channels(lame, in_num_channels);
	//LOGD("Number of channels: %d", in_num_channels);
	lame_set_in_samplerate(lame, in_samplerate);
	//LOGD("Sample rate: %d", in_samplerate);
	lame_set_mode(lame, in_mode);
	//LOGD("Mode: %d", in_mode);

	lame_set_VBR(lame,vbr_default);
	lame_set_preset(lame,preset);

//	lame_set_bWriteVbrTag(lame,1);


	int res = lame_init_params(lame);
	//LOGD("Init returned: %d", res);

//	lame_init_bitstream(lame);

}

void Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_initDecoder(JNIEnv *env,
		jobject jobj){

	hipt = hip_decode_init();
//	lame_decode_init();
	//LOGD("Encoder created, i guess...");

}

void Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_destroyEncoder(
		JNIEnv *env, jobject jobj) {
	int res = lame_close(lame);
	//LOGD("Deinit returned: %d", res);
}

void Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_destroyDecoder(
		JNIEnv *env, jobject jobj) {
	hip_decode_exit(hipt);
	//LOGD("Decoder destroyed, i guess...");
}



#define LOW__BYTE(x) (x & 0x00ff)
#define HIGH_BYTE(x) ((x >> 8) & 0x00ff)

void put_pcm16(FILE * outf, short left[], short right[],int read){

	char data[2*1152*2*2];
	int i,m = 0;

	for (i = 0; i < read; i++) {
		short   x = left[i], y = right[i];
		/* write 16 Bits Low High */
		data[m++] = LOW__BYTE(x);
		data[m++] = HIGH_BYTE(x);
		/* write 16 Bits Low High */
		data[m++] = LOW__BYTE(y);
		data[m++] = HIGH_BYTE(y);
	}

	if (m > 0) {
		fwrite(data, 1, m, outf);
	}

}

void Java_antonioplesic_simpleabxtester_encoder_mp3EncoderDecoder_decodeFile(JNIEnv *env,
		jobject jobj, jstring in_source_path, jstring in_target_path, jobject progressCallbackObj, jobject stopExecutionCallbackObj){

	jclass klasa;
	jmethodID metoda;

	jclass klasa2;
	jmethodID metoda2;

	if(progressCallbackObj != NULL){
		klasa = (*env)->GetObjectClass(env,progressCallbackObj);
		metoda = (*env)->GetMethodID(env, klasa, "updateProgress", "(F)V");
	}
	else{
		//LOGD("Progress callback je null");
	}

	if(stopExecutionCallbackObj != NULL){
			klasa2 = (*env)->GetObjectClass(env,stopExecutionCallbackObj);
			metoda2 = (*env)->GetMethodID(env, klasa2, "stopExecution", "()Z");

		}

	const char *source_path, *target_path;
	source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);
	target_path = (*env)->GetStringUTFChars(env, in_target_path, NULL);

	FILE *input_file, *output_file;
	input_file = fopen(source_path, "rb");
	output_file = fopen(target_path, "wb");

	long fileSize;
	fseek(input_file, 0L, SEEK_END);
	fileSize = ftell(input_file);
	fseek(input_file, 0L, SEEK_SET);
	//LOGD("file size %d",fileSize);

//	//position file pointer to the first frame (to skip tags)
//	int firstHeaderStart = findFirstHeader(input_file);
//	//LOGD("first header found at %x",firstHeaderStart);
//	fseek(input_file,firstHeaderStart,SEEK_SET);



	char buffer[1500] = {NULL};

	short pcmLeft[2000];
	short pcmRight[2000];
	int ndecoded;

	int counter = 0;
	int length 	= 1;

	int iterationCounter = 0; //used to control frequency of calling stopExecution callback (TODO: progress callback)

	//**************************************************************************
	//new way of doing it

	parser* myParser = new_parser(source_path);
	parser_alignToFirst(myParser);
	//LOGD("ALIGN KAKTI GOTOV");

	int bytesRead;
	int bytesReadSoFar = 0;

	do{
		bytesRead = parser_getNextFrame(myParser,buffer);
		//LOGD("procitano %d byteova, novi nacin", bytesRead);
		bytesReadSoFar += bytesRead;

		ndecoded = hip_decode(hipt,buffer,bytesRead,pcmLeft,pcmRight);
		//LOGD("dekodirano %d sampleova koristeci novi nacin",ndecoded);

		//this progress is not completely accurate as it ignores possibly large tags
		//though they are usually relatively small compared to audio data
		//TODO: fix progress
		jfloat progress = ((jfloat) bytesReadSoFar)/fileSize;
		if(progressCallbackObj != NULL){
			(*env)->CallVoidMethod(env,progressCallbackObj,metoda,progress);
		}

		if((stopExecutionCallbackObj != NULL) && (iterationCounter%100 == 0)){
			jboolean stop = (*env)->CallBooleanMethod(env,stopExecutionCallbackObj,metoda2);
			if(stop){
				break;
			}
		}

		if(ndecoded>0){
			put_pcm16(output_file,pcmLeft,pcmRight,ndecoded);
		}

		iterationCounter++;

		//LOGD("bytesRead: %d",bytesRead);
	}while(bytesRead>0);

	delete_parser(myParser);

	//*************************************************************************


//	while(!feof(input_file) && length>0){
//
//		length = getMp3Frame(input_file,&counter,buffer);
//		//LOGD("frame, length %d",length);
//
////		//LOGD("dajem frame decoderu");
//		ndecoded = hip_decode(hipt,buffer,length,pcmLeft,pcmRight);
//		//LOGD("dekodirano %d sampleova",ndecoded);
//
//		jfloat progress = ((jfloat) ftell(input_file))/fileSize;
//		if(progressCallbackObj != NULL){
//			(*env)->CallVoidMethod(env,progressCallbackObj,metoda,progress);
//		}
//
//		if((stopExecutionCallbackObj != NULL) && (iterationCounter%100 == 0)){
//			jboolean stop = (*env)->CallBooleanMethod(env,stopExecutionCallbackObj,metoda2);
//			if(stop){
//				break;
//			}
//		}
//
//
//
//
////		if(length>0){
////			fwrite(buffer,sizeof(char),length,output_file);
////		}
//
//		if(ndecoded>0){
//			put_pcm16(output_file,pcmLeft,pcmRight,ndecoded);
//		}
//
//		iterationCounter++;
//
//
//	}

	//LOGD("Decoding complete");

	fflush(output_file);

	fclose(input_file);
	fclose(output_file);

}

























