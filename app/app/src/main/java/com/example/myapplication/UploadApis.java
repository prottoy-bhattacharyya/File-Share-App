// UploadApis.java
package com.example.myapplication;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadApis {


    @Multipart
    @POST("post_files/")
    Call<UploadResponse> uploadFile(@Part MultipartBody.Part file, @Part("unique_text") RequestBody unique_text);

    @Multipart
    @POST("save_sender/")
    Call<UploadResponse> save_sender(@Part("unique_text") RequestBody unique_text, @Part("username") RequestBody username);

    @Multipart
    @POST("save_receiver/")
    Call<UploadResponse> save_receiver(@Part("unique_text") RequestBody unique_text, @Part("username") RequestBody username);

    @Multipart
    @POST("user_info/")
    Call<UserDataResponse> user_info(@Part("username") RequestBody username);
}