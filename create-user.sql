drop user if exists 'newuser'@'localhost';
create user 'newuser'@'localhost' identified by 'password';
grant all privileges on *.* to 'newuser'@'localhost' with grant option;
