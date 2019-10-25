package com.mobile.soundappp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NetWorkHelper {

    private static NetWorkHelper instance;

    private static String host = "http://43.240.97.26:8000/webapp";

    // private construct
    private NetWorkHelper() {
    }

    // outside method
    public static NetWorkHelper getInstance() {
        if (instance == null) {
            synchronized (NetWorkHelper.class) {
                if (instance == null) {
                    instance = new NetWorkHelper();
                }
            }
        }
        return instance;
    }

    /**
     * Login Or register
     * @param userName
     * @param password
     */
    public void loginUser(final Context context, String userName, String password, final boolean login) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)//set connect
                .readTimeout(2, TimeUnit.SECONDS)//set read
                .build();// create client
        FormBody.Builder formBody = new FormBody.Builder();// build arguments
        formBody.add("username",userName);// set username
        formBody.add("password",password);// set password
        String url = host + (login?"/login/":"/register/");
        Request request = new Request.Builder()// create Request
                .url(url)
                .post(formBody.build())// paste body
                .build();
        Log.e("net", url);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("net", "onFailure");
                ToastUtils.show(context, "network failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e("net", "onResponse" + response.toString());
                JSONObject jsonObject = parseJsonWithJsonObject(response);
                if(jsonObject != null && getJsonVal(jsonObject, "status").equals("true")){
                    if(login){
                        String id = getJsonVal(jsonObject, "ID");
                        StatusUtil.idNumber = id;
                        Intent intent = new Intent(context, MainActivity.class);
                        context.startActivity(intent);
                    }else{
                        ToastUtils.show(context, "register successfully, now please sign in");
                    }
                }else{
                    ToastUtils.show(context, "operation failed");
                }
            }
        });// use okhttp to send request
    }

    public void loopServer(final Context context, final Handler handler) {
        OkHttpClient client = new OkHttpClient();// create client
        FormBody.Builder formBody = new FormBody.Builder();// build arguments
        formBody.add("id",StatusUtil.idNumber);// set userid
        formBody.add("lectureID","1"); // set userid
        String url = host + "/check/";
        Request request = new Request.Builder()// create Request
                .url(url)
                .post(formBody.build())// paste body
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ToastUtils.show(context, "network failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject jsonObject = parseJsonWithJsonObject(response);
                /*if (jsonObject != null){
                    Log.e("response",jsonObject.toString());
                }
                 */
                if(jsonObject != null && getJsonVal(jsonObject, "status").equals("true")){
                    String type = getJsonVal(jsonObject, "type");
                    String data = getJsonVal(jsonObject, "data");

                    // data = new String(Base64.decode(data.getBytes(), Base64.DEFAULT));
                    //use Handler to control message
                    Log.e("net", type + " " + data);
                    Message message = null;
                    if(type.equals("answer_question")){
                        // send notification
                        message = handler.obtainMessage(1, data);
                    }else if(type.equals("link")){
                        // open link
                        message = handler.obtainMessage(2, data);
                    }
                    if(message != null) {
                        Log.e("net", "send message");
                        handler.sendMessage(message);
                    }
                }else{
                    if(jsonObject == null){
                        ToastUtils.show(context, "no response from server");
                    }
                }
            }
        });// use okhttp to send request
    }

    /**
     * @param file  file
     */
    public void upload(final Context context, File file) throws IOException {
        String encodeString = "";
        try{
            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[(int)file.length()];
            inputStream.read(buffer);
            inputStream.close();
            encodeString = Base64.encodeToString(buffer, Base64.DEFAULT);
            Log.e("buffer", buffer.length+"");
            Log.e("encode string", encodeString);
        }catch (Exception e){
            ToastUtils.show(context, "upload file not exist");
            e.printStackTrace();
        }
        OkHttpClient client = new OkHttpClient();// create client
        FormBody.Builder formBody = new FormBody.Builder();// build arguments
        formBody.add("id",StatusUtil.idNumber);// set useridarguments
        formBody.add("lectureID", "1");// set userid
        formBody.add("ID_to_be_helped","");
        formBody.add("type","voice");// set userid
        formBody.add("data",encodeString);// set userid
        String url = host + "/upload/";
        Request request = new Request.Builder()// create Request
                .header("Content-type", "application/x-www-form-urlencode")
                .url(url)
                .post(formBody.build())// paste body
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ToastUtils.show(context, "network failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e("net", "onResponse" + response.toString());
                JSONObject jsonObject = parseJsonWithJsonObject(response);
                if(jsonObject != null && getJsonVal(jsonObject, "status").equals("true")){
                    ToastUtils.show(context, "upload successfully");
                }else if (jsonObject != null && getJsonVal(jsonObject, "status").equals("false")){
                    ToastUtils.show(context, "upload failed");
                }else
                    ToastUtils.show(context, "no response");

            }
        });
    }

    private JSONObject parseJsonWithJsonObject(Response response) throws IOException {
        String responseData = response.body().string();
        try{
            JSONObject jsonObject = new JSONObject(responseData);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getJsonVal(JSONObject jsonObject, String key){
        try {
            return jsonObject.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
}
