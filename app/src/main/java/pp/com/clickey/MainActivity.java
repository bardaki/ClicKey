package pp.com.clickey;

import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import com.orhanobut.hawk.Hawk;

public class MainActivity extends AppCompatActivity {

    private TextView openSettingsBtn;
    private TextView loginPaypalBtn;
    private TextView templateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hawk.init(this).build();
        setContentView(R.layout.activity_main);

        openSettingsBtn = (TextView) findViewById(R.id.open_settings_btn);
        loginPaypalBtn = (TextView) findViewById(R.id.login_paypal_btn);
        templateBtn = (TextView) findViewById(R.id.template_btn);

        openSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentOpenBluetoothSettings = new Intent();
                intentOpenBluetoothSettings.setAction(Settings.ACTION_INPUT_METHOD_SETTINGS);
                startActivity(intentOpenBluetoothSettings);
            }
        });
        loginPaypalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClicKeyUtils.isOnline(getApplicationContext()))
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                else {
                    showError();
                }
            }
        });
        templateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PayPalTemplateActivity.class));
            }
        });
    }

    public void showError() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));

        // set title
        alertDialogBuilder.setTitle("No Internet Connection");

        // set dialog message
        alertDialogBuilder
                .setMessage("Please connect to network")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
}
