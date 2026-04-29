from django.apps import AppConfig
from django.conf import settings

import os
import firebase_admin
from firebase_admin import credentials

from .utils import get_connection

class ResponseAppConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'response_app'

    def ready(self):

        self.initialize_database_table()
        
        self.initiaize_firebase()


    def initiaize_firebase(self):
        if not firebase_admin._apps:
            
            cred_path = os.path.join(settings.BASE_DIR, 'Services', 'Firebase', 'firebase_credentials.json')
            
            if os.path.exists(cred_path):
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
                print("Firebase initialized successfully.")
            else:
                print("Firebase JSON not found. Notifications will fail.")

    
    def initialize_database_table(self):
        conn = get_connection()
        if not conn:
            print("Database connection failed.")
            return
        
        cursor = conn.cursor()
        try:

            cursor.execute("""create table if not exists user_credentials (
                                id int primary key auto_increment, 
                                fullname text,
                                username text,
                                email text,
                                is_verified BOOLEAN DEFAULT FALSE,
                                profile_picture mediumblob,
                                hashed_password text,
                                timestamp timestamp default current_timestamp
                            );""")
            
            cursor.execute("""create table if not exists file_info (
                                id int primary key auto_increment, 
                                sender text,
                                unique_text text,
                                receiver text,
                                timestamp timestamp default current_timestamp
                            );""")
            
            cursor.execute("""create table if not exists file_blobs (
                                id int primary key auto_increment, 
                                unique_text text,
                                file_name text,
                                file_blob longblob,
                                timestamp timestamp default current_timestamp
                        );""")
            
            cursor.execute("""CREATE TABLE IF NOT EXISTS otp_verifications (
                                id INT PRIMARY KEY AUTO_INCREMENT,
                                email VARCHAR(255) NOT NULL,
                                otp_code VARCHAR(6) NOT NULL,
                                is_verified BOOLEAN DEFAULT FALSE,
                                expires_at TIMESTAMP NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                INDEX (email), -- For faster lookups
                                INDEX (expires_at) -- For cleanup scripts
                            );""")


            cursor.execute("""CREATE TABLE user_tokens (
                                id INT AUTO_INCREMENT PRIMARY KEY,
                                username VARCHAR(255) NOT NULL,
                                fcm_token TEXT NOT NULL,
                                device_name VARCHAR(100) DEFAULT 'Android Device',
                                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                
                                -- Ensures we don't store the exact same token multiple times
                                UNIQUE KEY unique_token (fcm_token(255)),
                                
                                -- Index for fast lookups when sending notifications to a specific user
                                INDEX idx_username (username)
                            );""")
            
            conn.commit()

            print("Database tables created successfully.")
        except Exception as e:
            print(f"Error creating tables: {e}")
        
        finally:
            cursor.close()
            conn.close()

        