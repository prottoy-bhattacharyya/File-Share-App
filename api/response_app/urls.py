from django.urls import path
from . import views

urlpatterns = [
    path('login/', views.login, name='login'),
    path('signup/', views.signup, name='signup'),

    path('setUserProfilePicture/', views.setUserProfilePicture, name='set_user_profile_picture'),
    path('getUserProfilePicture/', views.getUserProfilePicture, name='get_user_profile_picture'),
    
    path('get_file_list/', views.get_file_list, name='get_file_number'),
    path('download_single_file/', views.download_single_file, name='download'),

    
    path('upload_file/', views.upload_file, name='upload_file'),
    
    path('save_sender/', views.save_sender, name='save_sender'),
    path('save_receiver/', views.save_receiver, name='save_receiver'),

    path('send_fcm_token/', views.save_fcm_token, name='send_fcm_token'),
    
    path('user_history/', views.user_history, name='user_history'),
    path('user_sent_history/', views.user_sent_history, name='user_sent_history'),

    path('sendOtp/', views.send_otp, name='send_otp'),
    path('verifyOtp/', views.verify_otp, name='verify_otp'),
    path('resetPassword/', views.reset_password, name='reset_password'),

    path('check_email_verification/', views.check_email_verification, name='check_email_verification'),
    
    path('admin_view/', views.admin_view, name='admin_view'),
    path('', views.index, name='index'),
]
