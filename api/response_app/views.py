import io
import re
import threading
import os
import random
from datetime import timedelta
from urllib import response

from django.utils import timezone
from django.http import FileResponse, HttpResponse, JsonResponse
from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth.hashers import make_password, check_password
from django.core.mail import send_mail

from file_sharing_project import settings
from . import dbconfig
import mysql.connector

# Create your views here.

def get_connection():
    config = dbconfig.db()
    try:
        conn = mysql.connector.connect(**config)
        return conn
    except mysql.connector.Error as err:
        print(f"Error connecting to MySQL: {err}")
        return None


def index(request):
    conn = get_connection()
    if not conn:
        return HttpResponse("<h1 style='color: red;'>Database connection failed.</h1>")
    
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
        
        conn.commit()

        cursor.execute("SHOW TABLES;")
        result = cursor.fetchall()
    except mysql.connector.Error as err:
        return HttpResponse(f"<h1 style='color: red;'>Database table creation failed: {err}</h1>")
    cursor.close()
    conn.close()

    tables = str(result)
    return HttpResponse("<h1 style='color: green;'>Database Ready. TABLES: </h1>" + "<h1 style='color: blue;'>" + tables + "</h1>")

def admin_view(request):
    conn = get_connection()
    if not conn:
        return HttpResponse("<h1>Database connection failed.</h1>")
    
    cursor = conn.cursor()
    try:
        cursor.execute("select * from file_info;")
        file_info = cursor.fetchall()

        cursor.execute("select * from user_credentials;")
        user_credentials = cursor.fetchall()
    except mysql.connector.Error as err:
        return HttpResponse(f"<h1>Database query failed: {err}</h1>")
    cursor.close()
    conn.close()
    context = {
        'file_info': file_info,
        'user_credentials': user_credentials
    }
    return render(request, 'response_app/admin.html', context)


@csrf_exempt
def login(request):

    username_or_email = request.POST.get('username')
    password = request.POST.get('password')


    if not username_or_email or not password:
        response = {
            'status': 'data error',
            'message': 'Username and password are required.'
        }
        return JsonResponse(response, status=400)
    
    conn = get_connection()
    if not conn:
        response = {
            'status': 'DB error',
            'message': 'Database connection failed.'
        }
        return JsonResponse(response, status=500)
    
    cursor = conn.cursor()

    email_pattern = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9._%+-]+\.[a-zA-Z]{2,}$"

    if re.match(email_pattern, username_or_email):
        email = username_or_email
        cursor.execute("""SELECT username, fullname, email, hashed_password
                        FROM user_credentials
                        WHERE email = %s""", 
                        (email,)
                    )
        
    else:
        username = username_or_email
        cursor.execute("""SELECT username, fullname, email, hashed_password
                        FROM user_credentials
                        WHERE username = %s""", 
                        (username,)
                    )
    
    result = cursor.fetchone()
    cursor.close()
    conn.close()

    if result:
        valid_username, fullname, email, valid_hashed_password = result
    else:
        response = {
            'status': 'user error',
            'message': 'User not found.'
        }
        if cursor:
            cursor.close()
        if conn:
            conn.close()
        return JsonResponse(response)
    

    if check_password(password, valid_hashed_password):
        response = {
            'status': 'success',
            'username': valid_username,
            'email': email,
            'fullname': fullname,
            'message': 'Login successful.'
        }
    else:
        response = {
            'status': 'credentials error',
            'message': 'Incorrect username or password.'
        }
    if cursor:
        cursor.close()
    if conn:
        conn.close()

    email_thread = threading.Thread(target=send_login_mail, args=(fullname,))
    email_thread.start()
    
    return JsonResponse(response)

def send_login_mail(fullname):
    send_mail (
        subject='Login Attempt Notification',
        message=f'Hello {fullname},\n\nThis is a notification of a login attempt to your account. If this was you, you can safely ignore this email. If you did not attempt to log in, please secure your account immediately.',
        from_email=settings.EMAIL_HOST_USER,
        recipient_list=["prottoyvhattacharyya@gmail.com"],
        fail_silently=False,
    )

