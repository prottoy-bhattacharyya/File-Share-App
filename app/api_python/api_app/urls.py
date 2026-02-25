from django.urls import path
from . import views

urlpatterns = [
    path('get_data/', views.get_data, name='get_data'),
    path('post_data/', views.post_data, name='post_data'),
]
