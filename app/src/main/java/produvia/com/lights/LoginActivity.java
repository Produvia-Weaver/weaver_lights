/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/

package produvia.com.lights;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.produvia.sdk.WeaverSdk;

import org.json.JSONObject;

import produvia.com.weaverandroidsdk.WeaverSdkApi;

public class LoginActivity extends Activity implements WeaverSdk.WeaverSdkCallback{

    private boolean mShowLoginView = false;


    private String mUserEmail;
    private String mUserPassword;



    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize weaver sdk -
        // - enter your Weaver API key and call WeaverSdkApi.init
        // - if you don't have an API key - you can get one at:
        //       http://weavingthings.com
        String API_KEY = "XXXXXXXXXX";
        WeaverSdkApi.init(this, API_KEY, getApplicationContext());

        setContentView(R.layout.activity_login);


        TextView registerScreen = (TextView) findViewById(R.id.link_to_register);
        // Listening to activity_register new account link
        registerScreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Switching to Register screen
                Intent i = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(i);
            }
        });

        Button login_button = (Button)findViewById(R.id.btnLogin);
        login_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                login();
            }
        });
    }


    /****************************************************************************************
     * User login
     ****************************************************************************************/
    public void login() {
        EditText userEmailField = (EditText) findViewById(R.id.email);
        mUserEmail = userEmailField.getText().toString();
        EditText userPasswordField = (EditText) findViewById(R.id.password);
        mUserPassword = userPasswordField.getText().toString();

        if (mUserEmail.length() == 0 || mUserPassword.length() == 0) {
            // input fields are empty
            Toast.makeText(LoginActivity.this, "Please complete all the fields", Toast.LENGTH_LONG).show();
            return;
        } else {
            WeaverSdkApi.userLogin(this, mUserEmail, mUserPassword);

        }
    }

    @Override
    public void onTaskCompleted(final int flag, final JSONObject data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json = data;

                    if (json.getBoolean("success")) {

                        /**************************************************************************
                         * The user has been logged in - let's move on to the lights list activity
                         **************************************************************************/
                        Intent intent = new Intent(LoginActivity.this, SmartLightsActivity.class);
                        startActivity(intent);
                        finish();
                    }else {
                        /**************************************************************************
                         * Sing in wasn't successful:
                         **************************************************************************/
                        showLoginView();
                    }

                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }



    private void showLoginView(){
        mShowLoginView = true;
        View loginView = findViewById(R.id.login_view);
        if(loginView != null)
            loginView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mShowLoginView)
            showLoginView();
    }

    @Override
    public void onTaskUpdate(int i, JSONObject jsonObject) {

    }


}
