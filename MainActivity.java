package com.example.liudas.beissaugojimo;
// +370 676 23025 D.M.

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Cannot connect to OpenCV Manager");
        } else {
            Log.e("OpenCV", "Connected Successfully");
        }
    }
    ImageView imageView, imageView2;
    //ImageView imageView2 = (ImageView) findViewById(R.id.imageView2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //--------------



        //-----------


        Button pirmasbtn = (Button) findViewById(R.id.pirmasbtn);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView2 = (ImageView) findViewById(R.id.imageView2);

        pirmasbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent Kamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (Kamera.resolveActivity(getPackageManager()) != null)
                {
                    startActivityForResult(Kamera, 0);
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int dilationsize = 2;
        int erosionsize =2;
        Mat dilation = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(1*dilationsize , 1*dilationsize));
        Mat erosion = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(1*erosionsize , 1*erosionsize));

        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
        Bitmap bitmap32 = bitmap.copy(Bitmap.Config.RGB_565, true);

        Bitmap bitmap33 = bitmap.copy(Bitmap.Config.RGB_565, true);


        //Hue, saturation, brigthness// pagal spalva apdorojimas
        Mat HSV = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat HSG = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat HSG2 = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);

        Utils.bitmapToMat(bitmap33, HSV);

        Imgproc.cvtColor(HSV, HSV, Imgproc.COLOR_RGB2HSV);//BGR ar RGB?
        //Scalar yellow = (255; 255; 0);
        //Core.inRange(HSV, new Scalar(0,0, 0), new Scalar(9, 200, 200), HSG);//del priemaisu GALIMA PRIDETI ZALIUS GRUDUS ir kitokias spalvas
        //Core.inRange(HSV, new Scalar(31,0, 0), new Scalar(360, 255, 255), HSG2);//del priemaisu tamsios spalvos low value

        Core.inRange (HSV, new Scalar(10, 50, 50), new Scalar(30, 200, 200), HSV ); //(nuo 20laipsniu iki 60laipsniu ruda-geltona)

        Imgproc.dilate(HSV, HSV, dilation);
        Imgproc.erode(HSV, HSV, erosion);

        List<MatOfPoint> contours2 = new Vector<>();
        Mat hierarchy2 = new Mat();
        Imgproc.findContours(HSV, contours2 , hierarchy2, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        int spgr=0; //grudu skaicius pagal spalva
        double spalvagr=0; //grudu uzimamas plotas pagal spalva

        for (int i=0; i<contours2.size(); i++)
        {
            Imgproc.drawContours(HSV, contours2, i, new Scalar(255,0,0), 1);
            if (Imgproc.contourArea(contours2.get(i))>40)
            {
                //Imgproc.drawContours(RGB, contours, i, new Scalar(255,0,0), 1);
                spalvagr = spalvagr + Imgproc.contourArea(contours2.get(i)); //grudu plotas pagal spalva
                spgr=spgr+1;
            }
        }
        Utils.matToBitmap(HSV, bitmap33);
        imageView.setImageBitmap(bitmap33);



        //Pagal uzimama plota

        Mat RGB = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        //Mat Grey = new Mat();
        Utils.bitmapToMat(bitmap32,RGB);
        Imgproc.cvtColor(RGB, RGB, Imgproc.COLOR_RGB2GRAY); //pervercia i grey
        Imgproc.GaussianBlur(RGB, RGB, new Size(5, 5), 0, 0);
        //Imgproc.equalizeHist(RGB, RGB);
        //Imgproc.adaptiveThreshold();


        Imgproc.adaptiveThreshold(RGB, RGB, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 25, 9 );//hreshold value is the weighted sum of neighbourhood values where weights are a gaussian window.
        //Imgproc.equalizeHist(RGB, RGB);

        // 255- maxValue − A variable of double type representing the value that is to be given if pixel value is more than the threshold value

        // adaptiveMethod − A variable of integer the type representing the adaptive method to be used. This will be either of the following two values
        //ADAPTIVE_THRESH_MEAN_C − threshold value is the mean of neighborhood area.
        //ADAPTIVE_THRESH_GAUSSIAN_C − threshold value is the weighted sum of neighborhood values where weights are a Gaussian window.
        //thresholdType − A variable of integer type representing the type of threshold to be used
        //blockSize − A variable of the integer type representing size of the pixelneighborhood used to calculate the threshold value.
        //C − A variable of double type representing the constant used in the both methods (subtracted from the mean or weighted mean).
        // https://www.tutorialspoint.com/opencv/opencv_adaptive_threshold.htm

        //Imgproc.threshold(RGB, RGB, 15, 255, 1);

        Imgproc.dilate(RGB, RGB, dilation);
        Imgproc.erode(RGB, RGB, erosion);
        //Vector<Vector<Point>> contours=null;
        //Vector<Vec4i> hierarchy;
        List<MatOfPoint> contours = new Vector<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(RGB, contours , hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //RETR_EXTERNAL - isorines boundaries tiktais
        //RETR_TREE - ir vidines su hierarchija
        //RETR_LIST viska'
        //CHAIN_A.._simple pasilieka tik keleta tasku konturo, leidzia labiau suapvalinti
        int n=0;
        int g=0;
        double plotasp=0; //priemaisu plotas
        double plotasv=0; // visko plotas

        for (int i=0; i<contours.size(); i++)
        {
            //if(contours.size()>100)
            Imgproc.drawContours(RGB, contours, i, new Scalar(255,0,0), 1);
            if (Imgproc.contourArea(contours.get(i))<40)
            {
                //Imgproc.drawContours(RGB, contours, i, new Scalar(255,0,0), 1);
                plotasp = plotasp + Imgproc.contourArea(contours.get(i)); //priemaisu plotas
                n=n+1; //priemaisu skaicius
            }
            else
            {
                g=g+1; //grudu skaicius
            }
            plotasv=plotasv+Imgproc.contourArea(contours.get(i));

            //n=n+1;
        }
        double proc2= 100-(100*spalvagr/(spalvagr+plotasp)); //priemaisos pagal spalva
        double proc= (plotasp*100)/plotasv; //priemaisu procentas nuo visko


        Utils.matToBitmap(RGB,bitmap32);
        imageView2.setImageBitmap(bitmap32);

        TextView nr = (TextView) findViewById(R.id.nr);
        int iproc = (int) proc;
        int iproc2= (int) proc2;
        String sn= Integer.toString(iproc);
        String sn2= Integer.toString(iproc2);
        String gn= Integer.toString(n);
        String gn2= Integer.toString(g);
        nr.setText(sn + "%pr.plot "+ sn2+ "%pagalsp."+" "+ n + "-priemaisu sk." +" "+ g + "-gr.");




    }
}
