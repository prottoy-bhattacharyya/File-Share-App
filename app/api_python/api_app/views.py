from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import render
from django.http import JsonResponse
import mysql.connector
from .mysql_db_config import db_config
# Create your views here.

def get_db_connection():
    try:
        conn = mysql.connector.connect(**db_config())
        print(conn)
        return conn
    
    except mysql.connector.Error as err:
        print(f"Error connecting to database: {err}")
        return None
    

def get_data(request):
    conn = get_db_connection()
    if conn is None:    
        print("Failed to connect to the database.")
        response = {
            "status": "error",
            "message": "Failed to connect to the database."
        }
        return JsonResponse(response)
    
    cursor = conn.cursor(dictionary = True)
    try:
        cursor.execute("SELECT * FROM `table1`;")
    except mysql.connector.Error as err:
        response = {
            "status": "error" + str(err),
            "message": "Error fetching data"
        }
        return JsonResponse(response)
    
    data = cursor.fetchall()
    cursor.close()
    conn.close()

    response = {
        "status": "success",
        "data": data
    }

    return JsonResponse(response, safe=False)

@csrf_exempt
def post_data(request):
    conn = get_db_connection()
    if conn is None:
        response = {
            "status": "error",
            "message": "Failed to connect to the database."
        }
        return JsonResponse(response)

    cursor = conn.cursor()
    name = request.POST.get('name')
    email = request.POST.get('email')
    try:
        cursor.execute("""INSERT INTO `table1` (name, email) 
                    VALUES (%s, %s);""", 
                    (name, email))
        conn.commit()
    
    except mysql.connector.Error as err:
        response = {
            "status": "error" + str(err),
            "message": "Error inserting data"
        }
        return JsonResponse(response)
    
    cursor.close()
    conn.close()
    response = {
        "status": "success",
        "message": "Data inserted successfully"
    }
    return JsonResponse(response)

