#build LAME library
LOCAL_PATH := $(call my-dir)

 
		include $(CLEAR_VARS)
		LOCAL_MODULE    	:= libmp3lame
		LOCAL_SRC_FILES 	:= \
		./bitstream.c \
		./encoder.c \
		./fft.c \
		./gain_analysis.c \
		./id3tag.c \
		./lame.c \
		./mpglib_interface.c \
		./newmdct.c \
		./presets.c \
		./psymodel.c \
		./quantize.c \
		./quantize_pvt.c \
		./reservoir.c \
		./set_get.c \
		./tables.c \
		./takehiro.c \
		./util.c \
		./vbrquantize.c \
		./VbrTag.c \
		./version.c \
		../LAMEwrapper.c \
		../mp3FrameParser/Mp3FrameParser.cpp \
		./mpglib/common.c \
		./mpglib/interface.c \
		./mpglib/layer1.c \
		./mpglib/layer2.c \
		./mpglib/layer3.c \
		./mpglib/tabinit.c \
		./mpglib/dct64_i386.c \
		./mpglib/decode_i386.c
		
		LOCAL_LDLIBS := -llog
		
		include $(BUILD_SHARED_LIBRARY)