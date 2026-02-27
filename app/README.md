# File Share Android App

A file-sharing Android application that enables users to upload and share files through QR code scanning, built with Java and powered by Django REST API.

##  Features

- **Multi-File Upload**: Upload single or multiple files simultaneously to the server
- **QR Code Sharing**: Generate QR codes for file sharing - receivers can download files by simply scanning
- **User Authentication**: Secure login and signup functionality
- **Transfer History**: Automatically saves and displays user's send and receive history
- **Local Storage**: Efficiently stores user information locally after authentication

##  Future Works
- **⚡ Fast & Reliable**: Quick file transfers with real-time progress tracking

##  Tech Stack

- **Frontend**: Java (Android Native)
- **Backend**: Django REST Framework
- **Database**: MySQL

##  Prerequisites

Before running this project, ensure you have:

- Android Studio (latest version recommended)
- JDK 8 or higher
- Python 3.8+
- MySQL Server
- Android device or emulator (API level 21+)

##  Installation

### Backend Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/file-share-android.git
cd file-share-android
```

2. Configure MySQL database in `dbconfig.py`:
```python
DATABASES = {
    'default': {
        'USER': 'root',
        'PASSWORD': '1234',
        'HOST': 'localhost',
        'PORT': '3306',
        'database'; 'file_share_app'
    }
}
```

3. Start the Django server:
```bash
python -m uv run manage.py runserver 0.0.0.0:8000
```

### Android App Setup

1. Open the `android` folder in Android Studio

2. Update the API base URL in the app configuration:
```java
// In res/values/strings.xml file
<string name="server_url">your_server_ip:8000</string>
```

3. Sync Gradle files and build the project

4. Run the app on your device or emulator

##  Usage

### For Sender:
1. **Sign Up/Login**: Create an account or log in to existing one
2. **Upload Files**: Select single or multiple files from your device
3. **Generate QR Code**: After upload, a unique QR code is generated
4. **Share**: Show the QR code to the receiver

### For Receiver:
1. **Scan QR Code**: Open the app and use the QR scanner
2. **Download Files**: Files are automatically downloaded to your device
3. **View History**: Check your receive history anytime

##  Screenshots
<img width="300" alt="image" src="https://github.com/user-attachments/assets/59cdc9fa-bd03-446f-8bce-5f3813ba9cde" />
<img width="300" alt="image" src="https://github.com/user-attachments/assets/b0c8773f-45ed-4e29-bfa2-8254efe7d85e" />
<img width="300" alt="image" src="https://github.com/user-attachments/assets/9ff4f6d3-895d-49b7-b7c7-5654825ba379" />


##  API Endpoints


## Known Issues

- Large files download may failed




 If you found this project helpful, please give it a star!

## 📞 Support

For support, email prottoyvhattacharyya@gmail.com or open an issue in the repository.
