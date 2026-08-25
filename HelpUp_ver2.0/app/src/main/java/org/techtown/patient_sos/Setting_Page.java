package org.techtown.patient_sos;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Setting_Page extends AppCompatActivity {

    private RadioGroup radioGroup;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_SELECTED_RADIO_BUTTON = "selectedRadioButtonId";

    public static final int Patient_Heart_RadioButton = R.id.Patient_Heart_RadioButton;
    public static final int Patient_Breath_RadioButton = R.id.Patient_Breath_RadioButton;
    public static final int Patient_Exercise_RadioButton = R.id.Patient_Exercise_RadioButton;
    public static final int Patient_Eyes_RadioButton = R.id.Patient_Eyes_RadioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting_page);

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

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 라디오 그룹 초기화
        radioGroup = findViewById(R.id.radioGroup);

        // 저장된 라디오 버튼 상태 복원
        int savedRadioButtonId = sharedPreferences.getInt(KEY_SELECTED_RADIO_BUTTON, -1);
        if (savedRadioButtonId != -1) {
            RadioButton savedRadioButton = findViewById(savedRadioButtonId);
            if (savedRadioButton != null) {
                savedRadioButton.setChecked(true);
            }
        }

        // 라디오 버튼 선택 시 선택 상태 저장
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_SELECTED_RADIO_BUTTON, checkedId);
            editor.apply();
        });
    }

    public void onBackPressed () {
        Intent returnIntent = new Intent();
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId != -1) {
            returnIntent.putExtra("selectedPatientType", checkedId);
            setResult(RESULT_OK, returnIntent);
        }
        super.onBackPressed(); // 기본 동작 수행
    }
}