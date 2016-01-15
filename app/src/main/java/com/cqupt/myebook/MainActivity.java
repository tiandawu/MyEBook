package com.cqupt.myebook;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private int mWidth, mHeight;
    private BezierPageView bezierPageView;
    private Bitmap mCurPageBitmap, mNextPageBitmap;
    private Canvas mCurPageCanvas, mNextPageCanvas;
    private BezierPageFactory bezierPageFactory;

    private TextView tvProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        mWidth = GetW(this);
        mHeight = GetH(this);
        setContentView(R.layout.activity_main);

        tvProgress = ((TextView) findViewById(R.id.progress));

        bezierPageView = (BezierPageView) findViewById(R.id.pageView);

        mCurPageBitmap = Bitmap.createBitmap(mWidth, mHeight,
                Bitmap.Config.ARGB_8888);
        mNextPageBitmap = Bitmap.createBitmap(mWidth, mHeight,
                Bitmap.Config.ARGB_8888);

        mCurPageCanvas = new Canvas(mCurPageBitmap);
        mNextPageCanvas = new Canvas(mNextPageBitmap);
        bezierPageFactory = new BezierPageFactory(mWidth, mHeight, Color.WHITE, Color.BLACK, 50, 60, 0);

        bezierPageFactory.setUseBg(true);
        bezierPageFactory.setBgBitmap(BitmapFactory.decodeResource(this.getResources(), R.mipmap.theme_9));

        bezierPageFactory.setOnProgressChangedListener(new BezierPageFactory.OnProgressChangedListener() {
            @Override
            public void setProgress(String progress) {
                tvProgress.setText(progress);
            }
        });

        try {
            bezierPageFactory.openBook("/sdcard/test.txt");
            bezierPageFactory.Draw(mCurPageCanvas);
        } catch (IOException e) {
            e.printStackTrace();
        }

        bezierPageView.setBitmaps(mCurPageBitmap, mNextPageBitmap);

        bezierPageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

//                boolean ret = false;
                if (v == bezierPageView) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            bezierPageView.abortAnimation();
                            bezierPageView.bIsShowNoMore = false;
                            bezierPageView.calcCornerXY(event.getX(), event.getY());
                            bezierPageFactory.Draw(mCurPageCanvas);
                            if (bezierPageView.DragToRight()) {
                                try {
                                    bezierPageFactory.prePage();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                if (bezierPageFactory.isfirstPage()) {
                                    bezierPageView.bIsShowNoMore = true;
                                    bezierPageView.invalidate();
                                    return true;
                                }
                                bezierPageFactory.Draw(mNextPageCanvas);
                            } else {
                                try {
                                    bezierPageFactory.nextPage();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                if (bezierPageFactory.islastPage()) {
                                    bezierPageView.bIsShowNoMore = true;
                                    bezierPageView.invalidate();
                                    return true;
                                }
                                bezierPageFactory.Draw(mNextPageCanvas);
                            }
                            bezierPageView.setBitmaps(mCurPageBitmap, mNextPageBitmap);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            break;
                        case MotionEvent.ACTION_UP:
                            if (bezierPageFactory.islastPage()) {
                                Toast.makeText(MainActivity.this, "已经是最后一页", Toast.LENGTH_SHORT).show();
                            } else if (bezierPageFactory.isfirstPage()) {
                                Toast.makeText(MainActivity.this, "已经是第一页", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
//                    return ret;
                }
                return false;
            }
        });
    }

    public int GetW(Activity context) {
        // int screenWidth ;
        DisplayMetrics dm; // = new DisplayMetrics();
        // dm = context.getResources().getDisplayMetrics();

        dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);

        // dm.widthPixels;
        return dm.widthPixels;
    }


    public int GetH(Activity context) {
        // int screenWidth ;
        DisplayMetrics dm; // = new DisplayMetrics();
        // dm = context.getResources().getDisplayMetrics();

        dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);

        // dm.widthPixels;
        return dm.heightPixels;
    }
}
