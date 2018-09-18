// Liudas Maþeika 2018-05-20
// Android Studio 2.3.1
package com.example.liudas.beissaugojimo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

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

    ImageView imageView;
    Bitmap bitmap;

    String imgDecodableString;
    double kint = 12810720.0; //13MB tasku skaicius
    private static final int leidimas = 8;

    static //parodo ar buvo prijungta openCV biblioteka
    {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Cannot connect to OpenCV Manager");
        } else {
            Log.e("OpenCV", "Connected Successfully");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button pirmasbtn = (Button) findViewById(R.id.pirmasbtn); //surandami mygtukai
        Button antrasbtn = (Button) findViewById(R.id.antrasbtn);
        imageView = (ImageView) findViewById(R.id.imageView);

        //Priskiriami darbai mygtukams, paspaudus papraðomi atitinkami leidimai.

        pirmasbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    if (shouldShowRequestPermissionRationale(
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        //kodel reikia leidimo.
                    }

                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            leidimas);

                    return;
                }
                Intent Kamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (Kamera.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(Kamera, 1);

                }

            }

        });

        //antro mygtuko darbas paspaudus atidaryti galerijà ið kurios galima bûtø pasirinkti paveiksliukà.

        antrasbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent galerija = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galerija, 2);

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); //priklausomai nuo to ar paveikslas gautas ið galerijos ar ið kameros atliekami skirtingi dekodavimo veiksmai
        try {

            if (requestCode == 1) {
                bitmap = (Bitmap) data.getExtras().get("data"); //jeigu vaizdas ið kameros gaunamas
                imageView.setImageBitmap(bitmap);

            } else if (requestCode == 2) {
                Uri selectedImage = data.getData();

                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

                imgDecodableString = cursor.getString(columnIndex);

                cursor.close();
                bitmap = BitmapFactory.decodeFile(imgDecodableString); //jeigu vaizdas ið galerijos
            } else {

            }
        } catch (Exception e) {

        }

        bitmap = bitmap.createScaledBitmap(bitmap, 512, 512, true); //scalinamas vaizdas, kad bûtø galima apdoroti
        int aukstis = bitmap.getHeight();
        int plotis = bitmap.getWidth();

        final double k = (aukstis * plotis) / kint;

        double kart = 1.0 / k;
        final int nk2 = (int) Math.round(kart);
        double dil = k;
        dil = 1 / dil;

        final int nk = (int) Math.round(dil);
        int dilationsize = 5;
        int erosionsize = 5;
        //vaizdo apdorojimo operacijos panaikinti nereikalingas maþas detales
        Mat dilation = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5 * dilationsize, 5 * dilationsize));
        Mat erosion = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5 * erosionsize, 5 * erosionsize));
        if (k < 0.05) {
            dilationsize = 2;
            erosionsize = 2;
            dilation = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1 * dilationsize, 1 * dilationsize));
            erosion = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1 * erosionsize, 1 * erosionsize));

        }

        Bitmap bitmap33 = bitmap.copy(Bitmap.Config.RGB_565, true);

        Mat HSV = new Mat(bitmap33.getHeight(), bitmap33.getWidth(), CvType.CV_8UC4);

        Utils.bitmapToMat(bitmap33, HSV);
        //--------Grûdø atskyrimas nuo priemaiðø pagal spalvà-------

        Imgproc.cvtColor(HSV, HSV, Imgproc.COLOR_RGB2HSV);//Perverèiamas vaizdas ið RGB i HSV

        Imgproc.GaussianBlur(HSV, HSV, new Size(5, 5), 0, 0); //Gauso filtras

        //Core.inRange atskiria grûdus pagal spalva
        Core.inRange(HSV, new Scalar(10, 80, 80), new Scalar(30, 200, 200), HSV); //(nuo 20laipsniu iki 60laipsniu ruda-geltona)

        Imgproc.dilate(HSV, HSV, dilation);

        Imgproc.erode(HSV, HSV, erosion);
        Imgproc.dilate(HSV, HSV, dilation);

        Mat HSV1 = HSV;
        Utils.matToBitmap(HSV1, bitmap33);
        imageView.setImageBitmap(bitmap33);

        List<MatOfPoint> contours2 = new Vector<>();
        Mat hierarchy2 = new Mat();
        Imgproc.findContours(HSV, contours2, hierarchy2, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int spgr = 0; //grudu skaicius pagal spalva
        double spalvagr = 0; //grudu uzimamas plotas pagal spalva
        int didelis = 0;
        int vienas = 0;

        //skaiciuojamas grûdø plotas pagal spalva
        for (int i = 0; i < contours2.size(); i++) {
            Imgproc.drawContours(HSV, contours2, i, new Scalar(200, 200, 0, 0), 2);
            if (Imgproc.contourArea(contours2.get(i)) > 234) {
                if (Imgproc.contourArea(contours2.get(i)) > 55000 * k) {
                    didelis = 1;
                }
                spalvagr = spalvagr + Imgproc.contourArea(contours2.get(i)); //grudu plotas pagal spalva
                spgr = spgr + 1;
            }

        }
        if (spalvagr < 30000 * k) {
            vienas = 1;
        }

        //---------------Grûdø atskyrimas nuo priemaiðø pagal dydá------------------------
        Bitmap bitmap32 = bitmap.copy(Bitmap.Config.RGB_565, true);

        Mat RGB = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);

        Utils.bitmapToMat(bitmap32, RGB);

        Imgproc.cvtColor(RGB, RGB, Imgproc.COLOR_RGB2GRAY); //perverciamas vaizdas i pilkus atspalvius
        Imgproc.GaussianBlur(RGB, RGB, new Size(5, 5), 0, 0);//Gauso filtras

        //Pritaikomas prisitaikantis Thresholdingas ðeðëliø atsikratymui
        if (k < 0.04) {
            Imgproc.adaptiveThreshold(RGB, RGB, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 41, 9);//hreshold value is the weighted sum of neighbourhood values where weights are a gaussian window.
        } else {

            Imgproc.GaussianBlur(RGB, RGB, new Size(13, 13), 0, 0);
            Imgproc.threshold(RGB, RGB, 200, 255, Imgproc.THRESH_OTSU);

            Imgproc.threshold(RGB, RGB, 0, 255, Imgproc.THRESH_BINARY_INV);

        }


        List<MatOfPoint> contours = new Vector<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(RGB, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        int n = 0;
        int g = 0;
        int v = 0; //viskas ir priemaisos ir grudai
        double plotasp = 0; //priemaisu plotas
        double plotasv = 0; // visko plotas
        // skaièiuojamas grûdø uþimamas plotas pagal dydá
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(RGB, contours, i, new Scalar(200, 200, 0, 0), 2);
            if (Imgproc.contourArea(contours.get(i)) < 60000 * k) {

                plotasp = plotasp + Imgproc.contourArea(contours.get(i)); //priemaisu plotas
                if (Imgproc.contourArea(contours.get(i)) > 50) {
                    n = n + 1; //priemaisu skaicius
                }
            } else {
                g = g + 1; //grudu skaicius
            }
            plotasv = plotasv + Imgproc.contourArea(contours.get(i));

            v = v + 1;
        }
        double priem = plotasv - spalvagr;

        double proc2 = (100 * priem) / plotasv; //priemaisos pagal spalva
        if (proc2 < 0) {
            proc2 = 0;
        }
        double proc = (plotasp * 100) / plotasv; //priemaisu procentas nuo visko

        int iproc = (int) proc;
        int iproc2 = (int) proc2;
        int priemais2 = v - spgr;
        if (priemais2 < 1) {
            priemais2 = 0;
        }
        final int priemaisos = priemais2;

        if (priemaisos == 0) {
            iproc2 = 0;
        }

        n = n - spgr;
        final String sn = Integer.toString(iproc);
        final String sn2 = Integer.toString(iproc2);
        final String gn = Integer.toString(n);
        String gn2 = Integer.toString(g);

        Button treciasbtn = (Button) findViewById(R.id.treciasbtn);
        final int finalG = g; //grudu skaicius pagal plota
        final int finalN = n;

        final int finalSpgr = spgr;

        treciasbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent rezultatas = new Intent(getApplicationContext(), SecondActivity.class);
                rezultatas.putExtra("com.example.liudas.beissaugojimo", sn2 + "% priemaiðø:");
                rezultatas.putExtra("com.example.liudas.beissaugojimo.2", "1) " + finalSpgr + " grûdø/ai");
                rezultatas.putExtra("com.example.liudas.beissaugojimo.3", "2) " + finalN + " priemaiðø/os");
                startActivity(rezultatas);
            }
        });

    }
}