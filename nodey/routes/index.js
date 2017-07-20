var async = require('async');
var express = require('express');
var fs = require('fs');
var HashTable = require('../utils/hashtable');
var mysql = require('mysql');
var pos = require('../utils/position');
var request = require('request');
var router = express.Router();

var positions = new HashTable(pos.hasher, pos.comparer, 1337);

function prime_product(product_id) {
  return product_id.startsWith('g');
}

function dynamic_product(product_id) {
  return product_id.startsWith('dyn');
}

function dynamic_inventory_service() {
  return 'http://' + Math.trunc(Math.random() * 100000).toString()
                   + '.example.org/inventory?svc=sdyn199';
}

function inventory_services(product_id) {
  var services = fs
           .readFileSync(__dirname + '/../inventory.lst', 'utf-8')
           .split('\n')
           .filter(Boolean)
           .filter(function(s) { return !s.startsWith('#'); });
  if (!prime_product(product_id)) {
    services.splice(2, 1);
  }
  if (dynamic_product(product_id)) {
    services.push(dynamic_inventory_service());
  }
  return services.map(function(s) { return s + '&product_id=' + product_id; });
}

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

function authenticate_stats(key, cb) {
  setTimeout(function() {
    if (key == 'mykey')
      cb(null);
    else
      cb(new Error('invalid key'));
  }, 1000);
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

router.put('/stats', function(req, res, next) {
  var stats = req.body;
  authenticate_stats(stats.auth, function(err) {
    if (err)
      throw err;
    res.sendStatus(201);
  });
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

router.get('/inventory', function(req, res, next) {
  var services = inventory_services(req.query.product_id);
  async.mapSeries(services, request, function(err, results) {
    res.json({
      data: results.filter(Boolean).map(function(r) {
        var body = JSON.parse(r.body);
        return body.args;
      }),
      error: err ? "an error occurred enumerating one of the inventory services"
                 : null
    });
  });
});

router.post('/position', function(req, res, next) {
  var x = req.query.x, y = req.query.y, z = req.query.z;
  var position = new pos.Position(x, y, z);
  if (positions.get(position) != null)
    res.sendStatus(400);
  else {
    positions.put(position, 1);
    res.sendStatus(201);
  }
});

module.exports = router;
