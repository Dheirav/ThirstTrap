// parallax.js — the 2D "camera". Keyframe any element's transform against
// track progress, including a "fit" stop that zooms a target to fill the
// viewport (the fly-into-the-screen handoff, no WebGL needed).
//
//   <div class="cam" data-kf='{"0":{"scale":1},"0.6":{"scale":1.4,"y":"-8vh"},"1":{"fit":"#screen"}}'>
//
// Props: x, y (px number, or "10vw" / "5vh" strings), scale, rotate (deg),
// opacity, blur (px). Omitted props carry over from the previous stop.
// data-kf-portrait='{…}' replaces data-kf when the viewport is taller than
// wide: pan across the wide art to frame what matters on phones.
// Between stops the motion is smoothstepped, so it dwells at each keyframe
// and moves in between — that's what makes it read as a directed shot.
import { smoothstep } from './scroll-stage.js';

const PROPS = ['x', 'y', 'scale', 'rotate', 'opacity', 'blur'];
const DEFAULTS = { x: 0, y: 0, scale: 1, rotate: 0, opacity: 1, blur: 0 };

function toPx(v) {
  if (typeof v === 'number') return v;
  const n = parseFloat(v);
  if (v.endsWith('vw')) return (n * innerWidth) / 100;
  if (v.endsWith('vh')) return (n * innerHeight) / 100;
  return n;
}

/** Transform that makes `target` (a descendant of `el`) cover the viewport. */
function fitStop(el, target) {
  const prev = el.style.transform;
  el.style.transform = 'none';
  const c = el.getBoundingClientRect();
  const r = target.getBoundingClientRect();
  el.style.transform = prev;
  const s = Math.max(innerWidth / r.width, innerHeight / r.height);
  return {
    x: (innerWidth - r.width * s) / 2 - c.left - (r.left - c.left) * s,
    y: (innerHeight - r.height * s) / 2 - c.top - (r.top - c.top) * s,
    scale: s,
  };
}

function resolve(el, raw) {
  const stops = Object.entries(raw)
    .map(([k, v]) => [parseFloat(k), v])
    .sort((a, b) => a[0] - b[0]);
  let carry = { ...DEFAULTS };
  return stops.map(([at, v]) => {
    const fit = v.fit ? fitStop(el, el.querySelector(v.fit) || document.querySelector(v.fit)) : {};
    const out = { ...carry };
    for (const p of PROPS) if (p in v) out[p] = toPx(v[p]);
    Object.assign(out, fit);
    carry = out;
    return { at, v: out };
  });
}

// (cx, cy) is the element's own centre: the "lens" of the camera.
function sample(stops, p, cx, cy) {
  if (p <= stops[0].at) return stops[0].v;
  const last = stops[stops.length - 1];
  if (p >= last.at) return last.v;
  let i = 0;
  while (stops[i + 1].at < p) i++;
  const a = stops[i].v, b = stops[i + 1].v;
  const t = smoothstep((p - stops[i].at) / (stops[i + 1].at - stops[i].at));
  const o = {};
  for (const k of PROPS) o[k] = a[k] + (b[k] - a[k]) * t;
  // Scale interpolates in log space so zooms feel constant-speed, and we
  // interpolate the content point under the lens rather than the raw
  // translate; otherwise a zoom target slides out of frame mid-zoom.
  o.scale = Math.exp(Math.log(a.scale) + (Math.log(b.scale) - Math.log(a.scale)) * t);
  const fa = { x: (cx - a.x) / a.scale, y: (cy - a.y) / a.scale };
  const fb = { x: (cx - b.x) / b.scale, y: (cy - b.y) / b.scale };
  o.x = cx - (fa.x + (fb.x - fa.x) * t) * o.scale;
  o.y = cy - (fa.y + (fb.y - fa.y) * t) * o.scale;
  return o;
}

function apply(el, s) {
  el.style.transform = `translate(${s.x.toFixed(1)}px, ${s.y.toFixed(1)}px) scale(${s.scale.toFixed(4)}) rotate(${s.rotate.toFixed(2)}deg)`;
  el.style.opacity = s.opacity.toFixed(3);
  el.style.filter = s.blur > 0.05 ? `blur(${s.blur.toFixed(1)}px)` : '';
}

/**
 * Drive every [data-kf] element under `root`. Returns an updater(progress).
 * Layers with data-depth="0.3" etc. also get simple parallax drift
 * (depth 0 = pinned to the sky, 1 = moves with the camera's y).
 */
export function keyframed(root) {
  const items = [...root.querySelectorAll('[data-kf]')].map((el) => {
    el.style.transformOrigin = '0 0';
    return { el, stops: null };
  });
  const depth = [...root.querySelectorAll('[data-depth]')];
  let last = 0;
  const build = () => items.forEach((it) => {
    const src = innerHeight > innerWidth && it.el.dataset.kfPortrait ? it.el.dataset.kfPortrait : it.el.dataset.kf;
    it.stops = resolve(it.el, JSON.parse(src));
  });
  const update = (p) => {
    last = p;
    for (const it of items) apply(it.el, sample(it.stops, p, (it.el.offsetWidth || 0) / 2, (it.el.offsetHeight || 0) / 2));
    for (const el of depth) el.style.transform = `translateY(${(p * -100 * +el.dataset.depth).toFixed(2)}vh)`;
  };
  build();
  addEventListener('resize', () => { build(); update(last); });
  update(0);
  return update;
}
