package org.techtown.patient_sos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.SharedPreferences;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class setting_Message extends AppCompatActivity {

    private EditText message1;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting_message);

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

        message1 = findViewById(R.id.et_message);
        Button btn1 = findViewById(R.id.btn);

        sharedPreferences = getSharedPreferences("MessagePreferences", MODE_PRIVATE);

        // 저장된 메시지 불러오기
        message1.setText(sharedPreferences.getString("message", ""));

        // 저장 버튼 클릭
        btn1.setOnClickListener(v -> {
            String message = message1.getText().toString();
            sharedPreferences.edit().putString("message", message).apply();
            Toast.makeText(this, "메시지가 저장되었습니다.", Toast.LENGTH_SHORT).show();

            Intent returnIntent = new Intent();
            returnIntent.putExtra("message", message);
            setResult(RESULT_OK, returnIntent);  // 설정 화면으로 값 전달
            finish();  // 설정 화면으로 돌아가면서 종료
        });

    }
}