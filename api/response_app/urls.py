from django.urls import path
from . import views

urlpatterns = [
    path('login/', views.login, name='login'),
    path('signup/', views.signup, name='signup'),
    path('setUserProfilePicture/', views.setUserProfilePicture, name='set_user_profile_picture'),
    path('getUserProfilePicture/', views.getUserProfilePicture, name='get_user_profile_picture'),
    path('get_file_count/', views.get_file_count, name='get_file_number'),
    path('download/', views.download, name='download'),
    path('post_files/', views.post_files, name='post_files'),
    path('save_sender/', views.save_sender, name='save_sender'),
    path('save_receiver/', views.save_receiver, name='save_receiver'),
    path('user_history/', views.user_history, name='user_history'),
    path('admin_view/', views.admin_view, name='amin_view'),
    path('', views.index, name='index'),
]
