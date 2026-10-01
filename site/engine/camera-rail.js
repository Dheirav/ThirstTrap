// camera-rail.js — a Three.js camera that flies a spline as you scroll.
//
//   const rail = await createRail(canvas, {
//     glb: '/scene/scene.glb',            // or build: (scene, THREE) => {...}
//     keyframes: [ { pos:[-8.8,4.4,-12.8], at:[2.6,2.9,-1.8], fov:40 }, ... ],
//     screen: 'laptop_screen',            // optional mesh name to texture
//     screenTexture: '/poster-dash.webp', // what the screen shows (match the UI you hand off to!)
//     fitLast: 'screen',                  // recompute the last keyframe so this mesh fills the view
//   });
//   track(heroTrack, (p) => rail.setProgress(p));
//
// Two centripetal Catmull-Rom splines (camera position and look-at target)
// run through the keyframes. Progress is smoothstepped PER SEGMENT, so the
// camera dwells at every keyframe and moves between them: each keyframe is a
// "shot" and scroll is the cut between shots.
//
// Authoring: open the page with ?rail-debug — orbit freely, press K to log and
// copy the current {pos, at, fov} as a keyframe line.
import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';

const v3 = (a) => (a instanceof THREE.Vector3 ? a.clone() : new THREE.Vector3(...a));
const sstep = (x) => x * x * (3 - 2 * x);

