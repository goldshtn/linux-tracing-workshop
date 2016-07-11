#!/usr/bin/env python

from random import randint
import sys
import mysql.connector

NUM_EMPLOYEES = 10000

def insert():
    cursor = connection.cursor()
    try:
        cursor.execute("drop table employees")
    except:
        pass
    cursor.execute("create table employees (id integer primary key, name text)")
    cursor.close()

    print("Inserting employees...")
    for n in xrange(0, NUM_EMPLOYEES):
        cursor = connection.cursor()
        cursor.execute("insert into employees (id, name) values (%d, 'Employee_%d')" %
                       (n, n))
        connection.commit()
        cursor.close()

def select():
    print("Selecting employees...")
    while True:
        cursor = connection.cursor()
        cursor.execute("select * from employees where name like '%%%d'" % randint(0, NUM_EMPLOYEES))
        for row in cursor:
            pass
        cursor.close()

connection = mysql.connector.connect(host='localhost', database='test', user='root')

if "insert" in sys.argv:
    while True:
        insert()
elif "insert_once" in sys.argv:
    insert()
elif "select" in sys.argv:
    select()
else:
    print("USAGE: data_access.py <insert|insert_once|select>")

connection.close()
