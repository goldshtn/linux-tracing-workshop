var crypto = require('crypto');
var express = require('express');
var fs = require('fs');
var router = express.Router();

var users = [];

function clean_user(name, info) {
  var processed_salt = '';
  for (var i = 0; i < 100000; ++i) {
    var size = Math.floor(Math.random() * info.salt.length);
    processed_salt += info.salt.slice(0, size);
  }
  return { name: name, len: processed_salt.length };
}

function EmailTemplateNode(index) {
  this.index = index;
  this.index_desc = index.toString();
}

function EmailTemplate(template) {
  this.data = fs.readFileSync(__dirname + template, 'utf8');
  this.nodes = [];
  var node_count = Math.random()*1000;
  for (var i = 0; i < node_count; ++i) {
    this.nodes.push(new EmailTemplateNode(i));
  }
}

function fetch_template() {
  return new EmailTemplate('/../public/template.html');
}

/* GET users listing. */
router.get('/', function(req, res, next) {
  var clean_users = [];
  for (var user in users) {
    clean_users.push(clean_user(user, users[user]));
  }
  res.json(clean_users);
});

router.post('/subscribe', function(req, res, next) {
  var subscriptionTemplate = null;
  function refreshSubscription() {
    var original = subscriptionTemplate;
    var first_test = function() {
      if (original)
        console.log("Replacing original subscription.");
    };
    subscriptionTemplate = {
      template: fetch_template(),
      send: function() {
        console.log("Sending updated subscription.");
      }
    };
    first_test();
  }
  setInterval(refreshSubscription, 1000);
  res.sendStatus(200);
});

router.post('/new', function(req, res, next) {
  var username = req.query.username || '';
  var password = req.query.password || '';

  username = username.replace(/[!@#$%^&*]/g, '');

  if (!username || !password || users.username) {
    return res.sendStatus(400);
  }

  var salt = crypto.randomBytes(512).toString('base64');
  var hash = crypto.pbkdf2Sync(password, salt, 10000, 512, 'sha512');

  users[username] = {
    salt: salt,
    hash: hash
  };

  res.sendStatus(200);
});

router.post('/auth', function(req, res, next) {
  var username = req.query.username || '';
  var password = req.query.password || '';

  username = username.replace(/[!@#$%^&*]/g, '');

  if (!username || !password || !users[username]) {
    return res.sendStatus(400);
  }

  var hash = crypto.pbkdf2Sync(
      password, users[username].salt, 10000, 512, 'sha512');

  if (users[username].hash.toString() === hash.toString()) {
    res.sendStatus(200);
  } else {
    res.sendStatus(401);
  }
});

module.exports = router;
