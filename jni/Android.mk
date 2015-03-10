#build LAME library
LOCAL_PATH := $(call my-dir)
COPYPATH = $(LOCAL_PATH)

 
		include $(CLEAR_VARS)
		include $(LOCAL_PATH)/libmp3lame/Android.mk

		include $(CLEAR_VARS)
		include $(COPYPATH)/../libFLAC/Android.mk