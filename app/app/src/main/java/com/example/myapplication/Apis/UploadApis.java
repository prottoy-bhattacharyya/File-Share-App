// UploadApis.java
package com.example.myapplication.Apis;

import  com.example.myapplication.Responses.FcmTokenResponse;
import com.example.myapplication.Responses.FileListResponse;
import com.example.myapplication.Responses.FindAccoundResponse;
import com.example.myapplication.Responses.LoginResponse;
import com.example.myapplication.Responses.ProfilePicUploadResponse;
import com.example.myapplication.Responses.ResetPasswordResponse;
import com.example.myapplication.Responses.UploadResponse;
import com.example.myapplication.Responses.UserHistoryResponse;
import com.example.myapplication.Responses.VarifyOtpResponse;
import com.example.myapplication.Responses.VerifyEmailResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

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

    @FormUrlEncoded
    @POST("send_fcm_token/")
    Call<FcmTokenResponse> sendToken(@Field("fcm_token") String token, @Field("username") String username);

    @Multipart
    @POST("upload_file/")
    Call<UploadResponse> uploadFile(@Part MultipartBody.Part file,
                                    @Part("unique_text") RequestBody unique_text,
                                    @Part("username") RequestBody username);


    @GET("get_file_list/")
    Call<FileListResponse> getFileList(@Query("unique_text") String uniqueText);

    @Streaming
    @GET("download_single_file/")
    Call<ResponseBody> downloadFile(@Query("file_id") int fileId);

    @Multipart
    @POST("save_sender/")
    Call<UploadResponse> save_sender(@Part("unique_text") RequestBody unique_text,
                                     @Part("username") RequestBody username);

    @Multipart
    @POST("save_receiver/")
    Call<UploadResponse> save_receiver(@Part("unique_text") RequestBody unique_text,
                                       @Part("username") RequestBody username);

    @Multipart
    @POST("user_sent_history/")
    Call<UserHistoryResponse> user_sent_history(@Part("username") RequestBody username);

    @Multipart
    @POST("user_receive_history/")
    Call<UserHistoryResponse> user_receive_history(@Part("username") RequestBody username);

    @Multipart
    @POST("setUserProfilePicture/")
    Call<ProfilePicUploadResponse> setUserProfilePicture(@Part MultipartBody.Part profilePicture,
                                                         @Part("username") RequestBody username);

    @GET("getUserProfilePicture/")
    Call<ResponseBody> getUserProfilePicture(@Query("username") String username);

    @GET("sendOtp/")
    Call<FindAccoundResponse> findAccoundAndSendOtp(@Query("identifier") String identifier);

    @GET("verifyOtp/")
    Call<VarifyOtpResponse> verifyOtp(@Query("otp") String otp, @Query("email") String email);

    @GET("resetPassword/")
    Call<ResetPasswordResponse> resetPassword(@Query("email") String email, @Query("password") String password);

    @GET("check_email_verification/")
    Call<VerifyEmailResponse> checkEmailVerification(@Query("email") String email);

}