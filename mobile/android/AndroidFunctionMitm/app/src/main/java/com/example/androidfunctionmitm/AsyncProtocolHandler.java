package com.example.androidfunctionmitm;

import android.os.AsyncTask;

public class AsyncProtocolHandler extends AsyncTask<Object, Void, Object[]> {

    @Override
    protected Object[] doInBackground(Object... params) {
        ProtocolWorker protocolWorker = (ProtocolWorker) params[0];
        Object[] args = (Object[]) params[1];

        Object[] finalArgs = protocolWorker.run(args);

        return finalArgs;
    }
}
