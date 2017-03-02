package pp.com.clickey;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

public class PayPalTemplateActivity extends AppCompatActivity {
    private static final String CLIENT_ID_PROD = "AalKidGjLZHRohIHjyIXOGtXh7_tEmiyFh99upJjgQ4NPltqzzCOpXAjKoPQElwHoGLHM8COpl77cUrH";
    private static final String CLIENT_ID = "AQj3azE2XPN7ZkGbYOfu4txYj-zaVd17pX-3WT3tPjq0SzQ2FKvR7BGph6j5yVzmtgH4lBYS3MUkoQAu";
    private AccessHelperConnect helper;

    private EditText merchantEmailTxt;
    private EditText firstNameTxt;
    private EditText businessNameTxt;
    private EditText lastNameTxt;
    private EditText streetAddressTxt;
    private EditText cityTxt;
    private EditText stateTxt;
    private EditText postalCodeTxt;
    private EditText countryTxt;
    private EditText itemDescriptionTxt;
    private Button updateTemplateBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isTest = ClicKeyUtils.getIsTest();
        helper = AccessHelperConnect.init(isTest ? CLIENT_ID : CLIENT_ID_PROD, isTest);

        setContentView(R.layout.activity_pay_pal_template);
        merchantEmailTxt = (EditText) findViewById(R.id.merchant_email_txt);
        firstNameTxt = (EditText) findViewById(R.id.merchant_first_name_txt);
        lastNameTxt = (EditText) findViewById(R.id.merchant_last_name_txt);
        businessNameTxt = (EditText) findViewById(R.id.merchant_business_name_txt);
        streetAddressTxt = (EditText) findViewById(R.id.street_address_txt);
        cityTxt = (EditText) findViewById(R.id.city_txt);
        stateTxt = (EditText) findViewById(R.id.state_txt);
        postalCodeTxt = (EditText) findViewById(R.id.postal_code_txt);
        countryTxt = (EditText) findViewById(R.id.country_txt);
        itemDescriptionTxt = (EditText) findViewById(R.id.item_description_txt);
        updateTemplateBtn = (Button) findViewById(R.id.update_template_btn);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        updateTemplateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ClicKeyUtils.isOnline(getApplicationContext()))
                    saveTemplate();
                else
                    showError();

            }
        });

