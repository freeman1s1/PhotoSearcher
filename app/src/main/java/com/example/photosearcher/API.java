package com.example.photosearcher;

import com.google.gson.Gson;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface API {

    @Multipart
    @POST("images/visualsearch")
    public Call<ResponseBody> getImage(@Header("Ocp-Apim-Subscription-Key") String key, @Part MultipartBody.Part file);
}
