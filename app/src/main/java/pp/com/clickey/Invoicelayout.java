package pp.com.clickey;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by baryariv on 21/02/2017.
 */

public class Invoicelayout extends RelativeLayout {
    private static final String CLIENT_ID_PROD = "AalKidGjLZHRohIHjyIXOGtXh7_tEmiyFh99upJjgQ4NPltqzzCOpXAjKoPQElwHoGLHM8COpl77cUrH";
    private static final String CLIENT_ID = "AQj3azE2XPN7ZkGbYOfu4txYj-zaVd17pX-3WT3tPjq0SzQ2FKvR7BGph6j5yVzmtgH4lBYS3MUkoQAu";
    private static final String INVOICE_URL_PROD = "https://www.paypal.com?cmd=_pay-inv&id=";
    private static final String INVOICE_URL = "https://www.sandbox.paypal.com?cmd=_pay-inv&id=";


    private InvoiceListener mListener;
    private AccessHelperConnect helper;

    public ImageButton emailDoneBtn;
    private ImageButton backBtn;
    private ImageButton invoiceBtn;

    public EditTextSelectable sendToTxt;
    public TextView amountTxt;
    private AVLoadingIndicatorView loadAnim;

    boolean isTest;


    public Invoicelayout(Context context) {
        super(context);

        isTest = ClicKeyUtils.getIsTest();

        helper = AccessHelperConnect.init(isTest ? CLIENT_ID : CLIENT_ID_PROD, isTest);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.invoice_layout, this);
        invoiceBtn = (ImageButton) findViewById(R.id.invoice_btn);
        backBtn = (ImageButton) findViewById(R.id.back_btn);
        emailDoneBtn = (ImageButton) findViewById(R.id.email_done_btn);
        sendToTxt = (EditTextSelectable) findViewById(R.id.send_to_txt);
        amountTxt = (TextView) findViewById(R.id.amount_txt);
        loadAnim = (AVLoadingIndicatorView) findViewById(R.id.load_anim);


        sendToTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ClicKeyUtils.isEmailValid(sendToTxt.getText().toString())) {
                    sendToTxt.setTextColor(getResources().getColor(R.color.color_main_blue));
                    emailDoneBtn.setVisibility(VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        invoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = getContext().getSharedPreferences("myPref", Context.MODE_PRIVATE);
                String userId = sharedPref.getString("user_id", "");
                Log.e("user_id", userId);
//                for(int i = 0; i < 100 ; i ++)
                sendInvoice(userId);
            }
        });
        emailDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClicKeyUtils.isEmailValid(sendToTxt.getText().toString())) {
                    invoiceBtn.setVisibility(View.VISIBLE);
                    amountTxt.setVisibility(View.VISIBLE);
                    backBtn.setVisibility(View.VISIBLE);
//                    sendToTxt.setFocusable(false);
                    emailDoneBtn.setVisibility(GONE);
                    mListener.changeKeyboardToNumbers();
                }
            }
        });
        backBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editEmail();
            }
        });
    }

    private void sendInvoice(final String userId) {
        if (!ClicKeyUtils.isOnline(getContext())) {
            mListener.showErrorPopup("No Internet Connection.");
            mListener.changeKeyboardToLatin(false);
            resetLayout();
        } else {
            amountTxt.setVisibility(View.GONE);
            loadAnim.setVisibility(View.VISIBLE);
            loadAnim.show();
            final String urlString = helper.getInvoiceUrll(userId, sendToTxt.getText().toString(), amountTxt.getText().toString().substring(1));
            Log.e("invoice", urlString);

            new AsyncConnection(new AsyncConnection.AsyncConnectionListener() {
                public void connectionDone(String result) {
                    Log.e("invoice", result);
                    try {
                        final JSONObject object = new JSONObject(result);
                        final String invoiceId = object.getString("invoice_id");

                        resetLayout();
                        mListener.setTextInInputConnection("Here is your invoice link:\n\n" + (isTest ? INVOICE_URL : INVOICE_URL_PROD) + invoiceId);
                        mListener.changeKeyboardToLatin(false);

                    } catch (JSONException e) {
                        resetLayout();
                        mListener.showErrorPopup("Invoice creation failed.\nPlease try again later");
                        resetLayout();
                        mListener.changeKeyboardToLatin(false);
                        e.printStackTrace();
                    }
                }
            }).execute(AsyncConnection.METHOD_GET, urlString);
        }
    }

    public void resetLayout() {
        sendToTxt.setText("");
        amountTxt.setText("$0");
        emailDoneBtn.setVisibility(View.VISIBLE);
        invoiceBtn.setVisibility(View.GONE);
        backBtn.setVisibility(GONE);
        loadAnim.hide();
    }

    public void editEmail() {
        invoiceBtn.setVisibility(View.GONE);
        amountTxt.setVisibility(View.GONE);
        emailDoneBtn.setVisibility(VISIBLE);
        backBtn.setVisibility(GONE);
        mListener.changeKeyboardToLatin(true);
    }

    public void setListener(InvoiceListener listener) {
        mListener = listener;
    }

    public interface InvoiceListener {
        void changeKeyboardToNumbers();

        void changeKeyboardToLatin(boolean showCandidateView);

        void setTextInInputConnection(String text);

        void showErrorPopup(String text);
    }
}