@csrf_exempt
def signup(request):
    fullname = request.POST.get('fullname')
    email = request.POST.get('email')
    username = request.POST.get('username')
    password = request.POST.get('password')

    if not fullname or not email or not username or not password:
        response = {
            'status': 'data error',
            'message': 'All fields are required.'
        }
        return JsonResponse(response, status=400)
    
    conn = get_connection()
    if not conn:
        response = {
            'status': 'DB conn error',
            'message': 'Database connection failed.'
        }
        return JsonResponse(response, status=500)
    
    hashed_password = make_password(password)

    cursor = conn.cursor()
    cursor.execute("""SELECT username FROM user_credentials WHERE username = %s""", (username,))
    if cursor.fetchone():
        response = {
            'status': 'username exists error',
            'message': 'Username already exists.'
        }
        cursor.close()
        conn.close()
        return JsonResponse(response, status=400)
    
    cursor.execute("""SELECT email FROM user_credentials WHERE email = %s""", (email,))
    if cursor.fetchone():
        response = {
            'status': 'email exists error',
            'message': 'Email already exists.'
        }
        cursor.close()
        conn.close()
        return JsonResponse(response, status=400)
    
    cursor.execute("INSERT INTO user_credentials (fullname, email, username, hashed_password) VALUES (%s, %s, %s, %s)", (fullname, email, username, hashed_password))
    conn.commit()
    cursor.close()
    conn.close()

    response = {
        'status': 'success',
        'message': 'Sign up successful.'
    }

    return JsonResponse(response)

@csrf_exempt
def setUserProfilePicture(request):
    username = request.POST.get('username')
    if request.method == "POST":
        if 'profilePicture' in request.FILES:
            profilePicture = request.FILES['profilePicture']
        else:
            response = {
                'status': 'data error',
                'message': 'Profile picture is required.'
            }
            return JsonResponse(response)

    if not username or not profilePicture:
        response = {
            'status': 'data error',
            'message': 'Username and profile picture are required.'
        }
        return JsonResponse(response)
    
    conn = get_connection()
    if not conn:
        response = {
            'status': 'DB error',
            'message': 'Database connection failed.'
        }
        return JsonResponse(response)
    try:
        cursor = conn.cursor()
        cursor.execute("""UPDATE user_credentials
                        SET profile_picture = %s WHERE username = %s""", 
                        (profilePicture.read(), username)
                    )
        
        conn.commit()
        cursor.close()
        conn.close()

    except Exception as e:
        response = {
            'status': 'DB error',
            'message': 'Failed to update profile picture: ' + str(e)
        }
        return JsonResponse(response)
    
    response = {
        'status': 'success',
        'message': 'Profile picture updated successfully.'
    }

    return JsonResponse(response)


def getUserProfilePicture(request):
    username = request.GET.get('username')
    if not username:
        response = {
            'status': 'data error',
            'message': 'Username is required.'
        }
        return JsonResponse(response)
    
    conn = get_connection()
    if not conn:
        response = {
            'status': 'DB error',
            'message': 'Database connection failed.'
        }
        return JsonResponse(response)
    
    cursor = conn.cursor()
    cursor.execute("""SELECT profile_picture FROM user_credentials 
                   WHERE username = %s""", (username,)
                )
    result = cursor.fetchone()
    cursor.close()
    conn.close()

    if result:
        profile_picture_binary = result[0]
        response = FileResponse(io.BytesIO(profile_picture_binary), 
                                as_attachment=True, 
                                content_type='image/jpeg'
                            )
        return response
    
    else:
        response = {
            'status': 'user error',
            'message': 'User not found or no profile picture set yet.'
        }
        return JsonResponse(response)
    
@csrf_exempt
def upload_file(request):
    if request.method != 'POST':
        response = {
            'status': 'error',
            'message': 'Invalid request method.'
        }
        return JsonResponse(response, status=400)
    
    if 'file' in request.FILES:
        file = request.FILES['file']
    
    unique_text = request.POST.get('unique_text')
    file_name = file.name

    if not unique_text:
        response = {
            'status': 'error',
            'message': 'No unique_text provided.'
        }
        return JsonResponse(response, status=400)
    
    conn = get_connection()
    if not conn:
        response = {
            'status': 'DB error',
            'message': 'Database connection failed.'
        }
        return JsonResponse(response, status=500)
    
    cursor = conn.cursor()
    file_binary = file.read()
    cursor.execute("""insert into file_blobs(unique_text, file_name, file_blob) 
                        values (%s, %s, %s)""", 
                        (unique_text, file_name, file_binary)
                    )
    conn.commit()
    cursor.close()
    conn.close()

    response = {
        'status': 'success',
        'message': 'File uploaded successfully.'
    }
    return JsonResponse(response)

