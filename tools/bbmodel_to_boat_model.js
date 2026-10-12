// Turns the cannon and mast parts of assets/Fisherman/boat.bbmodel into the Java geometry the boat renderer draws
// (smart-villager-professions/.../client/render/BoatUpgradeModel.java). Run after editing the model in Blockbench:
//   node tools/bbmodel_to_boat_model.js
// The double chest comes from its own file, assets/Fisherman/boats/chest_boat_doublechest.bbmodel.
// The hull and the paddles are not exported: the hull is Vanilla's own boat. The sail's flag cube ("banner_flag") is not exported as a cube either, only
// where it sits (BoatUpgradeModel.FLAG): Vanilla's banner code draws it with the banner's colour and patterns (client/render/BoatUpgradeRenderer).
// Groups by name: "boat_cannon" is the cannon, "boat_sail" the mast with its sail, "boat_stern" the stern deck.
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const model = JSON.parse(fs.readFileSync(path.join(root, 'assets', 'Fisherman', 'boat.bbmodel'), 'utf8'));
const out = path.join(root, 'smart-villager-professions', 'src', 'main', 'java', 'com', 'smartvillager', 'professions', 'client', 'render', 'BoatUpgradeModel.java');

const CANNON_GROUPS = ['boat_cannon'];
const SAIL_GROUPS = ['boat_sail'];
const STERN_GROUPS = ['boat_stern'];
const FLAG_CUBE = 'banner_flag';
const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];

const byUuid = Object.fromEntries(model.elements.map(e => [e.uuid, e]));
const groupsByUuid = Object.fromEntries((model.groups || []).map(g => [g.uuid, g]));
const textures = Object.fromEntries(model.textures.map(t => [t.id, t]));

const num = v => {
  const s = String(Math.round(v * 100000) / 100000);
  return (s.includes('.') ? s : s + '.0') + 'F';
};
const arr = a => 'new float[]{' + a.map(num).join(', ') + '}';

// the double chest has its own Blockbench file (assets/Fisherman/boats/chest_boat_doublechest.bbmodel); its one texture gets the id 10
const DOUBLE_CHEST_FILE = path.join(root, 'assets', 'Fisherman', 'boats', 'chest_boat_doublechest.bbmodel');
const DOUBLE_CHEST_TEXTURE_ID = 10;

function cubeJava(element, group, ctx) {
  ctx = ctx || { textures, textureId: id => Number(id) };
  const faces = FACES.map(name => {
    const f = element.faces[name];
    if (!f || f.texture === null || f.texture === undefined) return 'null';
    const t = ctx.textures[f.texture];
    const [u1, v1, u2, v2] = f.uv;
    const texId = ctx.textureId(f.texture);
    return `new Face(${texId}, ${num(u1 / t.uv_width)}, ${num(v1 / t.uv_height)}, ${num(u2 / t.uv_width)}, ${num(v2 / t.uv_height)}, ${f.rotation || 0})`;
  });
  return `            new Cube(${arr(element.from)}, ${arr(element.to)}, ${arr(element.origin)}, ${arr(element.rotation || [0, 0, 0])},\n`
    + `                    ${arr(group.origin)}, ${arr(group.rotation || [0, 0, 0])},\n`
    + `                    new Face[]{${faces.join(', ')}})`;
}

function doubleChestJava() {
  const chest = JSON.parse(fs.readFileSync(DOUBLE_CHEST_FILE, 'utf8'));
  const ctx = { textures: Object.fromEntries(chest.textures.map(t => [t.id, t])), textureId: () => DOUBLE_CHEST_TEXTURE_ID };
  const elements = Object.fromEntries(chest.elements.map(e => [e.uuid, e]));
  const groups = Object.fromEntries((chest.groups || []).map(g => [g.uuid, g]));
  const cubes = [];
  for (const node of chest.outliner) {
    const group = Object.assign({}, groups[node.uuid] || {}, node);
    for (const child of group.children) {
      if (typeof child !== 'string') throw new Error('nested groups are not supported: ' + group.name);
      cubes.push(cubeJava(elements[child], group, ctx));
    }
  }
  return cubes.join(',\n');
}

