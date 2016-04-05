package com.artmap.manzo.artmap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    private UserLoginTask mAuthTask = null;

    private Boolean isOk = true;



    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mUsername;
    private EditText mPasswordView;
    private EditText mPassCheck;
    private View mProgressView;
    private View mLoginFormView;
    private View mLogoutForm;
    private View mLoginWrapper;
    private boolean pushedLogin = true;

    //AdminUiReferences
    static EditText aEtTitle;
    static EditText aEtYear;
    static EditText aEtAuthor;
    static EditText aEtTag;
    static Dialog moreinfoDialog;
    static ParseFile photoToUpload;
    static TextView tv_adminStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);





        // Set up the login form.
        mUsername = (EditText) findViewById(R.id.username);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mPassCheck = (EditText) findViewById(R.id.passCheck);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLoginOrRegister();
                    return true;
                }
                return false;
            }
        });

        LinearLayout loginBack = (LinearLayout) findViewById(R.id.ll_loginBack);
        loginBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                pushedLogin = true;
                attemptLoginOrRegister();
            }
        });


        Button facebookLogin = (Button) findViewById(R.id.btn_fblogin);
        facebookLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                pushedLogin = false;


                showProgress(true);


                List<String> permissions = Arrays.asList("public_profile", "email");
                ParseFacebookUtils.logInWithReadPermissionsInBackground(LoginActivity.this, permissions, new LogInCallback() {
                    @Override
                    public void done(final ParseUser user, ParseException err) {

                        if (user == null) {
                            showProgress(false);
                            Toast.makeText(getApplicationContext(),
                                    R.string.toast_logininterrupted,
                                    Toast.LENGTH_SHORT).show();
                            Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                            if (err != null) {
                                Log.e("////Parse:", err.toString());
                                showProgress(false);
                                Toast.makeText(getApplicationContext(),
                                        R.string.toast_connectionerror,
                                        Toast.LENGTH_SHORT).show();
                            }

                        } else if (user.isNew()) {
                            Log.d("MyApp", "User signed up and logged in through Facebook!");

                            fbRegister(user);

                        } else {
                            Log.d("MyApp", "User logged in through Facebook!");


                            final String returnedUsername = user.getUsername();
                            final String returnedEmail = user.getEmail();

                            ParseQuery<ParseUser> query = ParseUser.getQuery();
                            query.whereEqualTo("email", returnedEmail);
                            query.findInBackground(new FindCallback<ParseUser>() {
                                public void done(List<ParseUser> objects, ParseException e) {
                                    if (e == null) {

                                        ParseUser parseUser = objects.get(0);
//                                        final int returnedCheckins = parseUser.getInt("checkCounter");
                                        //Login client side
                                        changeLoginStatus(true, returnedUsername, returnedEmail, parseUser);
                                        setUsernameButton(returnedUsername);
                                        SharedPreferences loginSharedPreferences = getApplicationContext().getSharedPreferences("LoginStatus", 0);
                                        setUsernameScore(loginSharedPreferences);
                                        if (isAdministrator(getApplicationContext())) {
                                            setResult(RESULT_OK);
//                                            setAdministratorInterface();
//                                            setAdministrator();
                                        }


                                        Toast.makeText(getApplicationContext(), R.string.toast_loggedin, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }


                            });
                        }
                    }
                });


            }
        });





        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                pushedLogin = false;
                attemptLoginOrRegister();
            }
        });

//        Button logOutButton = (Button) findViewById(R.id.btn_logout);
//        logOutButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                pushedLogin = false;
//                changeLoginStatus(false, null);
//                MapsActivity.userNameTV.setText(null);
//                Toast.makeText(getApplicationContext(), "Logout", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        });



        TextView disclaimer = (TextView) findViewById(R.id.tv_disclaimer);
        disclaimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDisclaimerDialog();
            }
        });


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);