@csrf_exempt
def get_file_list(request):
    unique_text = request.GET.get('unique_text')
    conn = get_connection()
    cursor = conn.cursor()
    
    # Get ID and Name for all files with this unique_text
    cursor.execute("SELECT id, file_name FROM file_blobs WHERE unique_text=%s", (unique_text,))
    rows = cursor.fetchall()
    
    file_list = [
        {
            "id": row[0], 
            "name": row[1]
        } 
        for row in rows
        ]
    return JsonResponse({'status': 'success', 'files': file_list})

@csrf_exempt
def download_single_file(request):
    file_id = request.GET.get('file_id')
    conn = get_connection()
    cursor = conn.cursor()
    
    cursor.execute("SELECT file_name, file_blob FROM file_blobs WHERE id=%s", (file_id,))
    row = cursor.fetchone()
    
    if row:
        file_name, blob_data = row
        response = HttpResponse(blob_data, content_type='application/octet-stream')
        response['Content-Disposition'] = f'attachment; filename="{file_name}"'
        return response
    return HttpResponse(status=404)



@csrf_exempt
def save_sender(request):
    if request.method == 'POST':
        sender = request.POST.get('username')
        unique_text = request.POST.get('unique_text')
        conn = get_connection()
        if not conn:
            response = {
                'status': 'DB error',
                'message': 'Database connection failed.'
            }
            return JsonResponse(response, status=500)
        cursor = conn.cursor()
        cursor.execute("INSERT INTO file_info (sender, unique_text) VALUES (%s, %s)", (sender, unique_text))
        conn.commit()
        cursor.close()
        conn.close()
        return JsonResponse({'status': 'success', 'message': 'Sender saved successfully.'})
    else:
        return JsonResponse({'status': 'error', 'message': 'Invalid request method.'})

@csrf_exempt
def save_receiver(request):
    if request.method == 'POST':
        receiver = request.POST.get('username')
        unique_text = request.POST.get('unique_text')

        conn = get_connection()
        if not conn:
            response = {
                'status': 'DB error',
                'message': 'Database connection failed.'
            }
            return JsonResponse(response, status=500)
        cursor = conn.cursor()
        cursor.execute("""UPDATE file_info SET receiver = %s WHERE unique_text = %s""", (receiver, unique_text))
        conn.commit()
        cursor.close()
        conn.close()
        return JsonResponse({'status': 'success', 'message': 'Receiver saved successfully.'})
    else:
        return JsonResponse({'status': 'error', 'message': 'Invalid request method.'})

@csrf_exempt
def post_files(request):
    if request.method == 'POST':
        if 'file' in request.FILES:
            file = request.FILES['file']
        else:
            response = {
                'status': 'error',
                'message': 'No file provided.'
            }
            return JsonResponse(response, status=400)
        
        unique_text = request.POST.get('unique_text')
        if not unique_text:
            response = {
                'status': 'error',
                'message': 'No unique_text provided.'
            }
            return JsonResponse(response, status=400)

        folder_path = os.path.join('media', unique_text)
        os.makedirs(folder_path, exist_ok=True)

        
        file_path = os.path.join(folder_path, file.name)
        with open(file_path, 'wb+') as destination:
            for chunk in file.chunks():
                destination.write(chunk)


        conn = get_connection()
        if not conn:
            response = {
                'status': 'DB error',
                'message': 'Database connection failed.'
            }
            return JsonResponse(response, status=500)
        cursor = conn.cursor()

        response = {
            'status': 'success',
            'message': 'File uploaded successfully.'
        }
        return JsonResponse(response)
    else:
        response = {
            'status': 'error',
            'message': 'Invalid request method.'
        }
        return JsonResponse(response, status=400)

@csrf_exempt
def user_history(request):
    if request.method != 'POST':
        response = {
            'status': 'error',
            'message': 'Invalid request method.'
        }
        return JsonResponse(response, status=400)
    
    username = request.POST.get('username')
    conn = get_connection()
    if not conn:
        response = {
            'status': 'DB error',
            'message': 'Database connection failed.'
        }
        return JsonResponse(response, status=500)
    
    cursor = conn.cursor()
    cursor.execute("""SELECT sender, receiver, unique_text 
                   FROM file_info 
                   WHERE sender = %s OR receiver = %s""", 
                   (username, username)
                )
    
    results = cursor.fetchall()
    cursor.close()
    conn.close()

    response = {
        'status': 'success',
        'data': [
            {'sender': row[0], 
             'receiver': row[1], 
             'unique_text': row[2]
            } 
             for row in results
        ]
    }
    return JsonResponse(response)

