package org.techtown.patient_sos;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Vibrator;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static final int SMS_PERMISSION_CODE = 1;
    private static final int LOCATION_PERMISSION_CODE =2;
    public static final int REQUEST_CODE_MENU=101;
    private static final int SETTINGS_REQUEST_CODE = 1;
    private static final int SETTING_ACCELERATION_REQUEST_CODE=2;
    private static final int SETTING_MESSAGE_REQUEST_CODE=2;
    private SensorManager sensorManager;
    private TextToSpeech textToSpeech;
    private boolean isHeartPatientSelected = false;
    private boolean isBreathPatientSelected = false;
    private  boolean isExercisePatientSelected = false;
    private  boolean isEyesPatientSelected = false;
    private long lastShakeTime = 0;
    private CountDownTimer countDownTimer;
    private Dialog sosDialog;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_SELECTED_RADIO_BUTTON = "selectedRadioButtonId";
    private Vibrator vibrator;
    private FusedLocationProviderClient fusedLocationClient;
    private int sensitivity; // 민감도 값 저장용 변수
    private int time;
    private String userInput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 메인 레이아웃 참조
        ConstraintLayout mainLayout = findViewById(R.id.main); // "main"은 XML에서 정의된 루트 ConstraintLayout의 ID입니다.

        // 메인 레이아웃에 터치 리스너 추가
        mainLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // 키보드 닫기 및 커서 해제
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    getCurrentFocus().clearFocus();
                }
            }
            return false; // 다른 터치 이벤트는 계속 전달
        });

        // 상태 표시줄과 네비게이션 바 색상을 강제로 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.BLACK); // 검정색 상태 표시줄
            window.setNavigationBarColor(Color.BLACK); // 검정색 네비게이션 바
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 센서 매니저, 가속도 센서 변수
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);

        // TTS 초기화
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.KOREAN);

                // TTS 초기화 후에만 실행
                if (isEyesPatientSelected) {
                    textToSpeech.speak("HelpUp어플이 실행되었습니다.. SOS실행을 원하실 경우 휴대폰을 흔들어주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });

        if (isEyesPatientSelected) {
            textToSpeech.speak("HelpUp어플이 실행되었습니다.. SOS실행을 원하실 경우 휴대폰을 흔들어주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        //저장된 환자 상태 변수
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // SharedPreferences에서 민감도 값 불러오기
        SharedPreferences sharedPreferences_sens = getSharedPreferences("settings", MODE_PRIVATE);
        sensitivity = sharedPreferences_sens.getInt("sensitivity", 10); // 기본값 10

        // SharedPreferences에서 타이머 값 불러오기
        SharedPreferences sharedPreferences_timer = getSharedPreferences("settings", MODE_PRIVATE);
        time = sharedPreferences_timer.getInt("timer", 10); // 기본값 10


        // 저장된 환자 상태 불러오기
        int savedRadioButtonId = sharedPreferences.getInt(KEY_SELECTED_RADIO_BUTTON, -1);
        handleSelectedPatientType(savedRadioButtonId);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //연락처 추가 화면 열기 버튼
        ImageButton img2 = findViewById(R.id.imageButton2);
        img2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Add_Page.class);
                startActivityForResult(intent, SETTINGS_REQUEST_CODE); // SettingPageActivity 시작
            }
        });

        //설정화면 열기 버튼
        ImageButton img10 = findViewById(R.id.imageButton10);
        img10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Setting_main.class);
                startActivityForResult(intent, SETTINGS_REQUEST_CODE); // SettingPageActivity 시작
            }
        });

        //상세위치 클릭 리스너 등록
        Button button4 = findViewById(R.id.button4);
        TextInputEditText textField = findViewById(R.id.textfield);

        button4.setOnClickListener(v -> {
            userInput = textField.getText() != null ? textField.getText().toString().trim() : "";

            if (userInput.isEmpty()) {
                // 텍스트가 비어있으면 "상세위치가 삭제되었습니다"
                Toast.makeText(MainActivity.this, "상세위치가 삭제되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                // 텍스트가 존재하면 "상세위치가 저장되었습니다"
                Toast.makeText(MainActivity.this, "상세위치가 저장되었습니다", Toast.LENGTH_SHORT).show();
            }
        });
        
        //SOS버튼------------------------------------------------------------------------------------
        ImageButton img = findViewById(R.id.SOS_Button);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 위치 권한 확인
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
                } else {
                    getCurrentLocation(); // 위치 가져오기
                }

                // SMS 권한 확인
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        sensitivity = sharedPreferences.getInt("sensitivity", 10);  // 기본값 10

        SharedPreferences sharedPreferences_timer = getSharedPreferences("settings", MODE_PRIVATE);
        time = sharedPreferences_timer.getInt("timer", 10); // 기본값 10

        // SharedPreferences에서 메시지 값 불러오기
        //SharedPreferences sharedPreferences_message = getSharedPreferences("message", MODE_PRIVATE);
        //msg = sharedPreferences_message.getString("message", ""); // 기본값 10
    }

    // 현재 위치를 가져오는 메서드-----------------------------------------------------------------------
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            getAddressFromLocation(location); // 위치 정보와 함께 SMS 전송
                        }
                    }
                });
    }

    // 위도와 경도를 주소로 변환하는 메서드---------------------------------------------------------------
    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // 기본 주소 (시, 구, 동)
                String city = address.getAdminArea();  // 예: 대전광역시
                String district = address.getLocality();  // 예: 서구
                if (district == null) {
                    district = address.getSubLocality();  // 서구 대신 서구의 하위 지역을 확인
                }

                // 도로명 주소 (도로명과 건물번호)
                String thoroughfare = address.getThoroughfare(); // 도로명
                String subThoroughfare = address.getSubThoroughfare(); // 건물 번호

                // 도로명 주소가 없을 경우를 대비해 전체 주소라인을 가져옴
                String detailedAddress = "";
                if (thoroughfare != null) {
                    detailedAddress += thoroughfare + " ";
                }
                if (subThoroughfare != null) {
                    detailedAddress += subThoroughfare;
                }

                // 도로명 주소와 건물번호가 없으면 기본 주소라인을 사용
                if (detailedAddress.isEmpty()) {
                    detailedAddress = address.getAddressLine(0); // 전체 주소를 기본값으로 사용
                }

                // 기본 주소 (시, 구, 동)와 상세 주소 합치기
                String fullAddress = city + " " + district + " " + detailedAddress;

                sendSMSWithLocation(fullAddress); // SMS로 상세 주소 전송
            } else {
                Toast.makeText(MainActivity.this, "주소를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "주소를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 연락처를 불러오는 메서드-------------------------------------------------------------------------
    private ArrayList<Contact> loadContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences("contacts_prefs", MODE_PRIVATE);
        String contactsString = sharedPreferences.getString("contacts_list", "");
        ArrayList<Contact> contacts = new ArrayList<>();

        if (!contactsString.isEmpty()) {
            String[] contactArray = contactsString.split(";");
            for (String contactData : contactArray) {
                if (!contactData.isEmpty()) {
                    String[] data = contactData.split(",");
                    if (data.length == 2) {
                        contacts.add(new Contact(data[0], data[1]));
                    }
                }
            }
        }
        return contacts;
    }

    // 긴급 메시지 전송 기능---------------------------------------------------------------------------
    private void sendSMSWithLocation(String address) {
        ArrayList<Contact> contacts = loadContacts();  // 저장된 연락처 목록을 불러옴

        // SharedPreferences에서 메시지 불러오기
        SharedPreferences sharedPreferences_message = getSharedPreferences("MessagePreferences", MODE_PRIVATE);
        String msg = sharedPreferences_message.getString("message", ""); // 기본값 ""

        Log.d("DEBUG", "msg: [" + msg + "]");

        if (contacts.isEmpty()) {
            Toast.makeText(this, "긴급 연락처가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 현재 시간 가져오기
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // 현재 요일 한글로 변환
        String currentDay = getKoreanDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK));
        Log.d("DEBUG", "현재 요일 (한글): " + currentDay + ", 현재 시간: " + currentHour + ":" + currentMinute);

        // 스케줄 불러오기
        SharedPreferences schedulePrefs = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = schedulePrefs.getAll();

        // 최종 메시지 생성
        String message = "현재 위치: " + address ;

        // userInput이 null이 아니고 빈 텍스트가 아닌 경우에만 추가
        if (userInput != null && !userInput.isEmpty()) {
            message += "\n상세위치: " + userInput;
        }
        else {
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();

                Log.d("DEBUG", "검사 중인 키: " + key);

                // 키에서 요일과 시간대 추출
                String[] keyParts = key.split("_");
                if (keyParts.length != 2) continue; // 형식이 맞지 않으면 건너뜀
                String keyDay = keyParts[0];

                // 요일 비교
                if (!keyDay.equals(currentDay)) {
                    Log.d("DEBUG", "요일 불일치, 건너뜀: " + key);
                    continue;
                }

                // 시간대 비교
                String[] timeRange = keyParts[1].split("-");
                if (timeRange.length != 2) continue; // 형식이 맞지 않으면 건너뜀
                String[] startTime = timeRange[0].split(":");
                String[] endTime = timeRange[1].split(":");

                int startHour = Integer.parseInt(startTime[0]);
                int startMinute = Integer.parseInt(startTime[1]);
                int endHour = Integer.parseInt(endTime[0]);
                int endMinute = Integer.parseInt(endTime[1]);

                if (currentHour > startHour || (currentHour == startHour && currentMinute >= startMinute)) {
                    if (currentHour < endHour || (currentHour == endHour && currentMinute <= endMinute)) {
                        Log.d("DEBUG", "시간대 일치, 메시지 추가: " + value);
                        message += "\n" + "현재일정:" + value;
                    } else {
                        Log.d("DEBUG", "시간 초과, 건너뜀: " + key);
                    }
                } else {
                    Log.d("DEBUG", "시간 미달, 건너뜀: " + key);
                }
            }
        }
        message+= "\n" + msg;
        Log.d("DEBUG", "최종 메시지: " + message);

        // 각 연락처로 메시지 전송
        for (Contact contact : contacts) {
            sendSMS(contact.getPhone(), message, address); // 각 연락처로 메시지 전송
        }
    }

    // 요일 숫자를 한글로 변환
    private String getKoreanDayOfWeek(int dayOfWeek) {
        String[] days = {"일", "월", "화", "수", "목", "금", "토"};
        return days[dayOfWeek - 1]; // dayOfWeek는 1~7 범위
    }

    //스케줄 범위 확인---------------------------------------------------------------------------------
    private boolean isTimeWithinRange(int currentHour, int currentMinute, int startHour, int startMinute, int endHour, int endMinute) {
        if (currentHour < startHour || (currentHour == startHour && currentMinute < startMinute)) {
            return false;
        }
        if (currentHour > endHour || (currentHour == endHour && currentMinute > endMinute)) {
            return false;
        }
        return true;
    }

    //메세지 보내기 기능-------------------------------------------------------------------------------
    private void sendSMS(String phoneNumber, String message, String address) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "메시지가 자동으로 전송되었습니다.", Toast.LENGTH_SHORT).show();
            if(isEyesPatientSelected)
            {
                textToSpeech.speak("SOS기능이 작동했습니다. 현재 위치는"+address+"입니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } catch (Exception e) {
            Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //메세지 권한-------------------------------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 위치 권한도 요청

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //sendSMS("01012345678","권한");
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                }
            } else {
                Toast.makeText(this, "SMS 전송 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 100) { // 위치 권한 요청 처리
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //sendSMS("01012345678","권한");
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //환자 모드 받아오기-------------------------------------------------------------------------------
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                int selectedPatientType = data.getIntExtra("selectedPatientType", -1);
                handleSelectedPatientType(selectedPatientType);
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

    //기능구현 ---------------------------------------------------------------------------------------
    private void handleSelectedPatientType(int selectedPatientType) {

        if (selectedPatientType==R.id.Patient_Heart_RadioButton) {

            isHeartPatientSelected=selectedPatientType==R.id.Patient_Heart_RadioButton;
            isBreathPatientSelected=false;
            isExercisePatientSelected=false;
            isEyesPatientSelected=false;
            Toast.makeText(this, "심근경색 모드 실행", Toast.LENGTH_SHORT).show();
        }
        else if (selectedPatientType==R.id.Patient_Breath_RadioButton) {

            isBreathPatientSelected=selectedPatientType==R.id.Patient_Breath_RadioButton;
            isHeartPatientSelected=false;
            isExercisePatientSelected=false;
            isEyesPatientSelected=false;
            Toast.makeText(this, "천식발작 모드 실행", Toast.LENGTH_SHORT).show();
        }
        else if (selectedPatientType == R.id.Patient_Exercise_RadioButton) {

            isExercisePatientSelected=selectedPatientType == R.id.Patient_Exercise_RadioButton;
            isHeartPatientSelected=false;
            isBreathPatientSelected=false;
            isEyesPatientSelected=false;
            Toast.makeText(this, "운동기능 모드 실행", Toast.LENGTH_SHORT).show();
            showSOSDialog();

        }
        else if (selectedPatientType == R.id.Patient_Eyes_RadioButton) {

            isEyesPatientSelected=selectedPatientType == R.id.Patient_Eyes_RadioButton;
            isExercisePatientSelected=false;
            isHeartPatientSelected=false;
            isBreathPatientSelected=false;
            Toast.makeText(this, "시각장애 모드 실행", Toast.LENGTH_SHORT).show();
            textToSpeech.speak("HelpUp어플이 실행되었습니다.. SOS실행을 원하실 경우 휴대폰을 흔들어주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    //가속도 감지-------------------------------------------------------------------------------------
    public void onSensorChanged(SensorEvent event) {

        //심근경색 환자일때
        if (isHeartPatientSelected && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            long currentTime = System.currentTimeMillis();

            if (acceleration > sensitivity) {
                if (currentTime - lastShakeTime > 2000) {
                    lastShakeTime = currentTime;

                    showSOSDialog();  // 다이얼로그 호출
                }
            }
        }

        //천식발작환자일떄
        else if(isBreathPatientSelected && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            long currentTime = System.currentTimeMillis();

            if (acceleration > sensitivity) {
                if (currentTime - lastShakeTime > 2000) {
                    lastShakeTime = currentTime;

                    showSOSDialog();  // 다이얼로그 호출
                }
            }
        }

        //시각장애환자일때
        else if (isEyesPatientSelected && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            long currentTime = System.currentTimeMillis();

            if (acceleration > 12) {
                if (currentTime - lastShakeTime > 2000) {
                    lastShakeTime = currentTime;

                    // 휴대폰 흔들림 감지 시 위치를 가져와 SMS 전송
                    getCurrentLocation();  // 위치를 가져온 후 SMS 전송
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //tts 설정---------------------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        // Release resources
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    // SOS 다이얼로그 생성-----------------------------------------------------------------------------
    private void showSOSDialog() {

        vibrator=(Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if(vibrator != null && vibrator.hasVibrator())
        {
            vibrator.vibrate(1000000);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(time+"초 후 SOS가 작동합니다.")
                .setCancelable(false);

        // 취소 버튼 추가
        builder.setNegativeButton("취소", (dialog, which) -> {
            // 타이머 중지 및 다이얼로그 닫기
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            sosDialog.dismiss();

            if(vibrator !=  null)
            {
                vibrator.cancel();
            }
        });

        sosDialog = builder.create();
        sosDialog.show();

        // 타이머 시작
        countDownTimer = new CountDownTimer(time*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                // 10초 만료 시 SOS 다이얼로그 닫기, SMS 전송 및 TTS 출력 반복 시작
                sosDialog.dismiss();

                if(vibrator != null)
                {
                    vibrator.cancel();
                }
                sendSOSMessage();

                if(isHeartPatientSelected || isBreathPatientSelected)
                {
                    startTTSRepeatingMessage(); // TTS 반복 시작
                }
            }
        }.start();
    }

    // SOS 메시지 전송 및 대처법 다이얼로그 호출----------------------------------------------------------
    private void startTTSRepeatingMessage() {
        // 10초마다 음성을 반복하는 타이머
        if(isHeartPatientSelected) {
            countDownTimer = new CountDownTimer(Long.MAX_VALUE, 4000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (textToSpeech != null) {
                        textToSpeech.speak("심근경색 환자 발생, 도움이 필요합니다", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                @Override
                public void onFinish() {
                }
            }.start();
        }

        else if(isBreathPatientSelected){
            countDownTimer = new CountDownTimer(Long.MAX_VALUE, 4000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (textToSpeech != null) {
                        textToSpeech.speak("천식발작 환자 발생, 도움이 필요합니다", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                @Override
                public void onFinish() {
                }
            }.start();
        }

        else if(isEyesPatientSelected)
        {
            if (textToSpeech != null) {
            textToSpeech.speak("HelpUp어플이 실행되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        }
    }

    // SOS 메세지 전송 및 TTS 출력---------------------------------------------------------------------
    private void sendSOSMessage() {
        getCurrentLocation();
        if(isHeartPatientSelected || isBreathPatientSelected) {
            showPatientHeartMethodDialog();  // 대처법 다이얼로그 호출
        }
    }

    // 심근경색 대처법 다이얼로그 ----------------------------------------------------------------------
    private void showPatientHeartMethodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(isHeartPatientSelected)
        {
            builder.setMessage("심근경색 대처법 보기");
        }
        else if(isBreathPatientSelected)
        {
            builder.setMessage("천식발작 대처법 보기");
        }

        // 대처법 보기 버튼 추가-----------------------------------------------------------------------
        builder.setPositiveButton("보기", (dialog, which) -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();  // TTS 반복 중지
            }
            if (textToSpeech != null) {
                textToSpeech.stop(); // TTS 종료
            }
            if(isHeartPatientSelected) {
                Intent intent = new Intent(MainActivity.this, PatientHeart_Method.class);
                startActivity(intent);  // 새로운 액티비티로 이동
            }
            else if(isBreathPatientSelected) {
                Intent intent = new Intent(MainActivity.this, PatientBreath_Method.class);
                startActivity(intent);  // 새로운 액티비티로 이동
            }
        });
        Dialog careDialog = builder.create();
        careDialog.show();
    }
}
