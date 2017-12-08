package com.drolmen.nokotlintest;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PorterDuffView porterDuffView = new PorterDuffView(this);
        FrameLayout contentView = new FrameLayout(this);
        contentView.addView(porterDuffView, -1, -1);
        Button button = new Button(this);
        button.setText("CLEAR");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                porterDuffView.clear();
            }
        });
        contentView.addView(button, 300, 200);
        setContentView(contentView);
    }
}
