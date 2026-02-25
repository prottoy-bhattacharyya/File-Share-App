from django.urls import path
from . import views

urlpatterns = [
    path('check_login/', views.check_login, name='check_login'),
    path('signup/', views.signup, name='signup'),
    path('get_file_count/', views.get_file_count, name='get_file_number'),
    path('download/', views.download, name='download'),
    path('post_files/', views.post_files, name='post_files'),
    path('save_sender/', views.save_sender, name='save_sender'),
    path('save_receiver/', views.save_receiver, name='save_receiver'),
    path('user_info/', views.user_info, name='user_info'),
    path('admin_view/', views.admin_view, name='amin_view'),
    path('', views.index, name='index'),
]
