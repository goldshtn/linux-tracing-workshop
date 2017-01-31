create table if not exists test (value integer);
do
    $do$
    begin
        for i in 1..1000000 loop
            insert into test values (i);
        end loop;
    end $do$;

select count(*) from test where value % 17 = 0;
select count(*) from test where (value * 3) % 17 = 0;
select count(*) from test;
select avg(value) from test;
select max(value) from test where value % 3 = 1;
