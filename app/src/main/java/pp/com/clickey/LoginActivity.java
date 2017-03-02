package pp.com.clickey;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import static android.R.attr.country;

public class LoginActivity extends Activity {
    public static final String TYPE = "type";

    /*
     * The id is used to identify your application. Those
     * credentials can be obtained at developer.papyal.com
     */
    private static final String CLIENT_ID_PROD = "AalKidGjLZHRohIHjyIXOGtXh7_tEmiyFh99upJjgQ4NPltqzzCOpXAjKoPQElwHoGLHM8COpl77cUrH";
    private static final String CLIENT_ID = "AQj3azE2XPN7ZkGbYOfu4txYj-zaVd17pX-3WT3tPjq0SzQ2FKvR7BGph6j5yVzmtgH4lBYS3MUkoQAu";

    private static final String ACCESS_DENIED = "access_denied";

    private WebView webView;
    private ProgressDialog progress;
    private AccessHelperConnect helper;

    String state = " ";
    String userId = " ";
    String userFirstName = " ";
    String userLastName = " ";
    String userEmail = " ";
    String street = " ";
    String city = " ";
    String postalCode = " ";
    String country = " ";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isTest = ClicKeyUtils.getIsTest();

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new PPWebViewClient());

        setContentView(webView);

        helper = AccessHelperConnect.init(isTest ? CLIENT_ID : CLIENT_ID_PROD, isTest);

        progress = ProgressDialog.show(LoginActivity.this,
                "Loading...",
                "Login With PayPal");

        webView.loadUrl(helper.getAuthUrl());
        Log.e("URL: ", helper.getAuthUrl());

    }

    private class PPWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
                progress = null;
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.e("URL: ", url);
            if (url.contains(ACCESS_DENIED)) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            } else if (url.startsWith(helper.getAccessCodeUrl())
                    && url.contains(helper.getCodeParameter())) {
                getAccessToken(url);
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        private void getAccessToken(String url) {
            final Uri uri = Uri.parse(url);
            final String code = uri.getQueryParameter("code");
            final String urlParams = helper.getTokenServiceParameters(code);
            final String urlString = helper.getTokenServiceUrl();

            //sendEmail(code);
            getProfile(code);

//            new AsyncConnection(new AsyncConnection.AsyncConnectionListener() {
//                public void connectionDone(String result) {
//                    try {
//                        final JSONObject object = new JSONObject(result);
//                        final String accessToken = object
//                                .getString("access_token");
//
//                        if (accessToken != null && !accessToken.equals("")) {
//                            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
//                            emailIntent.setType("text/plain");
//                            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"serveroverloadofficial@gmail.com"});
//                            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Hello There");
//                            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, accessToken);
//
//
//                            emailIntent.setType("message/rfc822");
//
//                            try {
//                                startActivity(Intent.createChooser(emailIntent,
//                                        "Send email using..."));
//                            } catch (android.content.ActivityNotFoundException ex) {
//
//                            }
//                            getProfile(accessToken);
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).execute(AsyncConnection.METHOD_POST, urlString, urlParams);
        }

        private void getProfile(final String accessToken) {
            final String urlString = helper.getProfileUrl(accessToken);
            Log.e("sdfsdf", urlString);

            new AsyncConnection(new AsyncConnection.AsyncConnectionListener() {
                public void connectionDone(String result) {
                    Log.e("token", accessToken);
                    Log.e("user profile", result);
                    JSONObject jsonObject = null;
                    try {

                        jsonObject = new JSONObject(result);
                        if(jsonObject.has("user_id"))
                            userId = jsonObject.getString("user_id");
                        if(jsonObject.has("given_name"))
                            userFirstName = jsonObject.getString("given_name");
                        if(jsonObject.has("family_name"))
                            userLastName = jsonObject.getString("family_name");
                        if(jsonObject.has("email"))
                            userEmail = jsonObject.getString("email");
                        JSONObject userAddress = jsonObject.getJSONObject("address");
                        if(userAddress.has("street_address"))
                            street = userAddress.getString("street_address");
                        if(userAddress.has("locality"))
                            city = userAddress.getString("locality");
                        if(userAddress.has("region"))
                            state = userAddress.getString("region");
                        if(userAddress.has("postal_code"))
                            postalCode = userAddress.getString("postal_code");
                        if(userAddress.has("country"))
                            country = userAddress.getString("country");

                        Hawk.put("userId", userId);
                        Hawk.put("firstName", userFirstName);
                        Hawk.put("lastName", userLastName);
                        Hawk.put("email", userEmail);
                        Hawk.put("street", street);
                        Hawk.put("city", city);
                        Hawk.put("state", state);
                        Hawk.put("postalCode", postalCode);
                        Hawk.put("country", country);

                        SharedPreferences sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("user_id", userId);
                        editor.commit();
                        Log.e("user_id", userId);

                        saveTemplate();

                        //sendEmail(accessToken);

//                        sendInvoice(userId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setResult(RESULT_OK, new Intent().putExtra(
                            AccessHelperConnect.DATA_PROFILE, result));

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
            }).execute(AsyncConnection.METHOD_GET, urlString);
        }
    }

    public void saveTemplate() {
        SharedPreferences sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        String userId = sharedPref.getString("user_id", "");
        Log.e("user_id", userId);
        final String urlString = helper.saveTemplateUrl(userId);
        Log.e("template url", urlString);


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
                "     \"email\": \"" + userEmail + "\",\n" +
                "     \"first_name\": \"" + userFirstName + "\",\n" +
                "     \"last_name\": \"" + userLastName + "\",\n" +
                "     \"business_name\": \" \",\n" +
                "     \"address\": {\n" +
                "         \"line1\": \"" + street + "\",\n" +
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
                "     \"name\": \" \",\n" +
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

    public void sendEmail(String accessToken) {
        if (accessToken != null && !accessToken.equals("")) {
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Hello There");
            emailIntent.putExtra(Intent.EXTRA_TEXT, accessToken);


            emailIntent.setType("message/rfc822");

            try {
                startActivity(Intent.createChooser(emailIntent,
                        "Send email using..."));
            } catch (android.content.ActivityNotFoundException ex) {

            }
        }
    }
}