//        mLogoutForm = findViewById(R.id.logout_form);
//        mLoginWrapper = findViewById(R.id.wrap_login_n_register);
//
//        if (checkUserIsLogged()) {
//            mLoginWrapper.setVisibility(View.GONE);
//            mLogoutForm.setVisibility(View.VISIBLE);
//        } else {
//            mLoginWrapper.setVisibility(View.VISIBLE);
//            mLogoutForm.setVisibility(View.GONE);
//        }
    }

    public static void setUsernameButton(String username) {

        // Add space and trim to space to shorten long usernames
        String modifiedUsername = username + " ";
        modifiedUsername = modifiedUsername.substring(0, modifiedUsername.indexOf(" ")).trim();
        MapsActivity.userNameTV.setText(modifiedUsername);

    }

    public static void setUsernameScore (SharedPreferences sharedPreferences) {

        MapsActivity.userScoreTV.setText(String.valueOf(
                        (sharedPreferences.getInt("CheckInCounter", 0) * 20) +
                                (sharedPreferences.getInt(
                                        sharedPreferences.getString("Username", "")
                                        + "_UploadedPhotosCounter", 0) * 100) +
                                (sharedPreferences.getInt("ReportsCounter", 0) * 50)
                )
        );
    }


    private void fbRegister(final ParseUser user) {

        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                String fbEmail = null;
                String fbUsername = null;
                try {
                    fbEmail = jsonObject.getString("email");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    fbUsername = jsonObject.getString("name") + " ";
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                user.setUsername(fbUsername);
                user.setEmail(fbEmail);

                ///Dialog to set password

//                final Dialog passDialog = new Dialog(LoginActivity.this);
//                final String[] passreturn = {null};
//
//                passDialog.getWindow().setTitle("Inserisci una password");
//                final Boolean[] userdismissed = {true};
//
//                passDialog.setContentView(getLayoutInflater().inflate(R.layout.dialog_password, null));
//                final EditText etPass = (EditText) passDialog.findViewById(R.id.et_dialog_pass);
//                Button btnDialogSubmit = (Button)passDialog.findViewById(R.id.btn_passdial_back);
//                passDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        if (userdismissed[0]) {
//                            user.deleteInBackground();
//                            finish();
//                            Toast.makeText(LoginActivity.this, "Devi inserire una password per concludere la registrazione", Toast.LENGTH_LONG).show();
//                        }
//                    }
//                });


                final String finalFbUsername = fbUsername;
                final String finalFbEmail = fbEmail;


//                btnDialogSubmit.setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        passreturn[0] = etPass.getText().toString();
//                        if (isPasswordValid(passreturn[0])) {
//                            passDialog.dismiss();
//                            userdismissed[0] = false;
//                            user.setPassword(passreturn[0]);
//                            user.saveInBackground(new SaveCallback() {
//                                @Override
//                                public void done(ParseException e) {
//                                    if (e != null) {
//                                        Log.e("//////Parse: ", e.toString());
//                                        user.deleteInBackground();
//                                        finish();
//                                    } else {
//                                        changeLoginStatus(true, finalFbUsername);
//                                        setUsernameButton(finalFbUsername);
//                                        Toast.makeText(getApplicationContext(),
//                                                "Login effettuato!",
//                                                Toast.LENGTH_SHORT).show();
//                                        finish();
//                                    }
//                                }
//                            });
//
//
//                        } else {
//                            etPass.setError(getString(R.string.error_invalid_password));
//                            etPass.requestFocus();
//                        }
//                    }
//                });

//                passDialog.show();


                user.setPassword(
                        Long.toHexString(Double.doubleToLongBits(Math.random()))
                );
                user.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.e("//////Parse: ", e.toString());
                            user.deleteInBackground();
                            finish();
                        } else {
                            changeLoginStatus(true, finalFbUsername, finalFbEmail, null);
                            setUsernameButton(finalFbUsername);
                            setUsernameScore(getApplicationContext().getSharedPreferences("LoginStatus", 0));
                            Toast.makeText(getApplicationContext(),
                                    R.string.toast_loggedin,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });





            }
        });
        request.executeAsync();
    }

    private void feedFacebookUser(ParseUser user) { //todo
        final ParseUser finalUser = user;
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                try {
                    finalUser.setEmail(jsonObject.getString("email") );
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    finalUser.setUsername(jsonObject.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        request.executeAsync();
    }

    private void showDisclaimerDialog() {



            final Dialog disclaimerDialog = new Dialog(LoginActivity.this);
            disclaimerDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            disclaimerDialog.setContentView(getLayoutInflater().inflate(R.layout.disclaimer, null));

            Button btnDialogBack = (Button) disclaimerDialog.findViewById(R.id.btn_disclaimer_back);
            btnDialogBack.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    disclaimerDialog.dismiss();
                }
            });

            disclaimerDialog.show();


    }



    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLoginOrRegister() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String username = mUsername.getText().toString();
        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();
        String passCheck = mPassCheck.getText().toString();

        boolean cancel = false;
        View focusView = null;


        if (TextUtils.isEmpty(email) || email.equals(String.valueOf(R.string.prompt_email))) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || password.equals(String.valueOf(R.string.prompt_password))) {
            mPasswordView.setError(getString(R.string.error_insert_password));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }







        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            if (pushedLogin) {
                // Show a progress spinner, and kick off a background task to
                // perform the user login attempt.
                showProgress(true);
                //check if email is registered
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("email", email);
                query.findInBackground(new FindCallback<ParseUser>() {
                    public void done(List<ParseUser> objects, ParseException e) {
                                if (e == null) {
                                    if (objects.isEmpty()) { //no user returned with inserted email
                                        Toast.makeText(getApplicationContext(),
                                                R.string.error_credentials,
                                                Toast.LENGTH_LONG).show();
                                    } else {

                                        //if so, get username from returned object and login
                                        ParseUser returnedParseUser = objects.get(0);
                                        final String returnedUsername = returnedParseUser.getUsername();
                                        final String returnedEmail = returnedParseUser.getEmail();
//                                        final int returnedCheckIns = returnedParseUser.getInt("checkCounter"); //todo

                                        //Parse Login
                                        ParseUser.logInInBackground(returnedUsername, password, new LogInCallback() {
                                            @Override
                                            public void done(ParseUser parseUser, ParseException e) {
                                                if (e != null) {
                                                    Log.e("////Parse error", e.getMessage());
                                                }
                                            }
                                        });

                                        //Login client side
                                        changeLoginStatus(true, returnedUsername, returnedEmail, returnedParseUser);
                                        setUsernameButton(returnedUsername);
                                        setUsernameScore(getApplicationContext().getSharedPreferences("LoginStatus", 0));
                                        Toast.makeText(getApplicationContext(),
                                                R.string.toast_loggedin,
                                                Toast.LENGTH_SHORT).show();
                                        finish();

                                    }
                                } else {

                                    Log.e("//////Parse error", e.toString());
                                    Log.e("//////Error code", String.valueOf(e.getCode()));

                                    if ((e.getCode()) == 100) {
                                        Toast.makeText(getApplicationContext(),
                                                R.string.error_connection,
                                                Toast.LENGTH_LONG).show();
                                    }

                                    if ((e.getCode()) == 101) {
                                        Toast.makeText(getApplicationContext(),
                                                R.string.error_credentials,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                                showProgress(false);
                            }
                        });

            } else {

                // Check if password and check field are the same
                if (!(password.equals(passCheck))) {
                    mPasswordView.setError(getString(R.string.error_field_different));
                    focusView = mPasswordView;
                    cancel = true;
                }

                if (TextUtils.isEmpty(username) || username.equals(String.valueOf(R.string.prompt_username))) {
                    mUsername.setError(getString(R.string.error_insert_username));
                    focusView = mUsername;
                    cancel = true;
                }



                if (cancel) {
                    focusView.requestFocus();

                } else {

                    // Show a progress spinner, and kick off a background task to
                    // perform the user login attempt.
                    showProgress(true);
                    ParseUser newUser = new ParseUser();
                    newUser.setEmail(email);
                    newUser.setUsername(username);
                    newUser.setPassword(password);
                    newUser.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(getApplicationContext(),
                                        R.string.toast_registered,
                                        Toast.LENGTH_SHORT).show();
                                changeLoginStatus(true, username, email, null);
                                setUsernameButton(username);
                                setUsernameScore(getApplicationContext().getSharedPreferences("LoginStatus", 0));
                                finish();
                            } else {
                                Log.e("//////Parse error", e.toString());
                                Log.e("//////Error code", String.valueOf(e.getCode()));

                                if ((e.getCode()) == 100) {
                                    Toast.makeText(getApplicationContext(),
                                            R.string.error_connection,
                                            Toast.LENGTH_LONG).show();
                                }

                                if ((e.getCode()) == 202) {

                                    Random random = new Random();
                                    String hint = username + String.valueOf(
                                            random.nextInt((99-10)+1) + 10);
                                    mUsername.setText(hint);

                                    mUsername.setError(getString(R.string.error_username_hint));
                                    View focusUsername = mUsername;
                                    focusUsername.requestFocus();

                                }

                                if ((e.getCode()) == 203) {

                                    mEmailView.setError(getString(R.string.error_used_email));
                                    View focusUsername = mEmailView;
                                    focusUsername.requestFocus();
                                }


                            }
                            showProgress(false);
                        }
                    });

                }
