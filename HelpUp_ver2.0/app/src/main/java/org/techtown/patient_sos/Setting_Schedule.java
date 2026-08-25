package org.techtown.patient_sos;

import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Map;
import android.os.Build;

public class Setting_Schedule extends AppCompatActivity {

    private Spinner daySpinner;
    private TimePicker startTimePicker;
    private TimePicker endTimePicker;
    private EditText scheduleEditText;
    private Button saveButton;
    private ListView scheduleListView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> scheduleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_schedule);

        // 상태 표시줄과 네비게이션 바 색상을 강제로 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.BLACK); // 검정색 상태 표시줄
            window.setNavigationBarColor(Color.BLACK); // 검정색 네비게이션 바
        }

        daySpinner = findViewById(R.id.daySpinner);
        startTimePicker = findViewById(R.id.startTimePicker);
        endTimePicker = findViewById(R.id.endTimePicker);
        scheduleEditText = findViewById(R.id.scheduleEditText);
        saveButton = findViewById(R.id.saveButton);
        scheduleListView = findViewById(R.id.scheduleListView);

        scheduleList = new ArrayList<>();
        //adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scheduleList);
        //scheduleListView.setAdapter(adapter);

        //
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, scheduleList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);

                return view;
            }
        };
        scheduleListView.setAdapter(adapter);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSchedule();
            }
        });

        scheduleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showEditDeleteDialog(position);
            }
        });

        loadSchedules();

        /*
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(spinnerAdapter);
        */

        // Spinner 어댑터 생성 및 텍스트 색상 설정
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.days_of_week)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK); // 선택된 항목 텍스트 색상
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.WHITE); // 드롭다운 항목 텍스트 색상
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(spinnerAdapter);
    }

    private void saveSchedule() {
        String day = daySpinner.getSelectedItem().toString();
        int startHour = startTimePicker.getCurrentHour();
        int startMinute = startTimePicker.getCurrentMinute();
        int endHour = endTimePicker.getCurrentHour();
        int endMinute = endTimePicker.getCurrentMinute();
        String schedule = scheduleEditText.getText().toString();

        if (schedule.isEmpty()) {
            Toast.makeText(this, "일정을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = day + "_" + String.format("%02d:%02d-%02d:%02d", startHour, startMinute, endHour, endMinute);
        String value = schedule;

        SharedPreferences sharedPref = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();

        Toast.makeText(this, "일정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        scheduleEditText.setText("");
        loadSchedules();
    }

    private void loadSchedules() {
        SharedPreferences sharedPref = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPref.getAll();
        scheduleList.clear();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            scheduleList.add(key + ": " + value);
        }

        adapter.notifyDataSetChanged();
    }

    private void showEditDeleteDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("일정 관리")
                .setItems(new CharSequence[]{"수정", "삭제"}, (dialog, which) -> {
                    if (which == 0) {
                        editSchedule(position);
                    } else {
                        deleteSchedule(position);
                    }
                });
        builder.create().show();
    }

    private void editSchedule(int position) {
        String selectedSchedule = scheduleList.get(position);
        String[] parts = selectedSchedule.split(": ", 2);
        String key = parts[0];
        String value = parts[1];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setText(value);
        builder.setTitle("일정 수정")
                .setView(input)
                .setPositiveButton("확인", (dialog, which) -> {
                    String newValue = input.getText().toString();
                    if (!newValue.isEmpty()) {
                        SharedPreferences sharedPref = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(key, newValue);
                        editor.apply();
                        loadSchedules();
                        Toast.makeText(this, "일정이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteSchedule(int position) {
        String selectedSchedule = scheduleList.get(position);
        String key = selectedSchedule.split(": ")[0];
        SharedPreferences sharedPref = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(key);
        editor.apply();
        loadSchedules();
        Toast.makeText(this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private static class ScheduleItem {
        String key;
        String value;

        ScheduleItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
