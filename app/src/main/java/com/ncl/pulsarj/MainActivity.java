package com.ncl.pulsarj;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;


public class MainActivity extends AppCompatActivity {

    // String wsUrl = "ws://192.168.1.79:8080/ws/v2/producer/persistent/public/default/my-topic";
    String wsUrl = "ws://192.168.1.79:8080/ws/v2/consumer/persistent/public/default/my-topic/my-subs";
    // String wsUrl = "wss://echo.websocket.org";

    private WebSocket webSocket;
    private AppCompatEditText outText;
    private TextView inText;
    private int counter = 0;
    private boolean producer;
    private boolean automaticAck = true;
    private String lastMsgId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createWebSocket();

        outText = findViewById(R.id.outText);
        inText = findViewById(R.id.inText);

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (webSocket != null) {

                    if (producer) {
                        String msg = outText.getText().toString() + "_" + counter;
                        counter ++;

                        String msgEncoded = "\"" + Base64.encodeToString(msg.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING) + "\"";
                        webSocket.send("{\"payload\":"+ msgEncoded + "}");
                    }
                    else {
                        String ackMsg = "{\"messageId\":" + "\"" + lastMsgId + "\"}";
                        webSocket.send(ackMsg);
                    }

                }

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (webSocket != null) {
            webSocket.cancel();
            webSocket.close(1000, "All good");
        }
    }

    private void createWebSocket() {
        Request wsRequest = createRequest(wsUrl);
        OkHttpClient okHttpClient = createHttpClient();

        webSocket = okHttpClient.newWebSocket(wsRequest, wsListener);
    }

    private OkHttpClient createHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    private Request createRequest(String url) {

        return new Request.Builder()
                .url(url)
                .build();
    }


    private WebSocketListener wsListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, final Response response) {
            super.onOpen(webSocket, response);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inText.setText(response.message());
                    inText.append("\n");

                    try {
                        inText.append(response.body().string());
                        inText.append("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onMessage(final WebSocket webSocket, final String text) {
            super.onMessage(webSocket, text);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        inText.append("onMessage:\n");
                        inText.append(text);
                        inText.append("\n");

                        JSONObject jMsg = new JSONObject(text);
                        lastMsgId = jMsg.optString("messageId");

                        if (!TextUtils.isEmpty(lastMsgId) && automaticAck) {
                            String ackMsg = "{\"messageId\":" + "\"" + lastMsgId + "\"}";
                            webSocket.send(ackMsg);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, final String reason) {
            super.onClosed(webSocket, code, reason);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inText.append("onClosed:\n" + reason);
                    inText.append("\n");
                }
            });
        }

        @Override
        public void onFailure(WebSocket webSocket, final Throwable t, final @Nullable Response response) {
            super.onFailure(webSocket, t, response);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inText.append("onFailure:\n" + t.getMessage());
                    inText.append("\n");
                }
            });
        }

    };

}
