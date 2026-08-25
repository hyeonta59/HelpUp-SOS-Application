package org.techtown.patient_sos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Setting_AccelerationSensor extends AppCompatActivity {

    private SeekBar sensitivitySeekBar;
    private TextView sensitivityValue;
    private Button saveButton;
    private int sensitivity = 0; // 초기값 설정

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting_acceleration_sensor);

        // 상태 표시줄과 네비게이션 바 색상을 강제로 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.BLACK); // 검정색 상태 표시줄
            window.setNavigationBarColor(Color.BLACK); // 검정색 네비게이션 바
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sensitivitySeekBar = findViewById(R.id.seekBar);
        sensitivityValue = findViewById(R.id.textView4);

        // SharedPreferences에서 저장된 민감도 값 불러오기
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        sensitivity = sharedPreferences.getInt("sensitivity", 0); // 기본값 10
        sensitivitySeekBar.setProgress(sensitivity); // SeekBar에 초기값 설정
        sensitivityValue.setText("민감도: " + sensitivity); // 민감도 값 텍스트에 설정

        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivity = progress;
                sensitivityValue.setText("민감도: " + sensitivity);

                // 값이 바뀔 때마다 SharedPreferences에 저장
                SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("sensitivity", sensitivity);
                editor.apply(); // 즉시 저장
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("sensitivity", sensitivity);
        setResult(RESULT_OK, returnIntent);  // 설정 화면으로 값 전달
        finish();  // 설정 화면으로 돌아가면서 종료
    }
}