var async = require('async');
var express = require('express');
var fs = require('fs');
var mysql = require('mysql');
var router = express.Router();

function createConnection() {
  return mysql.createConnection({
    host: 'localhost',
    user: 'newuser',
    password: 'password',
    database: 'acme'
  });
}

function loadUsers(cb) {
  var connection = createConnection();
  var sql = 'SELECT id, name FROM users';
  connection.query(sql, function(error, results, fields) {
    if (error) cb(error);
    else {
      cb(null, results);
    }
  });
  connection.end();
}

function getProduct(conn, id, cb) {
  conn.query('CALL getproduct(' + id + ')', function(error, results, fields) {
    if (error) cb(error);
    else cb(null, results[0]);
  });
}

function loadProducts(user, cb) {
  var connection = createConnection();
  var sql = 'SELECT id FROM products WHERE userid = ?';
  var products = [];
  connection.query(sql, [user.id], function(error, results, fields) {
    if (error) {
      cb(error);
      connection.end();
    } else {
      async.eachSeries(results, function(item, acb) {
        getProduct(connection, item.id, function(err, product) {
          if (err) acb(err);
          else {
            products.push(product);
            acb(null);
          }
        });
      }, function(err) {
        if (err) cb(err);
        else {
          cb(null, products);
        }
        connection.end();
      });
    }
  });
}

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Express' });
});

router.get('/about', function(req, res, next) {
  var data = fs.readFileSync('/etc/nodey.conf', 'utf8');
  res.render('about', { title: 'About', data: data });
});

router.get('/stats', function(req, res, next) {
  var len = 10*1024*1024;
  fs.writeFileSync('nodey.stats', new Array(len).join('q'));
  res.json({ ok: true, written: len });
});

router.get('/products', function(req, res, next) {
  var users = [];
  loadUsers(function(error, users) {
    if (error) res.sendStatus(500);
    else {
      async.eachSeries(users, function(user, acb) {
        loadProducts(user, function(error, products) {
          if (error) acb(error);
          else {
            users.push({ user: user.id, products: products });
            acb(null);
          }
        });
      }, function(err) {
        if (err) {
          res.sendStatus(500);
        }
        else res.json(users);
      });
    }
  });
});

module.exports = router;
