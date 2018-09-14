package com.ncl.pulsarj;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;


public class MainActivity extends AppCompatActivity {

    //private PulsarClient pulsarClient;
    //private Producer<byte[]> producer;

    String wsUrl = "ws://192.168.1.79:6650/ws/v2/producer/persistent/public/default/my-topic";
    // String wsUrl = "wss://echo.websocket.org";
    // admin/v2/persistent,pulsar://localhost:6650/

    private WebSocket webSocket;
    private AppCompatEditText outText;
    private TextView inText;


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

                    String msg = outText.getText().toString();

                    webSocket.send("{\"payload\":\"" + msg + "\"}");

                }

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (webSocket != null) {
            webSocket.cancel();
            webSocket.close(1001, "All good");
        }
    }

    /*
    private void createClient() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {


                try {

                    pulsarClient = PulsarClient.builder()
                            .serviceUrl("pulsar://localhost:6650")
                            .build();

                    producer = pulsarClient.newProducer().topic("my-topic").create();

                } catch (PulsarClientException e) {
                    e.printStackTrace();
                }


                return null;

            }


        }.execute();
    }
    */

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
        public void onMessage(WebSocket webSocket, final String text) {
            super.onMessage(webSocket, text);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inText.append(text);
                    inText.append("\n");
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
                    inText.append("onClosed: " + reason);
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
                    inText.append("onFailure: " + t.getMessage());
                    inText.append("\n");
                }
            });
        }

    };

}
