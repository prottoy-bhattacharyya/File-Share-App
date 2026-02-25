from django.http import FileResponse, HttpResponse, JsonResponse
from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth.hashers import make_password, check_password
import os
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
        return HttpResponse("<h1>Database connection failed.</h1>")
    
    cursor = conn.cursor()
    try:

        cursor.execute("""create table if not exists user_credentials(
                        id int primary key auto_increment, 
                        username text,
                        hashed_password text	
                        );""")
        
        cursor.execute("""create table if not exists file_info(
                            id int primary key auto_increment, 
                            sender text,
                            unique_text text,
                            receiver text
                        );""")
        
        conn.commit()

        cursor.execute("SHOW TABLES;")
        result = cursor.fetchall()
    except mysql.connector.Error as err:
        return HttpResponse(f"<h1>Database table creation failed: {err}</h1>")
    cursor.close()
    conn.close()

    tables = str(result)
    return HttpResponse("<h1>Database Ready. TABLES: " + tables + "</h1>")

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



def check_login(request):

    username = request.GET.get('username')
    password = request.GET.get('password')

    if not username or not password:
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
    cursor.execute("""SELECT username, hashed_password
                    FROM user_credentials
                    WHERE username = %s""", 
                    (username,)
                )
    result = cursor.fetchone()
    cursor.close()
    conn.close()

    if result:
        valid_username, valid_hashed_password = result
    else:
        response = {
            'status': 'user error',
            'message': 'User not found.'
        }
        if cursor:
            cursor.close()
        if conn:
            conn.close()
        return JsonResponse(response, status=404)
    

    if username == valid_username and check_password(password, valid_hashed_password):
        response = {
            'status': 'success',
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
    
    return JsonResponse(response)


def signup(request):
    username = request.GET.get('username')
    password = request.GET.get('password')

    if not username or not password:
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
    
    hashed_password = make_password(password)

    cursor = conn.cursor()
    cursor.execute("INSERT INTO user_credentials (username, hashed_password) VALUES (%s, %s)", (username, hashed_password))
    conn.commit()
    cursor.close()
    conn.close()

    response = {
        'status': 'success',
        'message': 'Sign up successful.'
    }

    return JsonResponse(response)

def download(request):
    unique_text = request.GET.get('unique_text')
    file_index = int(request.GET.get('file_index'))

    try:
        folder_path = os.path.join('media', unique_text)
        files = os.listdir(folder_path)
        
        filename = files[file_index]
        file_path = os.path.join(folder_path, filename)

    except Exception as e:
        response ={
            'status': 'error',
            'message': 'File not found.' + str(e)
        }
        return JsonResponse(response, status=404)
    
    return FileResponse(open(file_path, 'rb'), as_attachment=True, filename=filename)


def get_file_count(request):
    unique_text = request.GET.get('unique_text')
    folder_path = os.path.join('media', unique_text)
    
    if not os.path.exists(folder_path):
        response ={
            'status': 'error',
            'message': 'File not found.'
        }
        return JsonResponse(response, status=404)
    
    files = os.listdir(folder_path)
    file_count = len(files)

    response = {
        'status': 'success',
        'file_count': file_count,
    }
    
    return JsonResponse(response)

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
def user_info(request):
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


