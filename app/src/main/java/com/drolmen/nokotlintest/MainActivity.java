package com.drolmen.nokotlintest;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout contentView = new LinearLayout(this);
        final PorterDuffView porterDuffView = new PorterDuffView(this);

        Button button = new Button(this);
        button.setText("CLEAR");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                porterDuffView.clear();
            }
        });
        contentView.addView(button);
        Button alphaControl = new Button(this);
        alphaControl.setText("透明度");
        alphaControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                porterDuffView.setOrCancleAlpha();
            }
        });
        contentView.addView(alphaControl);

        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.addView(porterDuffView, -1, -1);

        setContentView(contentView);
    }
}
