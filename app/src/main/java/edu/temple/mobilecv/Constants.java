package edu.temple.mobilecv;

import android.util.Size;

public class Constants {

    public static final String DEBUG_TAG = "MobileCV";
    public static final String EXTRA_Y_DATA = "edu.temple.mobilecv.Y_DATA";
    public static final String EXTRA_U_DATA = "edu.temple.mobilecv.U_DATA";
    public static final String EXTRA_V_DATA = "edu.temple.mobilecv.V_DATA";
    public static final String EXTRA_Y_ROW_STRIDE = "edu.temple.mobilecv.Y_ROW";
    public static final String EXTRA_UV_ROW_STRIDE = "edu.temple.mobilecv.UV_ROW";
    public static final String EXTRA_UV_PIXEL_STRIDE = "edu.temple.mobilecv.UV_PIXEL";
    public static final String EXTRA_ROTATION = "edu.temple.mobilecv.ROTATION";

    public static final int DEFAULT_HEIGHT = 480, DEFAULT_WIDTH = 640;
    public static final int INPUT_SIZE = 224;
    public static final int IMAGE_MEAN = 117;
    public static final float IMAGE_STD = 1;

    public static final String INPUT_NAME = "input";
    public static final String OUTPUT_NAME = "output";

    public static final String MODEL_FILE =
            "file:///android_asset/tensorflow_inception_graph.pb";
    public static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    public static final boolean MAINTAIN_ASPECT = true;
    public static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

}