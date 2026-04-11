// UploadApis.java
package com.example.myapplication.Apis;

import com.example.myapplication.Responses.FindAccoundResponse;
import com.example.myapplication.Responses.LoginResponse;
import com.example.myapplication.Responses.ProfilePicResponse;
import com.example.myapplication.Responses.ResetPasswordResponse;
import com.example.myapplication.Responses.UploadResponse;
import com.example.myapplication.Responses.UserHistoryResponse;
import com.example.myapplication.Responses.VarifyOtpResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface UploadApis {

    @Multipart
    @POST("login/")
    Call<LoginResponse> login(@Part("username") RequestBody username,
                              @Part("password") RequestBody password
    );

    @Multipart
    @POST("signup/")
    Call<UploadResponse> signup(@Part("fullname") RequestBody fullname,
                                @Part("username") RequestBody username,
                                @Part("email") RequestBody email,
                                @Part("password") RequestBody password
    );

    @Multipart
    @POST("upload_file/")
    Call<UploadResponse> uploadFile(@Part MultipartBody.Part file,
                                    @Part("unique_text") RequestBody unique_text);

    @Multipart
    @POST("save_sender/")
    Call<UploadResponse> save_sender(@Part("unique_text") RequestBody unique_text,
                                     @Part("username") RequestBody username);

    @Multipart
    @POST("save_receiver/")
    Call<UploadResponse> save_receiver(@Part("unique_text") RequestBody unique_text,
                                       @Part("username") RequestBody username);

    @Multipart
    @POST("user_history/")
    Call<UserHistoryResponse> user_history(@Part("username") RequestBody username);

    @Multipart
    @POST("setUserProfilePicture/")
    Call<ProfilePicResponse> setUserProfilePicture(@Part MultipartBody.Part profilePicture,
                                                   @Part("username") RequestBody username);

    @GET("getUserProfilePicture/")
    Call<ResponseBody> getUserProfilePicture(@Query("username") String username);

    @GET("sendOtp/")
    Call<FindAccoundResponse> findAccoundAndSendOtp(@Query("identifier") String identifier);

    @GET("verifyOtp/")
    Call<VarifyOtpResponse> verifyOtp(@Query("otp") String otp, @Query("email") String email);

    @GET("resetPassword/")
    Call<ResetPasswordResponse> resetPassword(@Query("email") String email, @Query("password") String password);
}