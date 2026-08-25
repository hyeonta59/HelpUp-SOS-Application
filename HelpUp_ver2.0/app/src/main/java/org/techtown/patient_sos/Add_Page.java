package org.techtown.patient_sos;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

class Contact {
    private String name;
    private String phone;

    public Contact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return name + " - " + phone;
    }
}

public class Add_Page extends AppCompatActivity {
    private ArrayList<Contact> contacts = new ArrayList<>();
    private ArrayAdapter<Contact> adapter;
    private static final String PREFS_NAME = "contacts_prefs";
    private static final String CONTACTS_KEY = "contacts_list";
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_page);

        // 상태 표시줄과 네비게이션 바 색상을 강제로 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.BLACK); // 검정색 상태 표시줄
            window.setNavigationBarColor(Color.BLACK); // 검정색 네비게이션 바
        }

        EditText nameInput = findViewById(R.id.name);
        EditText phoneInput = findViewById(R.id.number);
        EditText searchContact = findViewById(R.id.Contact);
        ListView contactListView = findViewById(R.id.contactListView);
        Button addButton = findViewById(R.id.btn);

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, contacts) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);

                return view;
            }
        };
        contactListView.setAdapter(adapter);

        loadContacts();

        //추가버튼
        addButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString();
            String phone = phoneInput.getText().toString();

            if (!name.isEmpty() && !phone.isEmpty()) {
                Contact newContact = new Contact(name, phone);
                contacts.add(newContact);
                adapter.notifyDataSetChanged();
                saveContacts();
                nameInput.setText("");
                phoneInput.setText("");
            } else {
                Toast.makeText(this, "이름과 전화번호를 입력하세요", Toast.LENGTH_SHORT).show();
            }
        });

        searchContact.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        contactListView.setOnItemClickListener((parent, view, position, id) -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 500) {
                showDeleteConfirmationDialog(position);
            }
            lastClickTime = currentTime;
        });
    }

    private void showDeleteConfirmationDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("연락처 삭제")
                .setMessage("연락처를 삭제하겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    contacts.remove(position);
                    adapter.notifyDataSetChanged();
                    saveContacts();
                    Toast.makeText(this, "연락처가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("아니요", null)
                .show();
    }

    private void saveContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        StringBuilder sb = new StringBuilder();
        for (Contact contact : contacts) {
            sb.append(contact.getName()).append(",").append(contact.getPhone()).append(";");
        }

        editor.putString(CONTACTS_KEY, sb.toString());
        editor.apply();
    }

    private void loadContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String contactsString = sharedPreferences.getString(CONTACTS_KEY, "");

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

        adapter.notifyDataSetChanged();
    }
}
