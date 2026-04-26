from django.apps import AppConfig
from django.conf import settings

import os
import firebase_admin
from firebase_admin import credentials

class ResponseAppConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'response_app'

    def ready(self):
        
        if not firebase_admin._apps:
            
            cred_path = os.path.join(settings.BASE_DIR, 'Services', 'Firebase', 'firebase_credentials.json')
            
            if os.path.exists(cred_path):
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
                print("Firebase initialized successfully.")
            else:
                print("Firebase JSON not found. Notifications will fail.")