//        saveTemplate();
        getTemplate();
    }

    public void getTemplate() {
        SharedPreferences sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        String userId = sharedPref.getString("user_id", "");
        Log.e("user_id", userId);
        final String urlString = helper.getTemplateUrl(userId);
        Log.e("template url", urlString);

        new AsyncConnection(new AsyncConnection.AsyncConnectionListener() {
            public void connectionDone(String result) {
                Log.e("template", result);
                try {
                    final JSONObject object = new JSONObject(result);

                    JSONObject merchant = object.getJSONObject("merchant_info");
                    String merchantEmail = merchant.getString("email");
                    merchantEmailTxt.setText(merchantEmail);
                    String firstName = merchant.getString("first_name");
                    firstNameTxt.setText(firstName);
                    String lastName = merchant.getString("last_name");
                    lastNameTxt.setText(lastName);
                    String businessName = merchant.getString("business_name");
                    businessNameTxt.setText(businessName);

                    JSONObject userAddress = merchant.getJSONObject("address");
                    String street = userAddress.getString("line1");
                    streetAddressTxt.setText(street);
                    String city = userAddress.getString("city");
                    cityTxt.setText(city);
                    String state = userAddress.getString("state");
                    stateTxt.setText(state);
                    String postalCode = userAddress.getString("postal_code");
                    postalCodeTxt.setText(postalCode);
                    String country = userAddress.getString("country_code");
                    countryTxt.setText(country);

                    JSONObject item = (JSONObject) object.getJSONArray("items").get(0);
                    String itemDescription = item.getString("name");
                    itemDescriptionTxt.setText(itemDescription);

                } catch (JSONException e) {
                    if (Hawk.get("email") != null)
                        merchantEmailTxt.setText(Hawk.get("email").toString());
                    if (Hawk.get("firstName") != null)
                        firstNameTxt.setText(Hawk.get("firstName").toString());
                    if (Hawk.get("lastName") != null)
                        lastNameTxt.setText(Hawk.get("lastName").toString());
                    if (Hawk.get("street") != null)
                        streetAddressTxt.setText(Hawk.get("street").toString());
                    if (Hawk.get("city") != null)
                        cityTxt.setText(Hawk.get("city").toString());
                    if (Hawk.get("state") != null)
                        stateTxt.setText(Hawk.get("state").toString());
                    if (Hawk.get("postalCode") != null)
                        postalCodeTxt.setText(Hawk.get("postalCode").toString());
                    if (Hawk.get("country") != null)
                        countryTxt.setText(Hawk.get("country").toString());
                    if (Hawk.get("services") != null)
                        itemDescriptionTxt.setText("services");

                    e.printStackTrace();
                }
            }
        }).execute(AsyncConnection.METHOD_GET, urlString);
    }

    public void saveTemplate() {
        SharedPreferences sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        String userId = sharedPref.getString("user_id", "");
        Log.e("user_id", userId);
        final String urlString = helper.saveTemplateUrl(userId);
        Log.e("template url", urlString);

        String merchantEmail = " ";
        String firstName = " ";
        String lastName = " ";
        String businessName = " ";
        String streetAddress = " ";
        String city = " ";
        String state = " ";
        String postalCode = " ";
        String country = " ";
        String itemDescription = " ";
        if(!merchantEmailTxt.getText().toString().isEmpty())
            merchantEmail = merchantEmailTxt.getText().toString();
        if(!firstNameTxt.getText().toString().isEmpty())
            firstName = firstNameTxt.getText().toString();
        if(!lastNameTxt.getText().toString().isEmpty())
            lastName = lastNameTxt.getText().toString();
        if(!businessNameTxt.getText().toString().isEmpty())
            businessName = businessNameTxt.getText().toString();
        if(!streetAddressTxt.getText().toString().isEmpty())
            streetAddress = streetAddressTxt.getText().toString();
        if(!cityTxt.getText().toString().isEmpty())
            city = cityTxt.getText().toString();
        if(!stateTxt.getText().toString().isEmpty())
            state = stateTxt.getText().toString();
        if(!postalCodeTxt.getText().toString().isEmpty())
            postalCode = postalCodeTxt.getText().toString();
        if(!countryTxt.getText().toString().isEmpty())
            country = countryTxt.getText().toString();
        if(!itemDescriptionTxt.getText().toString().isEmpty())
            itemDescription = itemDescriptionTxt.getText().toString();


        new AsyncConnection(new AsyncConnection.AsyncConnectionListener() {
            public void connectionDone(String result) {
                Log.e("template", result);
                try {
                    final JSONObject object = new JSONObject(result);

                    Toast.makeText(getApplicationContext(), "Template Successfully Saved", Toast.LENGTH_LONG).show();
                    finish();

                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Template Update Failed", Toast.LENGTH_LONG).show();

                    e.printStackTrace();
                }
            }
        }).execute(AsyncConnection.METHOD_POST, urlString, "{\n" +
                " \"merchant_info\": {\n" +
                "     \"email\": \"" + merchantEmail + "\",\n" +
                "     \"first_name\": \"" + firstName + "\",\n" +
                "     \"last_name\": \"" + lastName + "\",\n" +
                "     \"business_name\": \"" + businessName + "\",\n" +
                "     \"address\": {\n" +
                "         \"line1\": \"" + streetAddress + "\",\n" +
                "         \"city\": \"" + city + "\",\n" +
                "         \"state\": \"" + state + "\",\n" +
                "         \"postal_code\": \"" + postalCode + "\",\n" +
                "         \"country_code\": \"" + country + "\"\n" +
                "     }\n" +
                " },\n" +
                " \"billing_info\": [{\n" +
                "     \"email\": \"\"\n" +
                " }],\n" +
                " \"items\": [{\n" +
                "     \"name\": \"" + itemDescription + "\",\n" +
                "     \"quantity\": \"1\",\n" +
                "     \"unit_price\": {\n" +
                "         \"currency\": \"USD\",\n" +
                "         \"value\": \"0\"\n" +
                "     }\n" +
                " }],\n" +
                " \"note\": \"\",\n" +
                " \"payment_term\": {\n" +
                "     \"term_type\": \"NET_45\"\n" +
                " },\n" +
                " \"tax_inclusive\": \"true\",\n" +
                " \"total_amount\": {\n" +
                "     \"currency\": \"\",\n" +
                "     \"value\": \"\"\n" +
                " }\n" +
                "}");
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
