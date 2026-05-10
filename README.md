<div align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Django-092E20?style=for-the-badge&logo=django&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />

  <h1 align="center">📁 File Share Android App</h1>

  <p align="center">
    A robust cross-platform file-sharing ecosystem. Transfer files via centralized Server storage or Direct connection. 
    <br />
    <b>Scan. Download. Share.</b>
  </p>
</div>

---

## 🚀 Overview

This application enables seamless file sharing through a dual-method approach. Users can upload files to a secure server and share access via **QR Codes** or unique **Download Codes**. Built with **Java (Android Native)** and backed by a powerful **Django REST API**.

### ✨ Key Features

- 🔐 **User Authentication**: Secure sign-up and login protocols.
- 📤 **Bulk Transfers**: Support for multiple file uploads and downloads.
- 📷 **QR Code Integration**: Instant sharing—receivers just scan and go.
- 📧 **Account Recovery**: OTP-based verification and password resets via email.
- 📜 **Transfer History**: Comprehensive logs for both sent and received files.
- 🛡️ **File Verification**: Pre-upload integrity checks to ensure data safety.

---

## 🛠 Tech Stack

| Component | Technology |
| :--- | :--- |
| **Frontend** | Java (Android Native) |
| **Backend** | Django REST Framework |
| **Database** | MySQL |
| **DevOps** | Docker, Docker Compose |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |

---

## 🐳 Quick Start (Docker)

The fastest way to get your server running.

- **Install Docker**: Download and run `Docker` on your machine.
  
- **Email Setup:** Web Search `goggle apppassword` and get your password
- **Environment Variables Setup**: goto `api/`
- add `.env` file and add these lines
 ```bash
  DB_HOST=db
  DB_USER=root
  DB_PASSWORD=1234
  DB_PORT=3306
  DB_NAME=file_share_db

 EMAIL_HOST_PASSWORD=your google app password 
 EMAIL_HOST_USER=your gmail
  ```


- **Setup FCM Admin:** create folder `Services/Firebase` inside `api/` and open it.
- Goto [Firebase Console](https://console.firebase.google.com/) -> `create new project` -> `Service Accounts` -> Select `Python` -> `Generate New Private Key`
- save the private key as `firebase_credentials.json` inside `Services/Firebase` folder.


- Run this command inside `api/` folder
 ```bash
  docker compose up
  ```

> [!TIP]
> goto `localhost:8000`, if it shows
> `Firebase is initialized.
> Database connection successful.
> [('file_blobs',), ('file_info',), ('otp_verifications',), ('user_credentials',), ('user_tokens',)]` then the server is successfully initialized everything.
> else restart the `django_app` container from `Docker` software

- Now begin  [Android App Setup](#android-app-setup)

# Run server without Docker
## Prerequisites

Before running this project, ensure you have:

- Android Studio (latest version recommended)
- JDK 8 or higher
- Python 3.8+
- MySQL Server
- Firebase Console Account

# Installation

Setup Backend Djano Server, MySQL, Firebase Cloude Messaging (for Notification) and the App's Server URL

## Backend Setup

- Open `api` folder in your IDE:

- **Database Setup:** Configure MySQL database in `dbconfig.py`:

```python
config = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'port': '3306',
    'database': 'file_share_db'
}
```

- **Email Setup:** Web Search `goggle apppassword` and get your password
  Configure Email in `api/ file_sharing_project/ settings.py`

```python
EMAIL_HOST_USER = "your gmail"
EMAIL_HOST_PASSWORD = "your password"
```

- **Setup FCM Admin:** Goto [Firebase Console](https://console.firebase.google.com/) -> `create new project` -> `Service Accounts` -> Select `Python` -> `Generate New Private Key`
- new json file will be downloaded.
- rename to `firebase_credentials.json`
- Save it to `api/Services/Firebase/firebase_credentials.json`
> [!WARNING]
> If path does not exists, then create `Services/Firebase` folder

- **Start** the Django server inside `api` folder:

```bash
pip install uv
```

```bash
python -m uv run manage.py runserver 0.0.0.0:8000
```

UV will download all the dependencies and start the server

> [!TIP]
> If terminal shows this when you first run the server, then the server started successfully.

```bash
Database tables created successfully.
Firebase initialized successfully.
... ...
```

## Android App Setup

### setup Server URL

- Open the `app` folder in Android Studio

- Update the API base URL in the app configuration (`app/res/values/strings.xml`):

```java
// In res/values/strings.xml file
<string name="server_url">your_server_ip:8000</string>
```

- If you are using `http` url, then add it here.

```java
// In res/xml/network_security_config.xml file
<domain includeSubdomains="true">123.123.123.123</domain>
```

### setup FCM (To Send Notifications)

- Goto `Android Studio` -> `Tools` (in the top) -> `Firebase` -> `Connect to Firebase` -> `Cloud Messaging` -> `connect the app`
- google-services.json folder will be added in your app folder.

> [!TIP]
> If you are confused, Search `How to connect Android Studio with Firebase` on Youtube

- Sync Gradle files and build the project

- Run the app on your device or emulator

## 📱 Screenshots

<div align="center">
  <table>
    <tr>
      <td><img src="Screenshots/main.png" width="180" alt="Home"></td>
      <td><img src="Screenshots/upload%20files.png" width="180" alt="Upload"></td>
      <td><img src="Screenshots/qr%20scan.png" width="180" alt="QR Scan"></td>
      <td><img src="Screenshots/receiving%20files.png" width="180" alt="Receiving"></td>
    </tr>
    <tr>
      <td align="center"><b>Home</b></td>
      <td align="center"><b>Upload</b></td>
      <td align="center"><b>Scanner</b></td>
      <td align="center"><b>Transfer</b></td>
    </tr>
    <tr>
      <td><img src="Screenshots/profile.png" width="180" alt="Profile"></td>
      <td><img src="Screenshots/received%20files.png" width="180" alt="Files"></td>
      <td><img src="Screenshots/sent%20history.jpg" width="180" alt="History Sent"></td>
      <td><img src="Screenshots/receive%20history.jpg" width="180" alt="History Rec"></td>
    </tr>
     <tr>
      <td align="center"><b>Profile</b></td>
      <td align="center"><b>File Manager</b></td>
      <td align="center"><b>Sent Logs</b></td>
      <td align="center"><b>Receive Logs</b></td>
    </tr>
  </table>
</div>

---

## 📞 Support & Feedback

<p align="left">
  If you find this project useful, please consider giving it a <b>⭐ Star</b>! <br><br>
  <b>Developer:</b> Prottoy Bhattacharyya <br>
  <b>Email:</b> <a href="mailto:prottoyvhattacharyya@gmail.com">prottoyvhattacharyya@gmail.com</a> <br>
  <b>Issues:</b> <a href="../../issues">Open a Bug Report</a>
</p>

<div align="center">
  <sub>Built with ❤️ for the Developer Community</sub>
</div>
