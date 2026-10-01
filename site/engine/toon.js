// toon.js — the illustrated look: light quantised into 3 flat bands.
// A tiny palette (dark / mid / lit + one warm accent) is what makes a
// low-poly scene read as a drawing rather than a cheap render.
import * as THREE from 'three';

/**
 * toonMaterial({ lit, mid, dark, t1, t2, warm })
 * Bands by lighting luminance: < t1 → dark, < t2 → mid, else lit.
 * `warm`: lights with a stronger red than blue channel tint toward this colour
 * (use one warm light — a lamp, a screen — against cool moonlight).
 */
export function toonMaterial({ lit = '#F4ECDC', mid = '#2F7F8C', dark = '#0E2A33', warm = '#F2B544', t1 = 0.1, t2 = 0.55, side = THREE.FrontSide } = {}) {
  const m = new THREE.MeshLambertMaterial({ side });
  const u = {
    uLit: { value: new THREE.Color(lit) }, uMid: { value: new THREE.Color(mid) },
    uDark: { value: new THREE.Color(dark) }, uWarm: { value: new THREE.Color(warm) },
    uT1: { value: t1 }, uT2: { value: t2 },
  };
  m.onBeforeCompile = (sh) => {
    Object.assign(sh.uniforms, u);
    sh.fragmentShader = sh.fragmentShader
      .replace('#include <common>', '#include <common>\nuniform vec3 uLit, uMid, uDark, uWarm; uniform float uT1, uT2;')
      .replace('#include <opaque_fragment>', `
        float lum = dot(outgoingLight, vec3(0.2126, 0.7152, 0.0722));
        vec3 toon = lum < uT1 ? uDark : (lum < uT2 ? uMid : uLit);
        float warmth = clamp((outgoingLight.r - outgoingLight.b) * 4.0, 0.0, 1.0);
        toon = mix(toon, uWarm, step(0.35, warmth) * 0.55);
        gl_FragColor = vec4(toon, 1.0);`);
  };
  m.customProgramCacheKey = () => `toon-${lit}-${mid}-${dark}-${t1}-${t2}`;
  m.userData.uniforms = u;
  return m;
}

/** Replace every mesh material in a loaded GLB with toon materials, reusing one per source colour. */
export function toonify(root, palette = {}) {
  const cache = new Map();
  root.traverse((o) => {
    if (!o.isMesh || o.userData.keepMaterial) return;
    const key = o.material?.color?.getHexString?.() ?? 'x';
    if (!cache.has(key)) cache.set(key, toonMaterial(palette));
    o.material = cache.get(key);
    o.castShadow = o.receiveShadow = true;
  });
}

/**
 * Pixel-art twinkling stars as one Points draw call.
 * Returns the Points; advance twinkle with stars.material.uniforms.uTime.
 */
export function pixelStars({ count = 400, radius = 40, center = new THREE.Vector3(0, 2, 0), px = 3, color = '#F4ECDC', accent = '#F2B544', seed = 7 } = {}) {
  let s = seed;
  const rnd = () => (s = (s * 16807) % 2147483647) / 2147483647;
  const pos = [], seeds = [], tint = [];
  for (let i = 0; i < count; i++) {
    const a = rnd() * Math.PI * 2, b = Math.acos(1 - rnd() * 0.95); // upper hemisphere bias
    pos.push(center.x + Math.sin(b) * Math.cos(a) * radius, center.y + Math.cos(b) * radius, center.z + Math.sin(b) * Math.sin(a) * radius);
    seeds.push(rnd() * 100, 1 + Math.floor(rnd() * 3));
    tint.push(rnd() < 0.2 ? 1 : 0);
  }
  const g = new THREE.BufferGeometry();
  g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
  g.setAttribute('aSeed', new THREE.Float32BufferAttribute(seeds, 2));
  g.setAttribute('aTint', new THREE.Float32BufferAttribute(tint, 1));
  const mat = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false,
    uniforms: { uTime: { value: 0 }, uPx: { value: px }, uColor: { value: new THREE.Color(color) }, uAccent: { value: new THREE.Color(accent) } },
    vertexShader: `
      attribute vec2 aSeed; attribute float aTint;
      uniform float uTime, uPx; varying float vTint, vArm;
      void main() {
        vTint = aTint;
        vArm = min(floor(mod(uTime * 0.6 + aSeed.x, 4.0)), aSeed.y); // arm length steps 0..2
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        gl_PointSize = uPx * 5.0;
      }`,
    fragmentShader: `
      uniform vec3 uColor, uAccent; varying float vTint, vArm;
      void main() {
        vec2 c = floor(gl_PointCoord * 5.0) - 2.0;       // 5x5 pixel grid
        float centre = step(abs(c.x) + abs(c.y), 0.5);
        float arm = step(min(abs(c.x), abs(c.y)), 0.5) * step(max(abs(c.x), abs(c.y)), vArm);
        if (max(centre, arm) < 0.5) discard;
        gl_FragColor = vec4(mix(uColor, uAccent, vTint), 1.0);
        #include <colorspace_fragment>
      }`,
  });
  return new THREE.Points(g, mat);
}
