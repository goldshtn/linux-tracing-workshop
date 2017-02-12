drop database if exists acme;
create database acme;
use acme;

create table users (id integer primary key, name varchar(200));
create table products (id integer primary key, userid integer,
                       name varchar(200), description varchar(200),
                       price double);

delimiter //
create procedure initialize()
begin
        declare i int default 0;
        while i < 100 do
                insert into users values (i, "Dave");
                insert into products values (2 * i, i, "Milk", "", 4.52);
                insert into products values (2 * i + 1, i, "Bread", "", 1.88);
                set i = i + 1;
        end while;
end //
delimiter ;

call initialize();
drop procedure initialize;

delimiter //
create procedure getproduct(in pid integer)
begin
        if pid = 97 then
                do sleep(2);
        end if;
        select * from products where id = pid;
end //
delimiter ;
