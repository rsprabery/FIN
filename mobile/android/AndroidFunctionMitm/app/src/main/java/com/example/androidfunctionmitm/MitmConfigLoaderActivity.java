package com.example.androidfunctionmitm;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


public class MitmConfigLoaderActivity extends ActionBarActivity {

    private static final String TAG = "MitmConfigLoaderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mitm_log);

        String dataDirectory = this.getApplicationContext().getApplicationInfo().dataDir;

        AssetManager am = getAssets();
        try {
            InputStream configFileStream = am.open("config.yaml");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(configFileStream));
            File outFile = new File(dataDirectory + "/config.yaml");

            OutputStream outputStream = new FileOutputStream(outFile);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            String line = null;
            line = bufferedReader.readLine();
            while(line != null) {
                bufferedWriter.write(line + "\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            configFileStream.close();
            bufferedWriter.close();
            outputStream.close();

            Runtime.getRuntime().exec("chmod 777 " + dataDirectory + "/config.yaml");

            TextView tv1 = (TextView)findViewById(R.id.tv1);
            tv1.setText("Config file is now in place!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mitm_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
