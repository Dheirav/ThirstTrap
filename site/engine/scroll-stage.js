// scroll-stage.js — the core idea: scroll position IS the timeline.
// Tall empty "tracks" produce a 0..1 progress; fixed layers read it.
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger.js';

gsap.registerPlugin(ScrollTrigger);
export { gsap, ScrollTrigger };

export const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;

export const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
/** 0 before a, 1 after b, linear between. The workhorse for crossfades. */
export const range = (p, a, b) => clamp((p - a) / (b - a));
export const smoothstep = (x) => x * x * (3 - 2 * x);

/** Lenis smooth scrolling wired into ScrollTrigger. Skipped for reduced motion. */
export async function smoothScroll({ lerp = 0.09, wheelMultiplier = 0.9 } = {}) {
  if (reduceMotion) return null;
  const { default: Lenis } = await import('lenis');
  const lenis = new Lenis({ lerp, wheelMultiplier });
  lenis.on('scroll', ScrollTrigger.update);
  gsap.ticker.add((t) => lenis.raf(t * 1000));
  gsap.ticker.lagSmoothing(0);
  return lenis;
}

/**
 * Report a track's progress (0 at its top reaching the viewport top,
 * 1 at its bottom reaching the viewport bottom).
 * Extra ScrollTrigger options (onLeave, onEnterBack…) pass straight through.
 */
export function track(el, onProgress, opts = {}) {
  return ScrollTrigger.create({
    trigger: el,
    start: 'top top',
    end: 'bottom bottom',
    onUpdate: (s) => onProgress(s.progress, s),
    ...opts,
  });
}

/**
 * Narrative captions pinned to moments of a track.
 *   <div class="beat" data-at="0.3">…</div>                 peaks at 30%
 *   <div class="beat" data-at="0.8" data-until="0.95">…</div> holds 80–95%
 *   data-win="0.12" widens the fade window for one beat.
 * Returns an updater: call it with the track's progress.
 */
export function beats(root, { win = 0.09, drift = 160 } = {}) {
  const els = [...root.querySelectorAll('[data-at]')];
  return (p) => {
    for (const el of els) {
      const at = +el.dataset.at;
      const until = el.dataset.until ? +el.dataset.until : at;
      const w = el.dataset.win ? +el.dataset.win : win;
      const d = p < at ? at - p : p > until ? p - until : 0;
      const o = clamp(1 - d / w);
      const off = p < at ? p - at : p > until ? p - until : 0;
      el.style.opacity = o.toFixed(3);
      el.style.visibility = o > 0.001 ? 'visible' : 'hidden';
      el.style.transform = `translateY(${(off * -drift).toFixed(1)}px)`;
    }
  };
}

/** Show a fixed layer by opacity; it only takes clicks once fully visible. */
export function layer(el, o) {
  el.style.opacity = o.toFixed(3);
  el.style.visibility = o > 0.001 ? 'visible' : 'hidden';
  el.style.pointerEvents = o > 0.99 ? 'auto' : 'none';
}

/** Copy-to-clipboard for any [data-copy] button with an optional .copy-state label. */
export function copyButtons(root = document) {
  root.querySelectorAll('[data-copy]').forEach((b) => {
    b.addEventListener('click', async () => {
      const s = b.querySelector('.copy-state');
      try {
        await navigator.clipboard.writeText(b.dataset.copy);
        if (s) s.textContent = 'copied';
      } catch {
        if (s) s.textContent = 'press ctrl+c';
      }
      setTimeout(() => s && (s.textContent = 'copy'), 1600);
    });
  });
}

/** Hide the loader and fade in whatever is marked ready. */
export function ready(loader, ...els) {
  loader?.classList.add('done');
  requestAnimationFrame(() => els.forEach((e) => e?.classList.add('ready')));
}
