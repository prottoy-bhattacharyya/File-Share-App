import mysql.connector


def upload_blob(file_path, unique_text, file_name):
    conn = None
    try:
        # 1. Connect to the database
        conn = mysql.connector.connect(
            host="localhost",
            user="root",
            password="",
            database="file_share_db"
        )
        cursor = conn.cursor()
        # 2. Read the file as binary data
        with open(file_path, 'rb') as file:
            blob_data = file.read()
        # 3. Execute INSERT query to store BLOB data
        query = "INSERT INTO file_blobs (unique_text, file_name, file_blob) "\
                "VALUES (%s, %s, %s)"
        cursor.execute(query, (unique_text, file_name, blob_data))
        conn.commit()


        cursor.close()
        conn.close()
        print("File uploaded successfully.")

    except mysql.connector.Error as err:
        print(f"Error: {err}")
            



def download_blob(id, output_path):
    conn = None
    try:
        # 1. Connect to the database
        conn = mysql.connector.connect(
            host="localhost",
            user="root",
            password="",
            database="file_share_db"
        )
        cursor = conn.cursor()

        # 2. Execute SELECT query to get BLOB data
        query = "SELECT file_blob FROM file_blobs WHERE id = %s"
        cursor.execute(query, (id,))
        
        # 3. Fetch the binary data
        record = cursor.fetchone()
        if record:
            blob_data = record[0]
            
            # 4. Write binary data to a file
            with open(output_path, 'wb') as file:
                file.write(blob_data)
            print(f"File saved successfully to {output_path}")
        else:
            print("No record found with that ID.")
        
        cursor.close()
        conn.close()

    except mysql.connector.Error as err:
        print(f"Error: {err}")


# Example usage:
upload_blob("D:/project/android/File-Share/api/media/exteGt/download.png", "unique_text", "download.png")

# Example usage:
download_blob(1, "downloaded_download.png")