//            mAuthTask = new UserLoginTask(email, password);
//            mAuthTask.execute((Void) null);
            }

        }
    }

    //////////////// Change user stauts logged\not logged /////////////
    private void changeLoginStatus(Boolean status, String username, String email, ParseUser parseUser) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("LoginStatus", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Username", username);
        editor.putString("Email", email);
        editor.putBoolean("LoginBoolStatus", status);

        if (parseUser != null) {
            editor.putInt("CheckInCounter", parseUser.getInt("checkCounter"));
            editor.putInt("Reports", parseUser.getInt("reports"));
            editor.putInt("IsAdmin", parseUser.getInt("isAdmin"));
        }

        editor.putInt(username + "_UploadedPhotosCounter",
                        ApplicationOverride.mDbHelper.countSameInColumn(username, "user")
        );

        editor.commit();

        try {
            MapsActivity.mUserIsLogged = status;
        } catch (NullPointerException e) {
            Log.e("Catch", e.getMessage());
        }

    }

    /////////////// Check intent extra for user logged //////////////

    private boolean checkUserIsLogged() {
        Bundle extras = getIntent().getExtras();
        Boolean isLogged = false;
        if (extras != null) {
            isLogged = extras.getBoolean("USER_IS_LOGGED", false);
        }
        return isLogged;
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }



    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

