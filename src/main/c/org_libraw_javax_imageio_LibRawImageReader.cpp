#include "libraw/libraw.h"
#include "org_libraw_javax_imageio_LibRawImageReader.h"

// TODO obtain error string from libraw_strerror(code)
JNIEXPORT jint JNICALL
        Java_org_libraw_javax_imageio_LibRawImageReader_loadImageFromFilePath(
        JNIEnv* env, jobject thisObj, jstring filePathJstr) {
    int err = LIBRAW_SUCCESS;
    LibRaw rawProcessor;

    const char* const fileName = env->GetStringUTFChars(filePathJstr, 0);
    err = rawProcessor.open_file(fileName);
    env->ReleaseStringUTFChars(filePathJstr, fileName);
    if (err != LIBRAW_SUCCESS) return err;

    err = rawProcessor.unpack();
    if (err != LIBRAW_SUCCESS) return err;

    err = rawProcessor.dcraw_process();
    if (err != LIBRAW_SUCCESS) return err;

    libraw_processed_image_t *img = rawProcessor.dcraw_make_mem_image(&err);
    if (img) {
      jclass thisClass = env->GetObjectClass(thisObj);

      // TODO error handling?
      jfieldID widthField = env->GetFieldID(thisClass, "width", "I");
      jfieldID heightField = env->GetFieldID(thisClass, "height", "I");
      jfieldID colorsField = env->GetFieldID(thisClass, "colors", "I");
      jfieldID bitsField = env->GetFieldID(thisClass, "bits", "I");
      jfieldID dataField = env->GetFieldID(thisClass, "data", "[B");

      env->SetIntField(thisObj, widthField, img->width);
      env->SetIntField(thisObj, heightField, img->height);
      env->SetIntField(thisObj, colorsField, img->colors);
      env->SetIntField(thisObj, bitsField, img->bits);

      jbyteArray byteArr = env->NewByteArray(img->data_size);
      jbyte* data = env->GetByteArrayElements(byteArr, 0);
      std::memcpy(data, img->data, img->data_size);
      env->SetObjectField(thisObj, dataField, byteArr);
      env->ReleaseByteArrayElements(byteArr, data, 0);
      LibRaw::dcraw_clear_mem(img);
    } else {
      // TODO determine good value
      return -1000000;
    }
    rawProcessor.recycle();

    return err;
}

// TODO Try wrapping the Java "stream" (ImageInputStream) in a LibRaw_abstract_datastream
JNIEXPORT jint JNICALL
        Java_org_libraw_javax_imageio_LibRawImageReader_loadImageFromImageInputStream(
        JNIEnv* env, jobject thisObj, jobject stream) {
    return -1000000;
}