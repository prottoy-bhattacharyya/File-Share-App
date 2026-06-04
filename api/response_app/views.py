import io
import re
import random
from datetime import timedelta

from django.utils import timezone
from django.http import FileResponse, HttpResponse, JsonResponse
from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth.hashers import make_password, check_password


from .utils import send_fcm_notification, send_otp_email, get_connection, check_firebase
import mysql.connector

# Create your views here.

def index(request):
    result = ""

    if not check_firebase():
        result += "<h1 style='color: red;'> Firebase is inactive. </h1>"
    else:
        result += "<h1 style='color: green;'> Firebase is initialized. </h1>"

    conn = get_connection()
    if not conn:
        result += "<h1 style='color: red;'>Database connection failed.</h1>"
        return HttpResponse(result)
    else:
        result += "<h1 style='color: green;'>Database connection successful.</h1>"
    
    cursor = conn.cursor()

    cursor.execute("SHOW TABLES;")
    tables = str(cursor.fetchall())

    cursor.close()
    conn.close()

    return HttpResponse(    result
                        +   "<h1 style='color: blue;'>" 
                        +   tables  
                        +   "</h1>"
                    )

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

    username_or_email = request.POST.get('username').strip()
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

    try:
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
    except mysql.connector.Error as err:
        cursor.close()
        conn.close()
        return JsonResponse({'status': 'DB error', 'message': str(err)}, status=500)

    cursor.close()
    conn.close()

    if result:
        valid_username, fullname, email, valid_hashed_password = result
    else:
        response = {
            'status': 'user error',
            'message': 'User not found.'
        }
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

    return JsonResponse(response)

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
        return JsonResponse(response)
    
    conn = get_connection()
    if not conn:
        response = {
            'status': 'DB conn error',
            'message': 'Database connection failed.'
        }
        return JsonResponse(response)
    
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
        return JsonResponse(response)
    
    cursor.execute("""SELECT email FROM user_credentials WHERE email = %s""", (email,))
    if cursor.fetchone():
        response = {
            'status': 'email exists error',
            'message': 'Email already exists.'
        }
        cursor.close()
        conn.close()
        return JsonResponse(response)
    
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
    sender = request.POST.get('username')
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
            return JsonResponse({'status': 'DB error', 'message': 'Database connection failed.'}, status=500)
        
        try:
            cursor = conn.cursor(buffered=True)
            
            cursor.execute("""SELECT sender FROM file_info 
                           WHERE unique_text = %s LIMIT 1;""", 
                           (unique_text,))
            
            result = cursor.fetchone()
            
            
            if result:
                sender = result[0]
                
                cursor.execute("""INSERT INTO file_info (sender, receiver, unique_text, receiving_time)
                               VALUES (%s, %s, %s, CURRENT_TIMESTAMP);""",
                               (sender, receiver, unique_text))
                
                conn.commit()

                cursor.execute("""SELECT fcm_token FROM user_tokens 
                               WHERE username = %s;""", 
                               (sender,)
                            )
                tokens = cursor.fetchall()

            # 4. Send notification to all devices owned by the Sender
            if tokens:
                for token_row in tokens:
                    send_fcm_notification(
                        token=token_row[0],
                        title="File Received!",
                        body=f"{receiver} just downloaded files using code: {unique_text}"
                    )
                
                return JsonResponse({'status': 'success', 'message': 'Receiver saved successfully.'})
            else:
                return JsonResponse({'status': 'error', 'message': 'Invalid unique text.'})

        except Exception as e:
            return JsonResponse({'status': 'error', 'message': str(e)})
        finally:
            cursor.close()
            conn.close()
    else:
        return JsonResponse({'status': 'error', 'message': 'Invalid request method.'})


