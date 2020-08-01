package com.example.bookacab;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    public void redirectActivity(){
        if(ParseUser.getCurrentUser().getString("riderOrDriver")=="rider"){
            Intent intent = new Intent(this,RiderActivity.class);
            startActivity(intent);
        }else{
            Intent intent = new Intent(this,ViewRequestsActivity.class);
            startActivity(intent);
        }
    }

    public void getStarted(View view){
        Switch userTypeSwitch = findViewById(R.id.activitySwitch);
        Log.i("Switch Value", String.valueOf(userTypeSwitch.isChecked()));
        String userType = "rider";

        if(userTypeSwitch.isChecked()){
            userType = "driver";
        }
        ParseUser.getCurrentUser().put("riderOrDriver",userType);
        ParseUser.getCurrentUser().saveInBackground();

        Log.i("Redirecting as",userType);
        redirectActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Save the current Installation to Back4App
        ParseInstallation.getCurrentInstallation().saveInBackground();

        getSupportActionBar().hide();

        if(ParseUser.getCurrentUser()==null){
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if(e==null){
                        Log.i("Info","Anonymous Login successful");
                    }else{
                        Log.i("Info","Anonymous Login failed");
                    }
                }
            });
        }else{
            if(ParseUser.getCurrentUser().getString("riderOrDriver") != null){
                Log.i("Redirecting as",ParseUser.getCurrentUser().getString("riderOrDriver"));
                redirectActivity();
            }
        }
    }
}