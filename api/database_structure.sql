create table if not exists user_credentials (
	id int primary key auto_increment, 
	fullname text,
	username text,
	email text,
	profile_picture mediumblob,
	hashed_password text,
    timestamp timestamp default current_timestamp
);

create table if not exists file_info (
	id int primary key auto_increment, 
	sender text,
	unique_text text,
	receiver text,
	timestamp timestamp default current_timestamp
);

create table if not exists file_blobs (
	id int primary key auto_increment, 
	unique_text text,
	file_name text,
	file_blob longblob,
	timestamp timestamp default current_timestamp
);
