package org.techtown.patient_sos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Setting_main extends AppCompatActivity {
    private static final int SETTINGS_PAGE_REQUEST_CODE = 1;
    private static final int SETTING_ACCELERATION_REQUEST_CODE=2;
    private static final int SETTING_MESSAGE_REQUEST_CODE=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting_main);

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

        //환자선택화면 열기 버튼
        ImageButton img3 = findViewById(R.id.imageButton3);
        img3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Setting_main.this, Setting_Page.class);
                startActivityForResult(intent, SETTINGS_PAGE_REQUEST_CODE); // SettingPageActivity 시작
            }
        });

        //메세지 설정화면
        ImageButton img4 = findViewById(R.id.imageButton4);
        img4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Setting_main.this, setting_Message.class);
                startActivity(intent); // SettingPageActivity 시작
            }
        });

        //가속도 설정화면
        ImageButton img8 = findViewById(R.id.imageButton8);
        img8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Setting_main.this, Setting_AccelerationSensor.class);
                startActivityForResult(intent,SETTING_ACCELERATION_REQUEST_CODE); // SettingPageActivity 시작
            }
        });

        //타이머 설정화면
        ImageButton img9 = findViewById(R.id.imageButton9);
        img9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Setting_main.this, Setting_Timer.class);
                startActivity(intent); // SettingPageActivity 시작
            }
        });

        //스케줄 설정화면
        ImageButton img11 = findViewById(R.id.imageButton11);
        img11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Setting_main.this, Setting_Schedule.class);
                startActivity(intent); // SettingPageActivity 시작
            }
        });
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_PAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Setting_Page에서 선택한 값을 MainActivity로 전달
            if (data != null) {
                int selectedPatientType = data.getIntExtra("selectedPatientType", -1);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("selectedPatientType", selectedPatientType);
                setResult(RESULT_OK, returnIntent);
            }
        }

        else if (requestCode == SETTING_ACCELERATION_REQUEST_CODE && resultCode == RESULT_OK) {
            int sensitivity = data.getIntExtra("sensitivity", 10);
        }

        else if (requestCode == SETTING_MESSAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null)
            {
                String message = data.getStringExtra("message");
            }
        }
    }

    public void onBackPressed() {
        super.onBackPressed();  // 기본 뒤로가기 동작 수행 (메인 화면으로 돌아갑니다)
    }
}