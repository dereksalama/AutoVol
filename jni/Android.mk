LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
#LOCAL_CFLAGS:= -Wall -std=c99 -g #-O2 -march=armv6 -marm -mfloat-abi=softfp -mfpu=vfp
#LOCAL_CPPFLAGS:= -Wall -g #-O2 -march=armv6 -marm -mfloat-abi=softfp -mfpu=vfp
LOCAL_CFLAGS:= -Wall -std=c99 -O2 -march=armv6 -marm -mfloat-abi=softfp -mfpu=vfp
LOCAL_CPPFLAGS:= -Wall -O2 -march=armv6 -marm -mfloat-abi=softfp -mfpu=vfp
# Here we give our module name and source file(s)
LOCAL_MODULE    := computeFeatures
#LOCAL_SRC_FILES := computeFeatures.c voice_features.c kiss_fft.c kiss_fftr.c fft.c mfcc.c mvnpdf.c
# LOCAL_SRC_FILES := computeFeatures.c voice_features.c kiss_fft.c kiss_fftr.c fft.c mfcc.c
LOCAL_SRC_FILES := computeFeatures.c voice_features.c kiss_fft.c kiss_fftr.c mvnpdf.c viterbi.c
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
 
include $(BUILD_SHARED_LIBRARY)