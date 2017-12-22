package com.myb.livevideo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.myb.rtmppush.ui.PushActivity;

public class VideoListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        final String email = intent.getStringExtra("email");

        setContentView(R.layout.activity_video_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(email);
        toolbar.setSubtitle("detail info here.");
        toolbar.setLogo(R.drawable.user_snap);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        PermissionChecker.requestPermission(this);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!PermissionChecker.checkPermissions(VideoListActivity.this)){
                    Toast.makeText(VideoListActivity.this,
                        "请开启摄像头，录音等权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(VideoListActivity.this, PushActivity.class);
                // use user name to push rtmp.
                intent.putExtra("push_url", AppConfig.RTMP_PUSH_URL_BASE + "/hls/" + email);
                startActivity(intent);
            }
        });
    }
}
