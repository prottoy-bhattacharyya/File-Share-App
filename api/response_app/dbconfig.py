import os

def db():
    config = {
        'host': os.environ.get('DB_HOST'),
        'user': os.environ.get('DB_USER'),
        'password': os.environ.get('DB_PASSWORD'),
        'port': os.environ.get('DB_PORT'),
        'database': os.environ.get('DB_NAME')
    }
    return config