function groupsOf(groupNames) {
  return model.outliner.map(node => Object.assign({}, groupsByUuid[node.uuid] || {}, node)).filter(group => groupNames.includes(group.name));
}

function flagJava() {
  for (const group of groupsOf(SAIL_GROUPS)) {
    for (const child of group.children) {
      const e = byUuid[child];
      if (e.name === FLAG_CUBE) {
        // Vanilla's flag cube (20 x 40 x 1) maps onto this cube: its corner (-10, -44, -2) in Vanilla's model space is the cube's (to.x, to.y, from.z).
        const shift = [e.to[0] - 10, e.to[1] - 44, e.from[2] + 2];
        return `new Flag(${arr(group.origin)}, ${arr(group.rotation || [0, 0, 0])}, ${arr(e.origin)}, ${arr(e.rotation || [0, 0, 0])}, ${arr(shift)})`;
      }
    }
  }
  throw new Error('no ' + FLAG_CUBE + ' cube in ' + SAIL_GROUPS);
}

function partsJava(groupNames) {
  const cubes = [];
  for (const node of model.outliner) {
    const group = Object.assign({}, groupsByUuid[node.uuid] || {}, node);
    if (!groupNames.includes(group.name)) continue;
    for (const child of group.children) {
      if (typeof child !== 'string') throw new Error('nested groups are not supported: ' + group.name);
      if (byUuid[child].name === FLAG_CUBE) continue;
      cubes.push(cubeJava(byUuid[child], group));
    }
  }
  if (cubes.length === 0) throw new Error('no cubes for ' + groupNames);
  return cubes.join(',\n');
}

const source = `package com.smartvillager.professions.client.render;

/**
 * Generated by tools/bbmodel_to_boat_model.js from assets/Fisherman/boat.bbmodel - edit the model in Blockbench and run the script, not this file.
 * The cannon, the sail and the stern of an upgraded boat in the model's own coordinates: pixels, Y up, bow towards -X, the hull's middle at X=0 Z=0,
 * Y=0 at the bottom of the boat's hitbox (BoatUpgradeRenderer turns them into Vanilla's boat space). Per cube: from, to, its own pivot and
 * rotation (degrees), its group's pivot and rotation, and six faces (north, east, south, west, up, down; null = not drawn) with a
 * texture id (0 = the boat's own wood, 1 = sail, 2 = cannon, 3 = railings, 10 = double chest), UV corners as 0..1 fractions of that texture, and a UV rotation.
 */
final class BoatUpgradeModel {

    record Face(int texture, float u1, float v1, float u2, float v2, int rotation) {
    }

    record Cube(float[] from, float[] to, float[] origin, float[] rotation, float[] groupOrigin, float[] groupRotation, Face[] faces) {
    }

    static final Cube[] CANNON = {
${partsJava(CANNON_GROUPS)}
    };

    static final Cube[] SAIL = {
${partsJava(SAIL_GROUPS)}
    };

    static final Cube[] STERN = {
${partsJava(STERN_GROUPS)}
    };

    /** The double chest (two chests in neighbouring slots), texture id 10; renderer centres it and turns it along the boat. */
    static final Cube[] DOUBLE_CHEST = {
${doubleChestJava()}
    };

    /** Where the sail's flag sits: its group's and its own pivot and rotation, and the shift from Vanilla's flag cube onto it. */
    record Flag(float[] groupOrigin, float[] groupRotation, float[] cubeOrigin, float[] cubeRotation, float[] shift) {
    }

    static final Flag FLAG = ${flagJava()};

    private BoatUpgradeModel() {
    }
}
`;
fs.writeFileSync(out, source);
console.log('wrote ' + path.relative(root, out));
