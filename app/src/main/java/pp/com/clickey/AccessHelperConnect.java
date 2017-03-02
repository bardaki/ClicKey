package pp.com.clickey;

import android.net.Uri;

/**
 * Created by baryariv on 09/01/2017.
 */

public class AccessHelperConnect {

    public static final String GET_USER = "http://clickkeypaypal-test.us-east-1.elasticbeanstalk.com/auth/create?code=";
    public static final String GET_INVOICE = "http://clickkeypaypal-test.us-east-1.elasticbeanstalk.com/invoice/create";

    public static final String GET_USER_PROD = "http://clickkeypaypal.us-east-1.elasticbeanstalk.com/auth/create?code=";
    public static final String GET_INVOICE_PROD = "http://clickkeypaypal.us-east-1.elasticbeanstalk.com/invoice/create";

    public static final String DATA_PROFILE = Uri.encode("profile email address https://uri.paypal.com/services/paypalattributes");
    public static final String DATA_INVOICE = "profile email address https://uri.paypal.com/services/invoicing";
    private static final String URL_REDIRECT = "http://clickey.online";

    public static final String GET_TEMPLATE = "http://clickkeypaypal-test.us-east-1.elasticbeanstalk.com/template/get";
    public static final String SAVE_TEMPLATE = "http://clickkeypaypal-test.us-east-1.elasticbeanstalk.com/template/create";

    public static final String GET_TEMPLATE_PROD = "http://clickkeypaypal.us-east-1.elasticbeanstalk.com/template/get";
    public static final String SAVE_TEMPLATE_PROD = "http://clickkeypaypal.us-east-1.elasticbeanstalk.com/template/create";

    private static final String PARAM_SEND_TO = "send_to=";
    private static final String PARAM_AMOUNT = "amount=";
    private static final String PARAM_USER_ID = "user_id=";
    private static final String PARAM_CLIENT_ID = "client_id=";
    private static final String PARAM_CLIENT_SECRET = "client_secret=";
    private static final String PARAM_REDIRECT_URI = "redirect_uri=";
    private static final String PARAM_SCOPE = "scope=";
    private static final String PARAM_SCHEMA = "schema=";
    private static final String PARAM_RESPONSE_TYPE = "response_type=";
    private static final String PARAM_CODE = "code=";
    private static final String PARAM_ACCESS_TOKEN = "access_token=";
    private static final String PARAM_GRANT_TYPE = "grant_type=authorization_code";
    private static final String VALUE_RESPONSE_TYPE = "code";

    private static final String URL_AUTHORIZE = "https://www.sandbox.paypal.com/signin/authorize";
    private static final String URL_TOKENSERVICE = "https://api.sandbox.paypal.com/v1/identity/openidconnect/tokenservice";
    private static final String URL_PROFILE = "https://api.sandbox.paypal.com/v1/identity/openidconnect/userinfo";

    private static final String URL_AUTHORIZE_PROD = "https://www.paypal.com/signin/authorize";
    private static final String URL_TOKENSERVICE_PROD = "https://api.paypal.com/v1/identity/openidconnect/tokenservice";
    private static final String URL_PROFILE_PROD = "https://api.paypal.com/v1/identity/openidconnect/userinfo";

    private static final String SCHEMA = "openid";

    private static boolean mIsTest;
    private static String valueClientId = null;
    private static String valueClientSecret = null;

    public static final String TOKEN_URL = URL_REDIRECT + "/?code";

    /**
     * Not going to be exposed.
     *
     * @param clientId
     */
    public AccessHelperConnect(final String clientId, boolean isTest) {
        valueClientId = clientId;
        mIsTest = isTest;
//        valueClientSecret = clientSecret;
    }

    /**
     * Initializes an instance of AccessHelper and returns it.
     *
     * @param clientId
     * @return the AccessHelper
     */
    public static AccessHelperConnect init(final String clientId, boolean isTest) {
        return new AccessHelperConnect(clientId, isTest);
    }

