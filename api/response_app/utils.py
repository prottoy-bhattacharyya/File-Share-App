from turtle import color

from firebase_admin import messaging

from file_sharing_project import settings

from django.core.mail import send_mail

from django.template.loader import render_to_string

from . import dbconfig

import mysql.connector

def get_connection():
    config = dbconfig.db()
    try:
        conn = mysql.connector.connect(**config)
        return conn
    except mysql.connector.Error as err:
        print(f"Error connecting to MySQL: {err}")
        return None
    
def get_connection_without_dbname():
    config = dbconfig.db()
    config.pop('database', None)  # Remove the database key if it exists
    try:
        conn = mysql.connector.connect(**config)
        return conn
    except mysql.connector.Error as err:
        print(f"Error connecting to MySQL: {err}")
        return None


def send_fcm_notification(token, title, body):
    try:
        message = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            token=token
        )
        response = messaging.send(message)
        return response
    except Exception as e:
        print(f"FCM Error: {e}")
        return None
    
def send_otp_email(email, otp):
    html_content = render_to_string('otp_template.html', {'otp': otp})

    send_mail(
        subject='File Share App - Verification Code',
        message=f'Your 6-digit OTP is: {otp}. It will expire in 5 minutes.',
        html_message=html_content,
        from_email=settings.EMAIL_HOST_USER,
        recipient_list=[email],
        fail_silently=False,
    )