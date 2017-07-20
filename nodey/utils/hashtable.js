function HashTable(hasher, comparer, n) {
  this._hasher = hasher;
  this._comparer = comparer;
  this._buckets = new Array(n || 131);
  this._count = 0;
}

HashTable.prototype.put = function(key, value) {
  var k = this._hasher(key) % this._buckets.length;
  var bucket = this._buckets[k];
  if (bucket == null) {
    bucket = new Array();
    this._buckets[k] = bucket;
  } else {
    for (var i = 0; i < bucket.length; ++i) {
      if (this._comparer(bucket[i].key, key)) {
        bucket[i].value = value;
        return;
      }
    }
  }
  bucket.push({ key: key, value: value });
  ++this._count;
};

HashTable.prototype.get = function(key) {
  var k = this._hasher(key) % this._buckets.length;
  var bucket = this._buckets[k];
  if (bucket == null)
    return null;
  for (var i = 0; i < bucket.length; ++i) {
    if (this._comparer(bucket[i].key, key)) {
      return bucket[i].value;
    }
  }
  return null;
};

HashTable.prototype.size = function() {
  return this._count;
};

HashTable.prototype.remove = function(key) {
  var k = this._hasher(key) % this._buckets.length;
  var bucket = this._buckets[k];
  if (bucket == null)
    return;
  for (var i = 0; i < bucket.length; ++i) {
    if (this._comparer(bucket[i].key, key)) {
      bucket.splice(i, 1);
      --this._count;
      return;
    }
  }
};

HashTable.prototype.clear = function(key) {
  this._count = 0;
  this._buckets = new Array(this._buckets.length);
};

HashTable.prototype.dump = function() {
  console.log('hashtable with ' + this._count + ' elements:');
  for (var i = 0; i < this._buckets.length; ++i) {
    var bucket = this._buckets[i];
    if (bucket != null) {
      console.log('  bucket ' + i + ' with ' + bucket.length + ' elements:');
      for (var j = 0; j < bucket.length; ++j) {
        console.log('    element ' + j + ': key = ' + bucket[j].key +
                    ', value = ' + bucket[j].value);
      }
    }
  }
};

module.exports = HashTable;
