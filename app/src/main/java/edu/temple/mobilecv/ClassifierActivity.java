package edu.temple.mobilecv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowImageClassifier;
import org.tensorflow.demo.env.ImageUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static edu.temple.mobilecv.Constants.DEFAULT_HEIGHT;
import static edu.temple.mobilecv.Constants.DEFAULT_WIDTH;
import static edu.temple.mobilecv.Constants.IMAGE_MEAN;
import static edu.temple.mobilecv.Constants.IMAGE_STD;
import static edu.temple.mobilecv.Constants.INPUT_NAME;
import static edu.temple.mobilecv.Constants.INPUT_SIZE;
import static edu.temple.mobilecv.Constants.LABEL_FILE;
import static edu.temple.mobilecv.Constants.MAINTAIN_ASPECT;
import static edu.temple.mobilecv.Constants.MODEL_FILE;
import static edu.temple.mobilecv.Constants.OUTPUT_NAME;

public class ClassifierActivity extends AppCompatActivity {

    private int previewWidth = DEFAULT_WIDTH, previewHeight = DEFAULT_HEIGHT;
    private Integer sensorOrientation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);
        Log.i(Constants.DEBUG_TAG, "Started classification activity.  Retrieving image from file system.");

        Intent intent = getIntent();
        sensorOrientation = 90 - intent.getIntExtra(Constants.EXTRA_ROTATION, 0);
        String rgbFilepath = intent.getStringExtra(Constants.EXTRA_CSV_PATH);
        int[] rgbBytes = getRgbBytes(rgbFilepath);

        Bitmap croppedBitmap = getCroppedBitmap(rgbBytes);
        ImageView finalImage = (ImageView) findViewById(R.id.imageView);
        finalImage.setImageBitmap(croppedBitmap);

        Classifier.Recognition bestResult = getBestResult(croppedBitmap);
        TextView resultsLabel = (TextView) findViewById(R.id.labelView);
        resultsLabel.setText("Class: " + bestResult.getTitle()
                + ", Confidence: " + bestResult.getConfidence()
                + "\nPlease restart app to classify again.");
    }

    private int[] getRgbBytes(String rgbFilepath) {
        List<String> stringBytes = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(rgbFilepath));
            String line;

            while ((line = br.readLine()) != null) {
                String[] snippets = line.split(Constants.COMMA);
                for (String snippet : snippets) stringBytes.add(snippet);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int[] rgbBytes = new int[stringBytes.size()];
        for (int i = 0; i < stringBytes.size(); i++)
            rgbBytes[i] = Integer.parseInt(stringBytes.get(i));

        return rgbBytes;
    }

    private Bitmap getCroppedBitmap(int[] rgbBytes) {
        Bitmap rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        Bitmap croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        Matrix frameToCropTransform = ImageUtils.getTransformationMatrix(
                previewWidth, previewHeight,
                INPUT_SIZE, INPUT_SIZE,
                sensorOrientation, MAINTAIN_ASPECT);

        Matrix cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        return croppedBitmap;
    }

    private Classifier.Recognition getBestResult(Bitmap image) {
        Log.i(Constants.DEBUG_TAG, "Starting classification recognition.");
        Classifier classifier = TensorFlowImageClassifier.create(getAssets(),
            MODEL_FILE, LABEL_FILE, INPUT_SIZE, IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME);

        List<Classifier.Recognition> results = classifier.recognizeImage(image);
        Classifier.Recognition bestResult = new Classifier.Recognition("", "", 0.0f, null);

        for (Classifier.Recognition result : results)
            if (result.getConfidence() > bestResult.getConfidence())
                bestResult = result;

        return bestResult;
    }

}