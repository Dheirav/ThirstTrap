import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';
import Lenis from 'https://cdn.jsdelivr.net/npm/lenis@1.1.18/+esm';

// The same seven framings flythrough.py renders, as (position, target, lens).
// Lens is millimetres on a 36 mm sensor, converted to a vertical FOV below, so
// the numbers stay the ones in world.py rather than becoming browser units.
const PATH = [
  { p:[ 0.25,-1.75, 1.70], t:[ 0.10, 0.60, 1.05 ], lens:24, say:'He watered all four, because that is what looking after something feels like.' },
  { p:[-0.34, 0.10, 1.60], t:[-1.03, 0.99, 1.548], lens:31, say:'It is also how you drown one.' },
  { p:[-0.30,-0.28, 1.21], t:[-0.10, 0.98, 1.03 ], lens:35, say:'Finger in the soil, water only the ones that are dry.' },
  { p:[ 0.02, 0.50, 1.175],t:[-0.245,0.975,1.052], lens:42, say:'The finger reaches an inch.' },
  { p:[-0.60,-0.62, 0.86], t:[-0.12, 0.12, 0.825], lens:45, say:'The pot is twelve deep, and the bottom has been wet for nine days.' },
  { p:[ 0.26,-0.52, 0.95], t:[ 0.31, 0.11, 0.800], lens:55, say:'Water has weight. He started writing the number down.' },
  { p:[ 0.70,-0.52, 0.800],t:[ 0.62,-0.17, 0.762], lens:33, say:'It says so, in eight different ways.' },
];
// Measured against the observal recording over 13 seconds at 15 fps: their
// picture is still for 7% of frames and ours was still for 83%. DWELL applies
// at BOTH ends of every leg, so 0.40 meant 80% of each leg was a hold and only
// 20% was travel. Their captions ride over continuous motion; I had built
// 'hold, travel, hold' on a principle I never checked against the reference.
const Q = new URLSearchParams(location.search);
const DWELL = +(Q.get('dwell') ?? 0.12);

const canvas = document.getElementById('stage3d');
const renderer = new THREE.WebGLRenderer({ canvas, antialias:true });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
// Blender's Standard view transform, which look.py chose deliberately: AgX
// lifts and desaturates its own shadows and this picture has a hard floor.
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.NoToneMapping;

const scene = new THREE.Scene();
scene.background = new THREE.Color(0x0d0a08);
const camera = new THREE.PerspectiveCamera(40, 1, 0.02, 60);

// The gradient map IS the posterise step. plate.py does this in the compositor
// with a constant ColorRamp over the lighting pass; MeshToonMaterial does the
// same thing per fragment, which is what makes this route viable at all.
function gradientMap(steps) {
  const d = new Uint8Array(steps);
  for (let i = 0; i < steps; i++) d[i] = Math.round((i / (steps - 1)) * 255);
  const t = new THREE.DataTexture(d, steps, 1, THREE.RedFormat);
  t.minFilter = t.magFilter = THREE.NearestFilter;
  t.needsUpdate = true;
  return t;
}
const GRAD = gradientMap(5);   // plate.py ships 5 steps

// One warm lamp over the table, at world.py's own key position, plus a cool
// ambient. The HDRI that look.py documents as useless and shipped anyway is
// not recreated here.
// Solved, not guessed. The Cycles gain and floor were derived from the pot's
// own albedo against the plate's lit and shaded pot; these were set by eye and
// blew out 9.6% of the frame where the reference blows out exactly 0.0%.
// Solved by sweeping against the plate's own targets, the same method the
// Cycles gain and floor were solved with: zero blown pixels, mean value 0.28,
// p90 0.60. 26 was set by eye and blew out 57% of the frame at the tightest
// framing. 2.2 lands at 0.00 blown, mean 0.34, p90 0.56.
const key = new THREE.PointLight(0xffcc94, +(Q.get('key') ?? 2.2), 9, 2);
key.position.set(0.60, 1.26, -0.24);          // y-up: Blender's (x, y, z) -> (x, z, -y)
key.castShadow = true;
key.shadow.mapSize.set(2048, 2048);
key.shadow.bias = -0.0015;
scene.add(key);
scene.add(new THREE.AmbientLight(0x39414e, +(Q.get('amb') ?? 0.9)));
const moon = new THREE.DirectionalLight(0x8fa8c8, +(Q.get('moon') ?? 0.35));
moon.position.set(-0.1, 2.1, -2.6);
scene.add(moon);

const loader = new GLTFLoader();
const draco = new DRACOLoader();
draco.setDecoderPath('https://cdn.jsdelivr.net/npm/three@0.169.0/examples/jsm/libs/draco/');
loader.setDRACOLoader(draco);

let ready = false;
loader.load('./room.glb', (gltf) => {
  let meshes = 0, tris = 0;
  gltf.scene.traverse((o) => {
    if (!o.isMesh) return;
    meshes++;
    tris += (o.geometry.index ? o.geometry.index.count : o.geometry.attributes.position.count) / 3;
    const src = Array.isArray(o.material) ? o.material[0] : o.material;
    const emissive = src.emissive && src.emissive.getHex() !== 0;
    o.material = emissive
      // Emission is added unstepped, exactly as the compositor does: the style
      // block allows gradients only for emitted light.
      ? new THREE.MeshBasicMaterial({ color: src.emissive.clone()
          .multiplyScalar(src.emissiveIntensity || 1) })
      : new THREE.MeshToonMaterial({ color: src.color, gradientMap: GRAD });
    o.castShadow = o.receiveShadow = !emissive;
  });
  scene.add(gltf.scene);
  document.getElementById('loader').classList.add('done');
  ready = true;
  window.__stats = { meshes, tris: Math.round(tris) };
}, undefined, (e) => {
  document.getElementById('loader').textContent = 'failed: ' + e;
});

