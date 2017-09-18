#!/bin/bash

ps aux | grep mysqld | grep -vq grep
if [ $? -eq 1 ]; then
    sudo -u mysql /usr/local/mysql/bin/mysqld_safe --user=mysql &
fi
exit

sudo /usr/local/mysql/bin/mysql --user=root < ./create-user.sql
/usr/local/mysql/bin/mysql --user=newuser --password=password < ./create-db.sql