    /**
     * Returns the application's authorization URL for PayPal Access.
     *
     * @return the authorization URL as {@link String}
     */
    public String getAuthUrl() {
        final StringBuilder authUrlBuilder = new StringBuilder();
        authUrlBuilder.append(mIsTest ? URL_AUTHORIZE : URL_AUTHORIZE_PROD).append("?")
                .append(PARAM_CLIENT_ID).append(valueClientId).append("&")
                .append(PARAM_SCOPE).append(DATA_INVOICE).append("&")
                .append(PARAM_REDIRECT_URI).append(Uri.encode(URL_REDIRECT))
                .append("&");
        return authUrlBuilder.toString();
    }

    /**
     * Returns the Access Token url.
     *
     * @return the Access Token url
     */
    public String getTokenServiceUrl() {
        return mIsTest ? URL_TOKENSERVICE : URL_TOKENSERVICE_PROD;
    }

    /**
     * Creates the needed parameters to get the Authorization Token.
     *
     * @param code the code from the Token Service
     * @return the needed parameters
     */
    public String getTokenServiceParameters(final String code) {
        final StringBuilder paramsBuilder = new StringBuilder();
        paramsBuilder.append(PARAM_CLIENT_ID).append(valueClientId).append("&")
                .append(PARAM_REDIRECT_URI).append(Uri.encode(URL_REDIRECT))
                .append("&").append(PARAM_GRANT_TYPE).append("&")
                .append(PARAM_CLIENT_SECRET).append(valueClientSecret)
                .append("&").append(PARAM_CODE).append(code);
        return paramsBuilder.toString();
    }

    /**
     * Returns the URL for requesting profile information.
     *
     * @param accessToken
     * @return the profile url including the Access Token
     */
    public String getProfileUrl(final String accessToken) {
        final StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(mIsTest ? GET_USER : GET_USER_PROD).append(accessToken);
//        urlBuilder.append(URL_PROFILE).append("?").append(PARAM_SCHEMA)
//                .append(SCHEMA).append("&").append(PARAM_ACCESS_TOKEN)
//                .append(accessToken);
        return urlBuilder.toString();
    }

    /**
     * Returns the URL for create invoice.
     *
     * @param userId
     * @return the create invoice url
     */
    public String getInvoiceUrll(final String userId, final String sendTo, final String amount) {
        final StringBuilder urlBuilder = new StringBuilder();
        //urlBuilder.append(GET_USER).append(accessToken);
        urlBuilder.append(mIsTest ? GET_INVOICE : GET_INVOICE_PROD).append("?").append(PARAM_USER_ID).append(userId)
        .append("&").append(PARAM_SEND_TO).append(sendTo)
        .append("&").append(PARAM_AMOUNT).append(amount);

        return urlBuilder.toString();
    }

    public String getTemplateUrl(final String userId) {
        final StringBuilder urlBuilder = new StringBuilder();
        //urlBuilder.append(GET_USER).append(accessToken);
        urlBuilder.append(mIsTest ? GET_TEMPLATE : GET_TEMPLATE_PROD).append("?").append(PARAM_USER_ID).append(userId);

        return urlBuilder.toString();
    }

    public String saveTemplateUrl(final String userId) {
        final StringBuilder urlBuilder = new StringBuilder();
        //urlBuilder.append(GET_USER).append(accessToken);
        urlBuilder.append(mIsTest ? SAVE_TEMPLATE : SAVE_TEMPLATE_PROD).append("?").append(PARAM_USER_ID).append(userId);

        return urlBuilder.toString();
    }

    /**
     * Returns the URL which can be converted to an URI to extract the access
     * code
     *
     * @return the callback URL
     */
    public String getAccessCodeUrl() {
        return TOKEN_URL;
    }

    /**
     * Returns the code parameter that can be used to check incoming URLs
     *
     * @return the code parameter
     */
    public String getCodeParameter() {
        return PARAM_CODE;
    }
}
