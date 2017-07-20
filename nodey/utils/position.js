function Position(x, y, z) {
  this._x = x;
  this._y = y;
  this._z = z;
  this._extra = Array(10001).join('*');
}

function hasher(pos) {
  return pos._z;
}

function comparer(pos1, pos2) {
  return pos1._extra == pos2._extra &&
         pos1._x == pos2._x && pos1._y == pos2._y && pos1._z == pos2._z;
}

module.exports = {
  Position: Position,
  hasher: hasher,
  comparer: comparer
};