def send_otp_email(email, otp):
    """Background task to send the email."""
    send_mail(
        subject='File Share App - Recovery Code',
        message=f'Your 6-digit OTP is: {otp}. It will expire in 5 minutes.',
        from_email=settings.EMAIL_HOST_USER,
        recipient_list=[email],
        fail_silently=False,
    )

@csrf_exempt
def send_otp(request):
    identifier = request.GET.get('identifier').strip()
    
    conn = get_connection()
    if not conn:
        print("Database connection failed during OTP sending")  # Debug log
        return JsonResponse({'status': 'error', 'message': 'Database connection failed'})
    cursor = conn.cursor()
    
    cursor.execute("SELECT email FROM user_credentials WHERE username=%s OR email=%s", (identifier, identifier))
    user = cursor.fetchone()
    
    if not user:
        return JsonResponse({'status': 'error', 'message': 'User not found'})
    
    user_email = user[0]
    otp = str(random.randint(100000, 999999))
    expiry = timezone.now() + timedelta(minutes=5)
    print(f"Generated OTP for {user_email}: {otp} (expires at {expiry})")  # Debug log
    
    
    cursor.execute("DELETE FROM otp_verifications WHERE email=%s", (user_email,))
    
    
    cursor.execute("INSERT INTO otp_verifications (email, otp_code, expires_at) VALUES (%s, %s, %s)", 
                   (user_email, otp, expiry))
    conn.commit()
    
    
    # threading.Thread(target=send_otp_email, args=(user_email, otp)).start()
    send_otp_email(user_email, otp)
    
    return JsonResponse({
        'status': 'success', 
        'email': user_email, 
        'message': 'OTP sent successfully'
    })

from django.utils import timezone

@csrf_exempt
def verify_otp(request):
    if request.method != 'GET':
        print("Invalid request method for OTP verification")  # Debug log
        return JsonResponse({'status': 'error', 'message': 'Invalid request method'})
    
    otp_input = request.GET.get('otp').strip()
    email = request.GET.get('email').strip()
    
    conn = get_connection()
    if not conn:
        print("Database connection failed during OTP verification")  # Debug log
        return JsonResponse({'status': 'error', 'message': 'Database connection failed'})
    
    cursor = conn.cursor()
    

    cursor.execute("""SELECT id FROM otp_verifications 
                      WHERE email=%s AND otp_code=%s AND is_verified=FALSE""", 
                   (email, otp_input))
    
    result = cursor.fetchone()
    

    print(f"OTP verification result for {email} with OTP {otp_input}: {'success' if result else 'failure'}")  # Debug log
    if result:
        cursor.execute("UPDATE otp_verifications SET is_verified=TRUE WHERE id=%s", (result[0],))
        cursor.execute("UPDATE user_credentials SET is_verified=TRUE WHERE email=%s", (email,))
        conn.commit()

        response = {
            'status': 'success', 
            'message': 'OTP Verified'
        }
    else:
        response = {
            'status': 'error', 
            'message': 'Invalid or expired OTP'
        }
    
    cursor.close()
    conn.close()
    return JsonResponse(response)

@csrf_exempt
def reset_password(request):
    email = request.GET.get('email').strip()
    password = request.GET.get('password').strip()
    
    
    hashed_pwd = make_password(password)
    
    conn = get_connection()
    if not conn:
        print("Database connection failed during password reset")  # Debug log
        return JsonResponse({'status': 'error', 'message': 'Database connection failed'})
    cursor = conn.cursor()
    
    
    cursor.execute("UPDATE user_credentials SET hashed_password=%s WHERE email=%s", (hashed_pwd, email))
    
    
    cursor.execute("UPDATE otp_verifications SET is_verified=TRUE WHERE email=%s", (email,))
    
    conn.commit()
    return JsonResponse({'status': 'success', 'message': 'Password reset successful'})


@csrf_exempt
def check_email_verification(request):
    email = request.GET.get('email').strip()
    
    conn = get_connection()
    if not conn:
        print("Database connection failed during verification check")  # Debug log
        return JsonResponse({'status': 'error', 'message': 'Database connection failed'})
    cursor = conn.cursor()
    
    cursor.execute("SELECT is_verified FROM user_credentials WHERE email=%s", (email,))
    result = cursor.fetchone()
    
    if result and result[0]:
        return JsonResponse({'status': 'success', 'message': 'User is verified'})
    else:
        return JsonResponse({'status': 'error', 'message': 'User is not verified'})