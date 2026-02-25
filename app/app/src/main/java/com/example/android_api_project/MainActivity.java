package com.example.android_api_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class MainActivity extends AppCompatActivity {
    EditText name, email, qr_input;
    Button submit, generate_qr_btn, send_files_btn;
    TextView response;
    ImageButton refresh_btn;
    ImageView image;
    ProgressBar image_progress;
    String test_data = "prottoy";


    static String host = "192.168.1.138:8000";
    static String url = "http://" + host + "/post_data/";
    static String get_url = "http://" + host + "/get_data";
    static String image_url = "https://i.ibb.co.com/B52wDW1N/Screenshot-2025-09-06-040525.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        submit = findViewById(R.id.submit);
        refresh_btn = findViewById(R.id.refresh_button);
        response = findViewById(R.id.response);
        image = findViewById(R.id.image);
        qr_input = findViewById(R.id.qr_input);
        generate_qr_btn = findViewById(R.id.generate_qr_btn);
        send_files_btn = findViewById(R.id.send_files_btn);

        get_data();
        //get_image();

        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                response.setText("Loading ...");
                response.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                get_data();
                //get_image();
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(name.getText().toString().isEmpty() || email.getText().toString().isEmpty()){
                    Toast toast = Toast.makeText(getApplicationContext(), "Please fill all the fields", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                set_data(name.getText().toString(), email.getText().toString());
                name.setText("");
                email.setText("");
            }
        });

        generate_qr_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generate_qr_code(qr_input.getText().toString());
            }
        });

        send_files_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, send_files.class);
                intent.putExtra("test_data", test_data);
                startActivity(intent);
            }
        });
    }

    private void generate_qr_code(String text) {
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter some text", Toast.LENGTH_SHORT).show();
            image.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.warning));
            image.setMaxWidth(20);
            image.setMaxHeight(20);

            return;
        }

        text = text.trim();

        QRGEncoder qrgEncoder = new QRGEncoder(text, null, QRGContents.Type.TEXT, 200);
        qrgEncoder.setColorBlack(Color.BLACK);
        qrgEncoder.setColorWhite(Color.WHITE);
        try {
            Bitmap bitmap = qrgEncoder.getBitmap(2);
            image.setImageBitmap(bitmap);
        } catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            image.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.warning));
            image.setMaxWidth(20);
            image.setMaxHeight(20);
            return;
        }
    }

    void get_data(){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, get_url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response_data) {
                        try {

                            String status = response_data.getString("status");

                            if (status.equals("success")) {

                                JSONArray dataArray = response_data.getJSONArray("data");
                                if (dataArray.length() == 0){
                                    response.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.yellow));
                                    response.setText("No data found");
                                    return;
                                }
                                StringBuilder formattedData = new StringBuilder();

                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject userObject = dataArray.getJSONObject(i);

                                    String name = userObject.getString("name");
                                    String email = userObject.getString("email");
                                    formattedData.append("Name: ").append(name).append(", Email: ").append(email).append("\n\n");
                                }

                                response.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                                response.setText(formattedData.toString());

                            } else {

                                Toast toast = Toast.makeText(getApplicationContext(), "Error: " + status, Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } catch (JSONException e) {
                            response.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                            response.setText("JSON parsing error: " + e.toString());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        MainActivity.this.response.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                        MainActivity.this.response.setText("Error: " + error.toString());
                    }
                });


        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(jsonObjectRequest);
        response.setVisibility(View.VISIBLE);
    }

    void set_data(final String name, final String email){
        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        try {
                            JSONObject jsonResponse = new JSONObject(s);
                            String status = jsonResponse.getString("status");
                            String message = jsonResponse.getString("message");

                            if(status.equals("success")) {
                                get_data();
                            }
                            Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                            toast.show();

                        } catch (JSONException e) {
                            response.setText("Something went wrong on response : " + e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        response.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                        response.setText("Error : " + volleyError.toString());
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("name", name);
                map.put("email", email);
                return map;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request);
    }

    void get_image(){
        image_progress.setProgress(100);

        int max_width = 0;
        int max_height = 0;

        ImageRequest imageRequest = new ImageRequest(image_url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        image.setImageBitmap(bitmap);
                    }
                },

                max_width, max_height, null,

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        image.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.warning));
                        image.setMaxWidth(20);
                        image.setMaxHeight(20);
                        Toast toast = Toast.makeText(getApplicationContext(), "image error", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
        );

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(imageRequest);
        image.setVisibility(View.VISIBLE);
        image_progress.setVisibility(View.INVISIBLE);
    }
}
