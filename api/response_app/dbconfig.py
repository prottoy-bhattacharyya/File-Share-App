import os

def db():
    # config = {
    #     'host': os.environ.get('DB_HOST'),
    #     'user': os.environ.get('DB_USER'),
    #     'password': os.environ.get('DB_PASSWORD'),
    #     'port': os.environ.get('DB_PORT'),
    #     'database': os.environ.get('DB_NAME')
    # }

    config = {
        'host': 'localhost',
        'user': 'root',
        'password': '',
        'port': '3306',
        'database': 'file_share_db'
    }
    return config