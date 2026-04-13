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
    config = {
        'host': 'localhost',
        'user': 'root',
        'password': '',
        'port': '3306',
        'database': 'file_share_db'
    }
```
3. Web Search `goggle apppassword` and get your password
Configure Email in `settings.py`
```python
    EMAIL_HOST_USER = "your gmail"
    EMAIL_HOST_PASSWORD = "your password"
```

4. Start the Django server inside `api` folder:
```bash
    pip install uv
    python -m uv run manage.py runserver 0.0.0.0:8000
```

### Android App Setup

1. Open the `app` folder in Android Studio

2. Update the API base URL in the app configuration:
```java
// In res/values/strings.xml file
<string name="server_url">your_server_ip:8000</string>
```
3. If you are using `http` url, then add it here.
```java
    // In res/xml/network_security_config.xml file
    <?xml version="1.0" encoding="utf-8"?>
    <network-security-config>
        <domain-config cleartextTrafficPermitted="true">
            <domain includeSubdomains="true">192.168.1.215</domain> 
        </domain-config>
    </network-security-config>
```
4. Sync Gradle files and build the project

5. Run the app on your device or emulator


##  Screenshots
<img src="Screenshots/main.png" alt="Home Page" width="150"> <img src="Screenshots/profile.png" alt="Project Logo" width="150"> <img src="Screenshots/received files.png" alt="Project Logo" width="150">

<img src="Screenshots/upload files.png" alt="Project Logo" width="150"> <img src="Screenshots/qr scan.png" alt="Project Logo" width="150"> <img src="Screenshots/receiving files.png" alt="Project Logo" width="150">





##  API Endpoints


## Known Issues

- Can't Upload or download greater than 4.5 MB file


 If you found this project helpful, please give it a star!

## 📞 Support

For support, email prottoyvhattacharyya@gmail.com or open an issue in the repository.