const V = (a) => new THREE.Vector3(a[0], a[2], -a[1]);   // Blender z-up -> three y-up
const smooth = (t) => t * t * (3 - 2 * t);
const ease = (t) => t < DWELL ? 0 : t > 1 - DWELL ? 1 : smooth((t - DWELL) / (1 - 2 * DWELL));
const cat = (p0, p1, p2, p3, t) => {
  const v = new THREE.Vector3();
  const t2 = t * t, t3 = t2 * t;
  for (const k of ['x','y','z'])
    v[k] = 0.5 * (2*p1[k] + (-p0[k]+p2[k])*t + (2*p0[k]-5*p1[k]+4*p2[k]-p3[k])*t2
                  + (-p0[k]+3*p1[k]-3*p2[k]+p3[k])*t3);
  return v;
};
const P = [V(PATH[0].p), ...PATH.map(s => V(s.p)), V(PATH.at(-1).p)];
const T = [V(PATH[0].t), ...PATH.map(s => V(s.t)), V(PATH.at(-1).t)];
const L = [PATH[0].lens, ...PATH.map(s => s.lens), PATH.at(-1).lens];
const legs = PATH.length - 1;

// Smooth scrolling, which is the whole of the difference.
//
// The first version read scrollY straight, so the camera sat on whatever
// discrete position the last wheel tick left: a wheel event is a jump of
// roughly 100 px and the camera teleported that far every notch. Their bundle
// carries Lenis 26 times and this site's own scroll-stage.js has used it with
// lerp 0.09 since it was written, so the lab was the only thing not doing it.
//
// A second, smaller lerp on the camera itself on top of that, because Lenis
// smooths the SCROLL and the camera's path through the room is a different
// curve: easing the position as well takes the last of the steppiness out of
// the corners where two legs meet.
// ?raw=1 turns both off, so the difference can be measured rather than claimed
const RAW = Q.has('raw');
// 0.07 measured lowest jerk of 0.16, 0.10 and 0.07 against the same scripted
// scroll: 5.14 mean and 12.69 p95, against the reference's 2.92 and 7.95.
const DAMP = RAW ? 1.0 : +(Q.get('damp') ?? 0.07);
const lenis = new Lenis({ lerp: 0.085, wheelMultiplier: 0.9 });
let smoothY = 0;
lenis.on('scroll', ({ scroll }) => { smoothY = scroll; });
function raf(time) { lenis.raf(time); requestAnimationFrame(raf); }
requestAnimationFrame(raf);

const beat = document.getElementById('beat'), hud = document.getElementById('hud');
let shown = -1;
const camPos = new THREE.Vector3(), camTgt = new THREE.Vector3();
let camFov = 24, started = false;
window.__trace = [];

const FIXED = Q.has('at') ? +Q.get('at') : null;   // deterministic framing for sweeps
function frame() {
  requestAnimationFrame(frame);
  if (!ready) return;
  const max = document.documentElement.scrollHeight - innerHeight;
  if (FIXED !== null) { smoothY = FIXED * max; }
  const u = THREE.MathUtils.clamp((RAW ? scrollY : smoothY) / Math.max(max, 1), 0, 1) * legs;
  const i = Math.min(Math.floor(u), legs - 1);
  const t = ease(u - i);
  const wantP = cat(P[i], P[i+1], P[i+2], P[i+3], t);
  const wantT = cat(T[i], T[i+1], T[i+2], T[i+3], t);
  const lens = L[i+1] + (L[i+2] - L[i+1]) * t;
  // 36 mm sensor, vertical FOV, so world.py's focal lengths mean the same here
  const wantFov = 2 * Math.atan(12 / lens) * 180 / Math.PI;
  if (!started) { camPos.copy(wantP); camTgt.copy(wantT); camFov = wantFov; started = true; }
  camPos.lerp(wantP, DAMP);
  camTgt.lerp(wantT, DAMP);
  camFov += (wantFov - camFov) * DAMP;
  camera.position.copy(camPos);
  camera.lookAt(camTgt);
  camera.fov = camFov;
  camera.updateProjectionMatrix();
  if (window.__trace.length < 4000) window.__trace.push([camPos.x, camPos.y, camPos.z]);

  const near = t < 0.5 ? i : i + 1;
  if (near !== shown) {
    shown = near;
    beat.textContent = PATH[near].say;
  }
  beat.style.opacity = (t < DWELL * 0.8 || t > 1 - DWELL * 0.8) ? '1' : '0';
  beat.style.visibility = 'visible';
  if (window.__stats)
    hud.textContent = `${window.__stats.meshes} meshes  ${(window.__stats.tris/1000).toFixed(0)}k tris  `
      + `leg ${i+1}/${legs}  ${lens.toFixed(0)}mm`;
  renderer.render(scene, camera);
}

function resize() {
  renderer.setSize(innerWidth, innerHeight, false);
  camera.aspect = innerWidth / innerHeight;
  camera.updateProjectionMatrix();
}
addEventListener('resize', resize);
resize();
frame();
