var express = require('express');
var fs = require('fs');
var router = express.Router();

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

module.exports = router;
