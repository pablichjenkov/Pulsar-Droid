package com.ncl.pulsarj;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
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

    private String basePubUrl = "ws://192.168.64.102:8080/ws/v2/producer/persistent/public/default/";// sink-topic
    private String baseSubUrl = "ws://192.168.64.102:8080/ws/v2/consumer/persistent/public/default/";// source-topic/my-subs";
    // String wsUrl = "wss://echo.websocket.org";

    private OkHttpClient okHttpClient;
    private WebSocket pubSocket;
    private WebSocket subSocket;
    private AppCompatEditText pubTopic;
    private AppCompatEditText subTopic;
    private AppCompatEditText message;
    private TextView inText;
    private int counter = 0;
    private String curSinkTopic;
    private String curSourceTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        okHttpClient = createHttpClient();

        pubTopic = findViewById(R.id.pubTopic);
        subTopic = findViewById(R.id.subTopic);
        message = findViewById(R.id.message);
        inText = findViewById(R.id.inText);

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (pubSocket != null) {
                    String msg = message.getText().toString() + "_" + counter;
                    counter ++;

                    String msgEncoded = "\"" + Base64.encodeToString(msg.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING) + "\"";
                    pubSocket.send("{\"payload\":"+ msgEncoded + "}");

                }

            }
        });

        findViewById(R.id.updateTopic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String newSinkTopic = pubTopic.getText().toString();
                String newSourceTopic = subTopic.getText().toString();

                if (TextUtils.isEmpty(newSinkTopic) || TextUtils.isEmpty(newSourceTopic)) {

                    Toast.makeText(MainActivity.this
                            , "Missing Topic Config"
                            , Toast.LENGTH_SHORT)
                            .show();

                    return;
                }

                if (newSinkTopic.equals(curSinkTopic) == false) {

                    // Close the old connection if there was one, avoiding leakage connection sockets
                    if (pubSocket != null) {
                        pubSocket.close(1000, "Connection changed");
                    }

                    // Create the new Socket to Publish to
                    curSinkTopic = newSinkTopic;
                    String pubUrl = basePubUrl.concat(newSinkTopic);
                    pubSocket = createWebSocket(pubUrl, outWsListener);
                }

                if (newSourceTopic.equals(curSourceTopic) == false) {

                    // Close the old connection if there was one, avoiding leakage connection sockets
                    if (subSocket != null) {
                        subSocket.close(1000, "Connection changed");
                    }

                    curSourceTopic = newSourceTopic;
                    String subUrl = baseSubUrl.concat(newSourceTopic)
                            .concat("/")
                            .concat("My-Subscription");

                    subSocket = createWebSocket(subUrl, inWsListener);
                }

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (pubSocket != null) {
            pubSocket.cancel();
            pubSocket.close(1000, "All good");
        }

        if (subSocket != null) {
            subSocket.cancel();
            subSocket.close(1000, "All good");
        }
    }

    private WebSocket createWebSocket(String wsUrl, WebSocketListener wsListener) {
        Request wsRequest = createRequest(wsUrl);
        okHttpClient = createHttpClient();

        return okHttpClient.newWebSocket(wsRequest, wsListener);
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


    private WebSocketListener inWsListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, final Response response) {
            super.onOpen(webSocket, response);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inText.append("in::" + response.message() + "\n");
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

                        String prevText = inText.getText().toString();

                        inText.setText("in::onMessage:\n");
                        inText.append(text);
                        inText.append("\n");
                        inText.append(prevText);

                        JSONObject jMsg = new JSONObject(text);
                        String msgId = jMsg.optString("messageId");

                        if (!TextUtils.isEmpty(msgId)) {
                            String ackMsg = "{\"messageId\":" + "\"" + msgId + "\"}";
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
                    inText.append("in::onClosed:\n" + reason);
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
                    inText.append("in::onFailure:\n" + t.getMessage());
                    inText.append("\n");
                }
            });
        }

    };

    private WebSocketListener outWsListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, final Response response) {
            super.onOpen(webSocket, response);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inText.append("out::" + response.message() + "\n");
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

                        String prevText = inText.getText().toString();

                        inText.setText("out::onMessage:\n");
                        inText.append(text);
                        inText.append("\n");
                        inText.append(prevText);

                        /*
                        JSONObject jMsg = new JSONObject(text);
                        String msgId = jMsg.optString("messageId");

                        if (!TextUtils.isEmpty(msgId)) {
                            String ackMsg = "{\"messageId\":" + "\"" + msgId + "\"}";
                            webSocket.send(ackMsg);
                        }
                        */

                    } catch (Exception e) {
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
                    inText.append("out::onClosed:\n" + reason);
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
                    inText.append("out::onFailure:\n" + t.getMessage());
                    inText.append("\n");
                }
            });
        }

    };

}
