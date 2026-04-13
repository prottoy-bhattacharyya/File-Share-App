# File Share Android App

A file-sharing Android application that enables users to upload and share files through QR code scanning, built with Java and powered by Django REST API.

##  Features
- **User Authentication**: User can create account and login
- **Multiple file Upload and Download**: Upload files to database (4.5 MB max size for each file because of free vercel account)
- **QR Code Sharing**: Generate QR codes for file sharing - receivers can download files by simply scanning
- **Account Recovery and Verification**: User can reset or verify the account via OTP sent to email
- **Transfer History**: Automatically saves and displays user's send and receive history


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

##  Installation

### Backend Setup

1. Open `api` folder in your IDE:

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
3. Goto `goggle apppassword` and get your password
Configure Email in `settings.py`
```python
    EMAIL_HOST_USER = "your gmail"
    EMAIL_HOST_PASSWORD = "your password"
```

4. Start the Django server:
```bash
python -m uv run manage.py runserver 0.0.0.0:8000
```

### Android App Setup

1. Open the `app` folder in Android Studio

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



##  API Endpoints


## Known Issues

- Large files download may failed




 If you found this project helpful, please give it a star!

## 📞 Support

For support, email prottoyvhattacharyya@gmail.com or open an issue in the repository.
