// MainActivity.java
package com.example.weather_application;
import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DecimalFormat;
import android.animation.ObjectAnimator;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.animation.ObjectAnimator;
import androidx.cardview.widget.CardView;
import android.widget.LinearLayout;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
//import com.example.weather_application.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.Manifest;


import android.animation.ObjectAnimator;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ImageButton locationButton;
    private boolean isSearchContainerMoved = false;
    private EditText editText;
    private TextView cityName, temperature, weatherCondition, humidityText, maxTemperature,
            minTemperature, pressureText, windText;
    private ImageView imageView;
    private LinearLayout searchContainer;
    private CardView weatherCard;
    private final String url = "https://api.openweathermap.org/data/2.5/weather";
    private final String appid = "1438d3e726be8efe5bc1fe1260f47fbf";
    DecimalFormat df = new DecimalFormat("#.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editText = findViewById(R.id.editTextTextPersonName);
        cityName = findViewById(R.id.cityName);
        temperature = findViewById(R.id.temperature);
        weatherCondition = findViewById(R.id.weatherCondition);
        humidityText = findViewById(R.id.humidity);
        maxTemperature = findViewById(R.id.maxTemperature);
        minTemperature = findViewById(R.id.minTemperature);
        pressureText = findViewById(R.id.pressure);
        windText = findViewById(R.id.wind);
        imageView = findViewById(R.id.imageView);

        // Initialize containers for animation
        searchContainer = findViewById(R.id.searchContainer);
        weatherCard = findViewById(R.id.weatherCard);

        //Location
        locationButton = findViewById(R.id.locationButton);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationButton.setOnClickListener(v -> {
            requestLocationPermission();
        });

        // Set up keyboard done action
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                getWeatherDetails(v);
                return true;
            }
            return false;
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Show loading indicator
        ProgressBar progressBar = new ProgressBar(this);
        locationButton.setVisibility(View.INVISIBLE);
        ((ViewGroup) locationButton.getParent()).addView(progressBar);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Remove progress bar and show location button
                        ((ViewGroup) progressBar.getParent()).removeView(progressBar);
                        locationButton.setVisibility(View.VISIBLE);

                        // Get weather for current location
                        getWeatherByLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    // Remove progress bar and show location button
                    ((ViewGroup) progressBar.getParent()).removeView(progressBar);
                    locationButton.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this,
                            "Error getting location", Toast.LENGTH_SHORT).show();
                });
    }

    private void getWeatherByLocation(double latitude, double longitude) {
        String tempUrl = url + "?lat=" + latitude + "&lon=" + longitude + "&appid=" + appid;

        // Move search container to top if not already moved
        if (!isSearchContainerMoved) {
            float moveDistance = searchContainer.getY() -
                    getResources().getDimensionPixelSize(R.dimen.search_container_top_margin);

            ObjectAnimator moveUpAnimation = ObjectAnimator.ofFloat(
                    searchContainer,
                    "translationY",
                    0f,
                    -moveDistance
            );
            moveUpAnimation.setDuration(500);
            moveUpAnimation.setInterpolator(new DecelerateInterpolator());
            moveUpAnimation.start();
            isSearchContainerMoved = true;
        }

        // Make the API call
        StringRequest stringRequest = new StringRequest(Request.Method.GET, tempUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                        JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                        JSONObject jsonObjectWeather = jsonResponse.getJSONArray("weather").getJSONObject(0);
                        String weatherDescription = jsonObjectWeather.getString("description");

                        // Get city name from response
                        String cityNameFromResponse = jsonResponse.getString("name");

                        // Update EditText with city name
                        editText.setText(cityNameFromResponse);

                        // Process temperature data
                        double temp = jsonObjectMain.getDouble("temp") - 273.15;
                        double tempMax = jsonObjectMain.getDouble("temp_max") - 273.15;
                        double tempMin = jsonObjectMain.getDouble("temp_min") - 273.15;

                        // Update UI elements
                        cityName.setText(cityNameFromResponse);
                        temperature.setText(df.format(temp) + "°C");
                        weatherCondition.setText(weatherDescription);
                        humidityText.setText("Humidity: " + jsonObjectMain.getString("humidity") + "%");
                        maxTemperature.setText("Max: " + df.format(tempMax) + "°C");
                        minTemperature.setText("Min: " + df.format(tempMin) + "°C");
                        pressureText.setText("Pressure: " + jsonObjectMain.getString("pressure") + " hPa");
                        windText.setText("Wind: " + jsonObjectWind.getString("speed") + " m/s");

                        // Set weather icon
                        setWeatherIcon(weatherDescription);

                        // Show the weather card with animation
                        weatherCard.setVisibility(View.VISIBLE);
                        weatherCard.setAlpha(0f);
                        weatherCard.animate()
                                .alpha(1f)
                                .setDuration(500)
                                .setStartDelay(300)
                                .start();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                "Error processing weather data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getApplicationContext(),
                        "Error getting weather data", Toast.LENGTH_SHORT).show());

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    public void getWeatherDetails(View view) {
        String tempUrl = "";
        String city = editText.getText().toString().trim();

        if(city.equals("")) {
            Toast.makeText(this, "City field cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        // Clear focus from EditText
        editText.clearFocus();

        // Animate search container to top if not already moved
        if (!isSearchContainerMoved) {
            // Calculate the distance to move up (current Y position minus target Y position)
            float moveDistance = searchContainer.getY() - getResources().getDimensionPixelSize(R.dimen.search_container_top_margin);

            // Create and configure the animation
            ObjectAnimator moveUpAnimation = ObjectAnimator.ofFloat(
                    searchContainer,
                    "translationY",
                    0f,
                    -moveDistance
            );
            moveUpAnimation.setDuration(500); // Duration in milliseconds
            moveUpAnimation.setInterpolator(new DecelerateInterpolator());

            moveUpAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Show weather card with fade in and slide up animation after search container moves
                    weatherCard.setVisibility(View.VISIBLE);
                    weatherCard.setAlpha(0f);
                    weatherCard.setTranslationY(50f); // Start slightly below target position

                    weatherCard.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(400)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            });

            moveUpAnimation.start();
            isSearchContainerMoved = true;
        } else {
            // If search container is already at top, just update the weather card with a refresh animation
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(weatherCard, "alpha", 1f, 0.5f);
            fadeOut.setDuration(200);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(weatherCard, "alpha", 0.5f, 1f);
            fadeIn.setDuration(200);

            AnimatorSet refreshAnimation = new AnimatorSet();
            refreshAnimation.playSequentially(fadeOut, fadeIn);
            refreshAnimation.start();
        }

        tempUrl = url + "?q=" + city + "&appid=" + appid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, tempUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                    JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                    JSONObject jsonObjectWeather = jsonResponse.getJSONArray("weather").getJSONObject(0);

                    String currentTemp = jsonObjectMain.getString("temp");
                    String humidityValue = jsonObjectMain.getString("humidity");
                    String pressureValue = jsonObjectMain.getString("pressure");
                    String windSpeed = jsonObjectWind.getString("speed");
                    String weatherDescription = jsonObjectWeather.getString("description");
                    String maxTemp = jsonObjectMain.getString("temp_max");
                    String minTemp = jsonObjectMain.getString("temp_min");

                    // Convert temperatures from Kelvin to Celsius
                    double temp = Double.parseDouble(currentTemp) - 273.15;
                    double tempMax = Double.parseDouble(maxTemp) - 273.15;
                    double tempMin = Double.parseDouble(minTemp) - 273.15;

                    cityName.setText(city);
                    temperature.setText(df.format(temp) + "°C");
                    weatherCondition.setText(weatherDescription);
                    humidityText.setText("Humidity: " + humidityValue + "%");
                    maxTemperature.setText("Max: " + df.format(tempMax) + "°C");
                    minTemperature.setText("Min: " + df.format(tempMin) + "°C");
                    pressureText.setText("Pressure: " + pressureValue + " hPa");
                    windText.setText("Wind: " + windSpeed + " m/s");

                    setWeatherIcon(weatherDescription);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),
                        "Error: " + error.toString().trim(), Toast.LENGTH_SHORT).show();
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    private void setWeatherIcon(String weatherDescription) {
        if (weatherDescription.contains("clear")) {
            imageView.setImageResource(R.drawable.clear);
        } else if (weatherDescription.contains("cloud")) {
            imageView.setImageResource(R.drawable.cloudy);
        } else if (weatherDescription.contains("rain")) {
            imageView.setImageResource(R.drawable.rain);
        } else if (weatherDescription.contains("snow")) {
            imageView.setImageResource(R.drawable.snow);
        } else if (weatherDescription.contains("mist")) {
            imageView.setImageResource(R.drawable.mist);
        } else if (weatherDescription.contains("haze")) {
            imageView.setImageResource(R.drawable.haze);
        } else {
            imageView.setImageResource(R.drawable.default_weather);
        }
    }
}