//            try {
//                // Simulate network access.
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                return false;
//            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }


    //////////////////////// ADMINISTRATOR //////////////////////////////////

    public static boolean isAdministrator (Context context) {

        SharedPreferences sharedPreferences = context.getSharedPreferences("LoginStatus", 0);
        Boolean isAdmin = false;
        if ((sharedPreferences.getInt("IsAdmin", 0)) == 1) {
            isAdmin = true;
        }
        return isAdmin;
    }


    public static void setAdministrator (final Context context, final LayoutInflater layoutInflater) {


        MapsActivity.adminButton.setVisibility(View.VISIBLE);
        MapsActivity.adminButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                MapsActivity.adminButton.setVisibility(View.GONE);
                MapsActivity.navWrap.findViewById(R.id.ll_navigation_fabs).setVisibility(View.GONE);
                MapsActivity.adminWrap.findViewById(R.id.ll_admin).setVisibility(View.VISIBLE);
                MapsActivity.adminClickableImage.setImageDrawable(null);
                MapsActivity.adminClickableImage.setVisibility(View.GONE);
                MapsActivity.adminLoadingImageTxt.setVisibility(View.VISIBLE);


                MapsActivity.adminButtonRotate.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        ParseUtilities.SaveAndSetModPhoto saveAndSetModPhoto = new ParseUtilities.SaveAndSetModPhoto();
                        saveAndSetModPhoto.execute(
                                PhotoUtilities.bitmapToByteArray(
                                        PhotoUtilities.rotateBitmap(
                                                PhotoUtilities.bitmapFromFile(ParseUtilities.lastModeratedImagePath)
                                        )
                                )
                        );
                    }
                });

                MapsActivity.adminButtonOk.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showModerationDialog(context, layoutInflater, ParseUtilities.lastModeratedObject);
                    }
                });

                MapsActivity.adminButtonNo.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ParseUtilities.setAsModerated(ParseUtilities.lastObjectId);
                    }
                });

                MapsActivity.adminButtonClose.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        closeAdministrator();
                    }
                });

                ParseUtilities.getFirstUnmoderated();
            }
        });



    }

    public static void unsetAdministrator() {
        MapsActivity.adminButton.setVisibility(View.GONE);
        MapsActivity.navWrap.findViewById(R.id.ll_navigation_fabs).setVisibility(View.VISIBLE);
        MapsActivity.adminWrap.findViewById(R.id.ll_admin).setVisibility(View.GONE);
    }

    public static void closeAdministrator() {
        MapsActivity.adminButton.setVisibility(View.VISIBLE);
        MapsActivity.navWrap.findViewById(R.id.ll_navigation_fabs).setVisibility(View.VISIBLE);
        MapsActivity.adminWrap.findViewById(R.id.ll_admin).setVisibility(View.GONE);
    }

    //////////////////// Show Dialog ///////////////////////


    private static void showModerationDialog(Context context, LayoutInflater layoutInflater, final ParseObject parseObject) {



        moreinfoDialog = new Dialog(context);
        moreinfoDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        moreinfoDialog.setContentView(layoutInflater.inflate(R.layout.popup_moreinfo, null));


        aEtTitle = (EditText)moreinfoDialog.findViewById(R.id.et_moreinfo_title);
        aEtTitle.setHint(parseObject.getString("title"));
        aEtAuthor = (EditText)moreinfoDialog.findViewById(R.id.et_moreinfo_author);
        aEtAuthor.setHint(parseObject.getString("author"));
        aEtYear = (EditText)moreinfoDialog.findViewById(R.id.et_moreinfo_year);
        aEtYear.setHint(parseObject.getString("year"));
        TextView tv_tagTitle = (TextView)moreinfoDialog.findViewById(R.id.tv_report_tag_static);
        tv_tagTitle.setVisibility(View.VISIBLE);
        aEtTag = (EditText)moreinfoDialog.findViewById(R.id.et_moreinfo_tag);
        aEtTag.setVisibility(View.VISIBLE);
        aEtTag.setHint("Painting/Poster/Installation/Sticker");


        Button popupForwardHD = (Button)moreinfoDialog.findViewById(R.id.btn__moreinfo_hd);
        popupForwardHD.setVisibility(View.GONE);


        Button popupBack = (Button)moreinfoDialog.findViewById(R.id.btn__moreinfo_back);
        popupBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreinfoDialog.dismiss();
            }
        });


        Button popupForward = (Button)moreinfoDialog.findViewById(R.id.btn__moreinfo_forward);
        popupForward.setText("Conferma");
        popupForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUtilities.uploadToMainDb(parseObject, ParseUtilities.lastModeratedImagePath);
            }
        });


        moreinfoDialog.show();

    }


