/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/
package produvia.com.lights;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.produvia.sdk.WeaverSdk;

import org.json.JSONObject;

import produvia.com.weaverandroidsdk.WeaverSdkApi;

public class RegisterActivity extends Activity implements WeaverSdk.WeaverSdkCallback{

    String mUserEmail;
    String mUserName;
    String mUserPassword;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        TextView loginScreen = (TextView) findViewById(R.id.link_to_login);
        
        // Listening to Login Screen link
        loginScreen.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				// Switching to Login Screen/closing activity_register screen
				finish();
			}
		});
        Button register = (Button)findViewById(R.id.btnRegister);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerNewAccount();
            }
        });
    }

    /**************************************************************************************************
     * Registration requires an email and a password that's at least 7 chars long:
     **************************************************************************************************/
    public void registerNewAccount() {
        EditText userEmailField = (EditText) findViewById(R.id.reg_email);
        mUserEmail = userEmailField.getText().toString();
        EditText userNameField = (EditText) findViewById(R.id.reg_fullname);
        mUserName = userNameField.getText().toString();
        EditText userPasswordField = (EditText) findViewById(R.id.reg_password);
        mUserPassword = userPasswordField.getText().toString();

        if (mUserEmail.length() == 0 || mUserName.length() == 0 || mUserPassword.length() == 0 ) {
            // input fields are empty
            Toast.makeText(this, "Please complete all the fields", Toast.LENGTH_LONG).show();
        } else {
            if( mUserPassword.length() < 8)
                Toast.makeText( this, "Password must be at least 8 characters long", Toast.LENGTH_LONG).show();
            else
                WeaverSdkApi.register(this, mUserEmail, mUserName, mUserPassword, mUserPassword);

        }
    }

    @Override
    public void onTaskCompleted(final int flag, final JSONObject json) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    if (json.getBoolean("success")) {
                        Intent intent = new Intent(RegisterActivity.this, SmartLightsActivity.class);
                        startActivity(intent);
                        finish();
                    }else{
                      if(json.has("info"))
                          Toast.makeText(RegisterActivity.this, json.getString("info"), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    //usually if the emmail is already taken:
                    Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onTaskUpdate(int i, JSONObject jsonObject) {

    }

}
