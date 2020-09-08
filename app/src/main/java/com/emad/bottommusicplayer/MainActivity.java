package com.emad.bottommusicplayer;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.emad.music_lib.MusicSelectorDialog;
import com.emad.music_lib.MusicViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.frame2).setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.tvSelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicSelectorDialog dialog = new MusicSelectorDialog(MainActivity.this, new ViewModelProvider(MainActivity.this).get(MusicViewModel.class));
                dialog.show();
            }
        });

        getSupportFragmentManager().beginTransaction().add(new MusicSelectFragment(), null).commit();
    }
}
