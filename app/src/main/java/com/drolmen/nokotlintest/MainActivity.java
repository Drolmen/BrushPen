package com.drolmen.nokotlintest;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    private PorterDuffView mPorterDuffView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPorterDuffView = findViewById(R.id.porter_view);
    }


    public void onClear(View view) {
        mPorterDuffView.clear();
    }

    public void onAlphaClick(View view) {
        mPorterDuffView.setOrCancleAlpha();
    }

    public void onColorClick(View view) {
        switch (view.getId()) {
            case R.id.redBtn:
                mPorterDuffView.setColor(Color.RED);
                break;
            case R.id.yelloBtn:
                mPorterDuffView.setColor(Color.YELLOW);
                break;
            case R.id.blueBtn:
                mPorterDuffView.setColor(Color.BLUE);
                break;
        }
    }
}
