#!/bin/bash

sudo /usr/local/mysql/bin/mysql --user=root < ./create-user.sql
/usr/local/mysql/bin/mysql --user=newuser --password=password < ./create-db.sql
