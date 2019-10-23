package com.mobile.soundappp;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UploadUtil {
    private OkHttpClient okHttpClient;
    private UploadUtil(){
        okHttpClient = new OkHttpClient();
    }
    /**
     * single instance
     */
    private static class UploadUtilInstance{
        private static final UploadUtil INSTANCE = new UploadUtil();
    }
    public static UploadUtil getInstance(){
        return UploadUtilInstance.INSTANCE;
    }


}