@csrf_exempt
def save_fcm_token(request):
    if request.method != 'POST':
        return JsonResponse({'status': 'error', 'message': 'Invalid request method.'})
    
    username = request.POST.get('username')
    fcm_token = request.POST.get('fcm_token')

    if not username or not fcm_token:
        return JsonResponse({'status': 'error', 'message': 'Username and FCM token are required.'})
    
    conn = get_connection()
    if not conn:
        return JsonResponse({'status': 'DB error', 'message': 'Database connection failed.'})
    
    cursor = conn.cursor()
    try:
        cursor.execute("""INSERT INTO user_tokens (username, fcm_token) 
                       VALUES (%s, %s) 
                       ON DUPLICATE KEY UPDATE fcm_token=%s, last_updated=NOW()""", 
                       (username, fcm_token, fcm_token))
        conn.commit()
        return JsonResponse({'status': 'success', 'message': 'FCM token saved successfully.'})
    except Exception as e:
        return JsonResponse({'status': 'error', 'message': str(e)})
    finally:
        cursor.close()
        conn.close()

@csrf_exempt
def user_receive_history(request):
    receiver = request.POST.get('username')

    if not receiver:
        response = {
            'status': 'error',
            'message': 'Username is required.'
        }
        return JsonResponse(response)

    if request.method != 'POST':
        response = {
            'status': 'error',
            'message': 'Invalid request method.'
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
    
    # We use a JOIN to get file names and info in one go
    # Grouping by unique_text helps if there are multiple files per code
    query = """
        SELECT fi.sender, fi.receiver, fi.unique_text, fi.receiving_time, fb.file_name
        FROM file_info fi
        LEFT JOIN file_blobs fb ON fi.unique_text = fb.unique_text
        WHERE fi.receiver = %s
        ORDER BY fi.receiving_time DESC
    """
    
    cursor.execute(query, (receiver,))
    results = cursor.fetchall()
    

    # Organize the flat results into a structured format
    history_dict = {}
    for sender, receiver, unique_text, timestamp, file_name in results:
        
        if unique_text not in history_dict:
            if timestamp:
                timestamp += timedelta(hours=6)  # Add 6 hours to convert UTC to Dhaka time
            history_dict[unique_text] = {
                'sender': sender,
                'receiver': receiver,
                'unique_text': unique_text,
                'timestamp': timestamp.strftime('%Y-%m-%d   %I:%M %p') if timestamp else None,
                'file_names': []
            }
        if file_name:
            history_dict[unique_text]['file_names'].append(file_name)
    
    cursor.close()
    conn.close()

    # Convert the dictionary back to a list for the JSON response
    return JsonResponse({
        'status': 'success', 
        'data': list(history_dict.values())
    })

@csrf_exempt
def user_sent_history(request):
    sender = request.POST.get('username')

    if not sender:
        response = {
            'status': 'error',
            'message': 'Username is required.'
        }
        return JsonResponse(response)

    if request.method != 'POST':
        response = {
            'status': 'error',
            'message': 'Invalid request method.'
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
    

    query = """
        SELECT fi.sender, fi.receiver, fi.unique_text, fi.sending_time, fb.file_name
        FROM file_info fi
        LEFT JOIN file_blobs fb ON fi.unique_text = fb.unique_text
        WHERE fi.sender = %s
        ORDER BY fi.sending_time DESC
    """
    
    cursor.execute(query, (sender,))
    results = cursor.fetchall()
    

    history_dict = {}
    for sender, receiver, unique_text, timestamp, file_name in results:
        if unique_text not in history_dict:
            if timestamp:
                timestamp += timedelta(hours=6)  # Convert UTC to Dhaka time
            history_dict[unique_text] = {
                'sender': sender,
                'receiver': receiver,
                'unique_text': unique_text,
                'timestamp': timestamp.strftime('%Y-%m-%d   %I:%M %p') if timestamp else None,
                'file_names': []
            }
        if file_name:
            history_dict[unique_text]['file_names'].append(file_name)
    
    cursor.close()
    conn.close()


    return JsonResponse({
        'status': 'success', 
        'data': list(history_dict.values())
    })

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
                      WHERE email=%s AND otp_code=%s AND is_verified=FALSE AND expires_at > NOW()""", 
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