create database if not exists file_share_db;
use file_share_db;

create table if not exists user_credentials(
	id int primary key auto_increment, 
    username text,
    hashed_password text	
)

create table if not exists file_info(
	id int primary key auto_increment, 
    sender text,
    unique_text text,
    receiver text
)