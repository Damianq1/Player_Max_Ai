package com.playermaxai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        TextView tv = new TextView(this);
        tv.setText("🎬 ULTIMATE PLAYER

APLIKACJA DZIAŁA!

Gotowy na LibVLC");
        tv.setTextSize(24);
        
        layout.addView(tv);
        setContentView(layout);
    }
}
