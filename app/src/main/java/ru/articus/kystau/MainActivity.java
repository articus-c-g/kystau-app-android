package ru.articus.kystau;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {

    private WebView view;
    DBHelper dbHelper;

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        this.view = (WebView) findViewById(R.id.webView);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setDomStorageEnabled(true);
        view.addJavascriptInterface(new JavaScriptInterface(this), "webView_Storage");
        view.addJavascriptInterface(new JavaScriptInfoApp(this), "webView_info");
        view.loadUrl("https://sauna-app.kystau.ru/");


        WebViewClient webViewClient = new WebViewClient() {
            @SuppressWarnings("deprecation") @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            @TargetApi(Build.VERSION_CODES.N) @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        };
        view.setWebViewClient(webViewClient);
    }
    @Override
    public void onBackPressed() {
        if (view.canGoBack()) {
            view.goBack();
        } else {
            openQuitDialog();
        }
    }
    private void openQuitDialog()
    {
        new AlertDialog.Builder(this)
                .setTitle("Выйти из приложения?")
                .setNegativeButton("Нет", null)
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    public class JavaScriptInterface {
        Context mContext;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        Cursor cr;
        final char kv = (char) 34;
        Boolean isOld;

        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void saveJson(String getJson, String nameKey) {
            cv.put(DBHelper.KEY_DATA, getJson);
            cv.put(DBHelper.KEY_NAME, nameKey);
            try {
                cr = db.rawQuery("SELECT * FROM mainTable WHERE name = " + kv + nameKey + kv, null);
                isOld = cr.getCount() > 0;
            } catch (Exception e) {
                Log.e("SQLiteRawQuery", e.toString());
            }
            if (!isOld) {
                db.insert(DBHelper.MAIN_TABLE, null, cv);
            } else {
                db.update(DBHelper.MAIN_TABLE, cv, "name = " + kv + nameKey + kv, null);
            }
            cr.close();
        }

        @JavascriptInterface
        public String loadJson(String nameKey) {
            cr = db.query(DBHelper.MAIN_TABLE, null, "name = " + kv + nameKey + kv, null, null, null, null);
            cr.moveToFirst();
            return cr.getString(1);
        }
    }

    public class JavaScriptInfoApp {

        Context mContext;

        JavaScriptInfoApp(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String versionapp() {
            return "(v" + BuildConfig.VERSION_NAME + ")";
        }

        @JavascriptInterface
        public void Call(String number)
        {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            startActivity(intent);
        }
    }
}