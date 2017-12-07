package edu.temple.mobilecv;

import android.os.Environment;

public class Constants {

    public static final String DEBUG_TAG = "MobileCV";
    public static final String EXTRA_ROTATION = "edu.temple.mobilecv.ROTATION";
    public static final String EXTRA_CSV_PATH = "edu.temple.mobilecv.CSV";

    public static final String MOBILE_CV_FILEPATH = Environment.getExternalStorageDirectory() + "/mobileCV/";


    // -------------------------------------------------------------------------
    //      TensorFlow for Poets demo
    // -------------------------------------------------------------------------

    public static final int INPUT_SIZE = 224;   // 32;
    public static final int IMAGE_MEAN = 128;
    public static final float IMAGE_STD = 128;

    public static final String INPUT_NAME = "input"; // "x";
    public static final String OUTPUT_NAME = "final_result";

    public static final String MODEL_FILE =
            "file:///android_asset/retrained_graph.pb";
    public static final String LABEL_FILE =
            "file:///android_asset/retrained_labels.txt";


    // -------------------------------------------------------------------------
    //      ImageNet demo
    // -------------------------------------------------------------------------

    /*public static final int INPUT_SIZE = 224;
    public static final int IMAGE_MEAN = 117;
    public static final float IMAGE_STD = 1;

    public static final String INPUT_NAME = "input";
    public static final String OUTPUT_NAME = "output";

    public static final String MODEL_FILE =
            "file:///android_asset/tensorflow_inception_graph.pb";
    public static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";*/

    public static final boolean MAINTAIN_ASPECT = true;
    public static final int DEFAULT_HEIGHT = 480, DEFAULT_WIDTH = 640;

    public static final String COMMA = ",";

}