package edu.temple.mobilecv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowImageClassifier;

import java.io.File;
import java.util.List;

import static edu.temple.mobilecv.Constants.IMAGE_MEAN;
import static edu.temple.mobilecv.Constants.IMAGE_STD;
import static edu.temple.mobilecv.Constants.INPUT_NAME;
import static edu.temple.mobilecv.Constants.INPUT_SIZE;
import static edu.temple.mobilecv.Constants.LABEL_FILE;
import static edu.temple.mobilecv.Constants.MODEL_FILE;
import static edu.temple.mobilecv.Constants.OUTPUT_NAME;

public class ClassifierActivity extends AppCompatActivity {

    Classifier classifier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);
        Log.i(Constants.DEBUG_TAG, "Started classification activity.");

        // main activity has taken a picture, passes it to classifier activity
        Intent intent = getIntent();
        String image_filepath = intent.getStringExtra(Constants.EXTRA_IMAGE_FILEPATH);

        // classifier activity displays picture to user
        Bitmap image = null;
        File imgFile = new  File(image_filepath);
        if (imgFile.exists()) {
            Bitmap originalImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView myImage = (ImageView) findViewById(R.id.imageView);
            myImage.setImageBitmap(originalImage);
            image = Bitmap.createScaledBitmap(originalImage, INPUT_SIZE, INPUT_SIZE, false);
        }

        // classifier activity assigns label to picture, displays label to user
        classifier = TensorFlowImageClassifier.create(
                getAssets(),
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME);

        List<Classifier.Recognition> results = classifier.recognizeImage(image);
        Classifier.Recognition bestResult = new Classifier.Recognition("", "", 0.0f, null);

        for (Classifier.Recognition result : results) {
            if (result.getConfidence() > bestResult.getConfidence())
                bestResult = result;
        }

        TextView resultsLabel = (TextView) findViewById(R.id.labelView);
        resultsLabel.setText("Class: " + bestResult.getTitle() + ", Confidence: " + bestResult.getConfidence());
    }

}