//
//    public static void uploadToMainDb (final ParseObject parseObject, final String filePath) {
//        final LinearLayout ll_infoFields = (LinearLayout)moreinfoDialog.findViewById(R.id.ll_moreinfo_fields_wrap);
//        ll_infoFields.setVisibility(View.GONE);
//        final Button btn_admin_ok = (Button)moreinfoDialog.findViewById(R.id.btn__moreinfo_forward);
//        btn_admin_ok.setVisibility(View.GONE);
//        tv_adminStatus = (TextView)moreinfoDialog.findViewById(R.id.tv_admin_upload_status);
//        tv_adminStatus.setVisibility(View.VISIBLE);
//        tv_adminStatus.setText("Checking last Main DB object..");
//
//        ParseQuery<ParseObject> parseQuery = ParseQuery.getQuery("TestDB"); //Todo change to MainDB
//        parseQuery.orderByDescending("artId");
//        parseQuery.getFirstInBackground(new GetCallback<ParseObject>() {
//            @Override
//            public void done(ParseObject countObject, ParseException e) {
//                if (e == null) {
//
//                    final int newId = (countObject.getInt("artId")) + 1;
//                    String filename = "art" + (newId) + ".jpg";
//                    String filenameHd = "art" + (newId) + "HD.jpg";
//
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inJustDecodeBounds = true;
//                    BitmapFactory.decodeFile(filePath, options);
////                    options.inJustDecodeBounds = false;
//                    if (options.outWidth > 600 || options.outHeight > 600) {
//
//                        File elab = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES, filenameHd);
//                        try {
//                            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(elab));
//                            PhotoUtilities.resizeFileToHD(filePath).compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//                            outputStream.flush();
//                            outputStream.close();
//                        } catch (FileNotFoundException e1) {
//                            e1.printStackTrace();
//                        } catch (IOException e1) {
//                            e1.printStackTrace();
//                        }
//
//                        String hdFilePath = elab.getAbsolutePath(); //Todo Erase on complete
//
//
//
//                        ParseUtilities.uploadViaFTP uploadViaFTP = new ParseUtilities.uploadViaFTP();
//                        uploadViaFTP.execute(hdFilePath);
//
//                        tv_adminStatus.setText("Uploading HD image.. Creating LD file..");
//                        photoToUpload = new ParseFile(filename, PhotoUtilities.bitmapToByteArray(PhotoUtilities.resizeFileToLD(filePath)));
//                    } else {
//                        tv_adminStatus.setText("Creating LD imagefile..");
//                        photoToUpload = new ParseFile(filename, PhotoUtilities.bitmapToByteArray(PhotoUtilities.bitmapFromFile(filePath)));
//                    }
//
//                    photoToUpload.saveInBackground(new SaveCallback() {
//                        @Override
//                        public void done(com.parse.ParseException e) {
//                            if (e != null) {
//                                moreinfoDialog.dismiss();
//                                Log.e("Parse ", e.getMessage());
//                            } else {
//                                tv_adminStatus.setText("Updating DB..");
//                                final ParseObject imageObject = new ParseObject("TestDB"); //Todo change to MainDB
//                                String year = aEtYear.getText().toString();
//                                String title = aEtTitle.getText().toString().isEmpty() ? aEtTitle.getHint().toString() : aEtTitle.getText().toString();
//                                String author = aEtAuthor.getText().toString().isEmpty() ? aEtAuthor.getHint().toString() : aEtAuthor.getText().toString();
//                                String tag = aEtTag.getText().toString().isEmpty() ? aEtTag.getHint().toString() : aEtTag.getText().toString();
//
//                                imageObject.put("artId", newId);
//                                imageObject.put("user", parseObject.getString("user"));
//                                imageObject.put("title", title);
//                                imageObject.put("author", author);
//                                if (!year.equals("")){
//                                    imageObject.put("year", Integer.parseInt(year));
//                                }
//                                imageObject.put("tag", tag);
//                                imageObject.put("latitude", parseObject.getDouble("latitude"));
//                                imageObject.put("longitude", parseObject.getDouble("longitude"));
//                                imageObject.put("image", photoToUpload);
//
//                                imageObject.saveInBackground(new SaveCallback() {
//                                    @Override
//                                    public void done(com.parse.ParseException e) {
//                                        moreinfoDialog.dismiss();
//                                        if (e != null) {
//                                            Log.e("Parse: ", e.getMessage());
//                                        } else {
//                                            MapsActivity.adminClickableImage.setImageDrawable(null);
//                                            ParseUtilities.setAsModerated(ParseUtilities.lastObjectId);
//                                        }
//                                    }
//                                });
//                            }
//                        }
//                    });
//
//                } else {
//                    moreinfoDialog.dismiss();
//                    Log.e("parse", e.getMessage());
//                }
//            }
//        });
//
//
//
//    }


}