export async function createRail(canvas, {
  glb, build, keyframes, background = '#0E2A33', fog = null,
  screen = null, screenTexture = null, fitLast = null, dracoPath = 'https://cdn.jsdelivr.net/npm/three@0.169.0/examples/jsm/libs/draco/',
  maxDpr = 1.5, shadows = true, onFrame, debug = /[?&]rail-debug\b/.test(location.search),
} = {}) {
  const dpr = Math.min(devicePixelRatio || 1, maxDpr);
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: dpr < 1.5, powerPreference: 'high-performance' });
  renderer.setPixelRatio(dpr);
  renderer.setClearColor(background);
  renderer.shadowMap.enabled = shadows;
  renderer.shadowMap.type = THREE.PCFShadowMap;

  const scene = new THREE.Scene();
  if (fog) scene.fog = new THREE.Fog(background, fog[0], fog[1]);
  const camera = new THREE.PerspectiveCamera(keyframes[0].fov ?? 40, 1, 0.02, 200);

  if (glb) {
    const loader = new GLTFLoader().setDRACOLoader(new DRACOLoader().setDecoderPath(dracoPath));
    const g = await loader.loadAsync(glb);
    scene.add(g.scene);
  }
  await build?.(scene, THREE);

  // Texture the in-scene screen with the same image the HTML stage shows,
  // so the 3D → UI handoff is an invisible cut.
  if (screen && screenTexture) {
    const mesh = scene.getObjectByName(screen);
    if (mesh) {
      const tex = await new THREE.TextureLoader().loadAsync(screenTexture);
      tex.colorSpace = THREE.SRGBColorSpace;
      mesh.material = new THREE.MeshBasicMaterial({ map: tex, toneMapped: false });
      mesh.castShadow = false;
    }
  }

  const posCurve = new THREE.CatmullRomCurve3(keyframes.map((k) => v3(k.pos)), false, 'centripetal');
  const atCurve = new THREE.CatmullRomCurve3(keyframes.map((k) => v3(k.at)), false, 'centripetal');
  const N = keyframes.length - 1;
  const target = new THREE.Vector3();

  let progress = 0, dirty = true, running = true, lastTick = 0, projector = null, anchors = [];
  const clock = { t: 0 };

  function place() {
    const e = Math.min(Math.max(progress, 0), 1) * N;
    const i = Math.min(Math.floor(e), N - 1);
    const f = sstep(e - i);
    const u = (i + f) / N;
    camera.position.copy(posCurve.getPoint(u));
    atCurve.getPoint(u, target);
    camera.lookAt(target);
    const f0 = keyframes[i].fov ?? 40, f1 = keyframes[i + 1].fov ?? f0;
    camera.fov = f0 + (f1 - f0) * f;
    camera.updateProjectionMatrix();
  }

  // The handoff only works if the screen covers the viewport at the last
  // keyframe, and that distance depends on aspect ratio. So place the last
  // keyframe square to the mesh, at the distance where it just overfills.
  const fitMesh = fitLast && scene.getObjectByName(fitLast);
  function fitLastKeyframe() {
    if (!fitMesh) return;
    scene.updateMatrixWorld(true);
    const g = fitMesh.geometry;
    g.computeBoundingBox();
    const size = g.boundingBox.getSize(new THREE.Vector3()).multiply(fitMesh.getWorldScale(new THREE.Vector3()));
    const c = g.boundingBox.getCenter(new THREE.Vector3()).applyMatrix4(fitMesh.matrixWorld);
    const normal = new THREE.Vector3(0, 0, 1).transformDirection(fitMesh.matrixWorld);
    const t = Math.tan(THREE.MathUtils.degToRad((keyframes[N].fov ?? 40) / 2));
    const d = Math.min(size.x / (2 * t * camera.aspect), size.y / (2 * t)) * 0.94;
    posCurve.points[N].copy(c).addScaledVector(normal, d);
    atCurve.points[N].copy(c);
  }

  function resize() {
    const w = canvas.clientWidth, h = canvas.clientHeight;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
    fitLastKeyframe();
    dirty = true;
  }
  addEventListener('resize', resize);
  resize();

  let controls = null;
  if (debug) {
    const { OrbitControls } = await import('three/addons/controls/OrbitControls.js');
    place();
    controls = new OrbitControls(camera, canvas);
    controls.target.copy(target);
    controls.addEventListener('change', () => (dirty = true));
    canvas.style.pointerEvents = 'auto';
    canvas.style.zIndex = 999;
    const r = (v) => `[${v.toArray().map((n) => +n.toFixed(2)).join(', ')}]`;
    addEventListener('keydown', (e) => {
      if (e.key !== 'k' && e.key !== 'K') return;
      const line = `{ pos: ${r(camera.position)}, at: ${r(controls.target)}, fov: ${Math.round(camera.fov)} },`;
      console.log(line);
      navigator.clipboard?.writeText(line).catch(() => {});
    });
    console.info('[scroll-story] rail-debug: orbit with mouse, press K to capture a keyframe');
  }

  // Render only when something changed; tick idle animation at ~24fps while
  // moving and ~8fps while still. Pause entirely when covered by another layer.
  function loop(t) {
    if (!running) return;
    requestAnimationFrame(loop);
    const moving = progress > 0.001 && progress < 0.999;
    if (t - lastTick > (moving ? 41 : 120)) { clock.t = t / 1000; lastTick = t; dirty = !!onFrame || dirty; }
    if (!dirty) return;
    if (!controls) place(); else controls.update();
    onFrame?.(clock.t, progress);
    renderer.render(scene, camera);
    if (projector && anchors.length) {
      const w = canvas.clientWidth, h = canvas.clientHeight, p = new THREE.Vector3();
      projector(anchors.map((a) => { p.copy(a).project(camera); return { x: (p.x * 0.5 + 0.5) * w, y: (-p.y * 0.5 + 0.5) * h, visible: p.z < 1 }; }));
    }
    dirty = false;
  }
  requestAnimationFrame(loop);

  return {
    THREE, scene, camera, renderer,
    setProgress(p) { progress = p; dirty = true; },
    invalidate() { dirty = true; },
    pause() { running = false; },
    resume() { if (!running) { running = true; dirty = true; requestAnimationFrame(loop); } },
    /** Pin HTML labels to 3D points: cb receives [{x, y, visible}] every rendered frame. */
    project(points, cb) { anchors = points.map(v3); projector = cb; dirty = true; },
  };
}

export const hasWebGL2 = () => { try { return !!document.createElement('canvas').getContext('webgl2'); } catch { return false; } };
