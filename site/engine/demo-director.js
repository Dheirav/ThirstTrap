// demo-director.js — a scripted "fake user" driving a product mock.
// The mock is plain HTML in the same page: screens live in
// <template data-view="name"> and hook targets carry data-t="name".
//
//   const demo = createDemo(stageEl);
//   demo.chapter('search', (s) => s
//     .view('home').cursorAt(0.7, 0.8)
//     .move('q').click('q').type('q', 'find security bugs')
//     .view('results', { fade: false })
//     .appear('[data-res]', { stagger: 0.1 })
//     .note('score', 'ranked by what you asked', { side: 'left' })
//     .wait(1.2));
//   demo.bindScroll(document.querySelector('#demo-track'));
import { gsap, ScrollTrigger, reduceMotion } from './scroll-stage.js';

const CURSOR_SVG = '<svg viewBox="0 0 24 24"><path d="M4 2l16 9-7 1.6L9.6 20z"/></svg>';

export function createDemo(stage, { cursorName = '', screen = stage.querySelector('.demo-screen'), speed = 1.2 } = {}) {
  const annot = el('div', 'annot', stage);
  const cursor = el('div', 'cursor', stage);
  cursor.innerHTML = CURSOR_SVG + (cursorName ? `<span>${cursorName}</span>` : '');
  gsap.set(cursor, { opacity: 0 });

  let pos = { x: 0, y: 0 };
  let seed = 11;
  const rand = () => (seed = (seed * 16807) % 2147483647) / 2147483647;

  // ---- lookup -----------------------------------------------------------
  const find = (t) => {
    if (typeof t === 'function') return t();
    if (t instanceof Element) return t;
    // Several matches (e.g. responsive duplicates): take the visible one.
    const all = [...screen.querySelectorAll(`[data-t="${t}"]`)];
    return all.find((e) => { const r = e.getBoundingClientRect(); return r.width && r.height; }) ?? all[0] ?? screen.querySelector(t);
  };
  const center = (t, dx = 0, dy = 0) => {
    const r = find(t).getBoundingClientRect(), s = stage.getBoundingClientRect();
    return { x: r.left - s.left + r.width / 2 + dx, y: r.top - s.top + r.height / 2 + dy };
  };
  const scroller = (t) => {
    for (let n = find(t)?.parentElement; n && n !== screen.parentElement; n = n.parentElement) {
      const oy = getComputedStyle(n).overflowY;
      if ((oy === 'auto' || oy === 'scroll') && n.scrollHeight > n.clientHeight + 4) return n;
    }
    return null;
  };
  const place = (x, y) => {
    pos = { x, y };
    gsap.set(cursor, { x, y });
    cursor.classList.toggle('tag-left', x > stage.clientWidth - 90);
  };
  const clearNotes = () => annot.replaceChildren();
  const show = (name) => {
    const tpl = screen.parentElement.querySelector(`template[data-view="${name}"]`) || document.querySelector(`template[data-view="${name}"]`);
    if (!tpl) throw new Error(`scroll-story: no <template data-view="${name}">`);
    screen.replaceChildren(tpl.content.cloneNode(true));
  };

  // ---- chainable script builder ----------------------------------------
  function script(tl) {
    const s = {
      tl,
      /** Swap the screen to another template view. */
      view(name, { fade = true } = {}) {
        tl.call(() => { clearNotes(); show(name); });
        if (fade) tl.fromTo(screen, { opacity: 0.55 }, { opacity: 1, duration: 0.22, ease: 'power1.out' });
        return s;
      },
      /** Put the cursor at a fraction of the stage and fade it in. */
      cursorAt(fx, fy) {
        tl.call(() => place(stage.clientWidth * fx, stage.clientHeight * fy));
        tl.to(cursor, { opacity: 1, duration: 0.25 });
        return s;
      },
      /** Glide to a target on a gentle arc (straight lines look robotic). */
      move(t, { dx = 0, dy = 0, dur = 0.85 } = {}) {
        const k = { t: 0 };
        let a, b, c;
        tl.to(k, {
          t: 1, duration: dur, ease: 'power2.inOut',
          onStart() {
            a = { ...pos }; b = center(t, dx, dy);
            const mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
            c = { x: mx - (b.y - a.y) * 0.2, y: my + (b.x - a.x) * 0.2 };
          },
          onUpdate() {
            if (!a) return;
            const u = k.t, v = 1 - u;
            place(v * v * a.x + 2 * v * u * c.x + u * u * b.x, v * v * a.y + 2 * v * u * c.y + u * u * b.y);
          },
        });
        return s;
      },
      /** Toggle a data-hover attribute so CSS can show hover state. */
      hover(t, on = true) { tl.call(() => find(t)?.toggleAttribute('data-hover', on)); return s; },
      /** Press: cursor squish, ripple, data-pressed flash, optional side effect. */
      click(t, fn) {
        const svg = cursor.querySelector('svg');
        tl.to(svg, { scale: 0.82, duration: 0.07, transformOrigin: '20% 10%' });
        tl.call(() => {
          const r = el('i', 'ripple', stage);
          gsap.fromTo(r, { x: pos.x, y: pos.y, scale: 0.3, opacity: 1 }, { scale: 1.3, opacity: 0, duration: 0.45, onComplete: () => r.remove() });
          const e = t && find(t);
          if (e) { e.setAttribute('data-pressed', ''); setTimeout(() => e.removeAttribute('data-pressed'), 140); }
          fn?.(e);
        });
        tl.to(svg, { scale: 1, duration: 0.12 });
        return s;
      },
      /** Type into an input or element, human-ish rhythm. onChar lets other fields react live. */
      type(t, text, { cps = 16, onChar } = {}) {
        const set = (v) => {
          const e = find(t);
          if (e.tagName === 'INPUT' || e.tagName === 'TEXTAREA') { e.value = v; e.setAttribute('value', v); } else e.textContent = v;
          onChar?.(v);
        };
        tl.call(() => set(''));
        for (let i = 1; i <= text.length; i++) {
          const gap = (1 / cps) * (0.55 + rand() * 0.9) + (text[i - 2] === ' ' ? 0.03 : 0);
          tl.call(() => set(text.slice(0, i)), null, `+=${gap.toFixed(3)}`);
        }
        return s;
      },
      /** Stagger elements in (rows arriving, cards loading). */
      appear(sel, { from = { opacity: 0, y: 12 }, stagger = 0.06, dur = 0.4, hold = 0.6 } = {}) {
        tl.call(() => {
          const els = typeof sel === 'function' ? sel() : screen.querySelectorAll(sel);
          if (els.length) gsap.fromTo(els, from, { opacity: 1, y: 0, duration: dur, stagger, ease: 'power2.out' });
        });
        tl.to({}, { duration: hold });
        return s;
      },
      /** Scroll the target's scroll container so the target sits `block` from its top. */
      scrollTo(t, { block = 0.12, dur = 1.1 } = {}) {
        const k = { v: 0 };
        let box, from, to;
        tl.to(k, {
          v: 1, duration: dur, ease: 'power2.inOut',
          onStart() {
            box = scroller(t); if (!box) return;
            from = box.scrollTop;
            const r = find(t).getBoundingClientRect(), b = box.getBoundingClientRect();
            to = Math.max(0, Math.min(box.scrollHeight - box.clientHeight, from + r.top - b.top - box.clientHeight * block));
          },
          onUpdate() { if (box) box.scrollTop = from + (to - from) * k.v; },
        });
        return s;
      },
      /** Hand-drawn arrow + handwritten label pointing at a target. */
      note(t, text, { side = 'left', dx = 0, dy = 0 } = {}) {
        tl.call(() => drawNote(t, text, side, dx, dy));
        return s;
      },
      clearNotes() { tl.call(clearNotes); return s; },
      /** Arbitrary side effect at this point in the script. */
      call(fn) { tl.call(fn); return s; },
      wait(sec) { tl.to({}, { duration: sec }); return s; },
    };
    return s;
  }

  function drawNote(t, text, side, dx, dy) {
    const target = find(t);
    if (!target) return;
    const r = target.getBoundingClientRect(), st = stage.getBoundingClientRect();
    const W = stage.clientWidth, H = stage.clientHeight;
    const L = r.left - st.left, T = r.top - st.top;
    // Arrow tip sits just outside the target on the chosen side.
    const tx = (side === 'left' ? L - 8 : side === 'right' ? L + r.width + 8 : L + r.width / 2) + dx;
    const ty = (side === 'top' ? T - 8 : side === 'bottom' ? T + r.height + 8 : T + r.height / 2) + dy;
    let ox = side === 'left' ? -120 : side === 'right' ? 120 : tx > W / 2 ? -90 : 90;
    if (tx + ox < 40 || tx + ox > W - 40) ox = -ox;
    let oy = side === 'top' ? -70 : side === 'bottom' ? 70 : -48;
    if (ty + oy < 50) oy = Math.abs(oy);
    if (ty + oy > H - 40) oy = -Math.abs(oy);
    const sx = tx + ox, sy = ty + oy;
    const cx = (sx + tx) / 2 + oy * 0.35, cy = (sy + ty) / 2 - ox * 0.12;
    const ang = Math.atan2(ty - cy, tx - cx);
    const h1 = [tx - 12 * Math.cos(ang - 0.5), ty - 12 * Math.sin(ang - 0.5)];
    const h2 = [tx - 12 * Math.cos(ang + 0.5), ty - 12 * Math.sin(ang + 0.5)];
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', `M${sx},${sy} Q${cx},${cy} ${tx},${ty} M${h1} L${tx},${ty} L${h2}`);
    svg.appendChild(path);
    annot.appendChild(svg);
    const p = el('p', '', annot);
    p.textContent = text;
    const w = p.offsetWidth;
    p.style.left = `${Math.max(8, Math.min(W - w - 8, sx - (ox < 0 ? w : 0)))}px`;
    p.style.top = `${Math.max(8, sy - (oy < 0 ? 34 : -6))}px`;
    const len = path.getTotalLength();
    gsap.fromTo(path, { strokeDasharray: len, strokeDashoffset: len }, { strokeDashoffset: 0, duration: 0.7, ease: 'power1.inOut' });
    gsap.fromTo(p, { opacity: 0, y: 6 }, { opacity: 1, y: 0, duration: 0.4, delay: 0.25 });
  }

  // ---- chapters -----------------------------------------------------------
  const chapters = [];
  const built = {};
  let current = null;

  function mark(name) {
    const i = chapters.findIndex((c) => c.name === name);
    stage.querySelectorAll('[data-note]').forEach((n) => n.classList.toggle('on', n.dataset.note === name));
    stage.querySelectorAll('[data-dot]').forEach((d) => d.classList.toggle('on', chapters.findIndex((c) => c.name === d.dataset.dot) <= i));
  }

  /** Play a chapter from the start, or jump to its end (scrolling backwards / reduced motion). */
  function play(name, { toEnd = false } = {}) {
    if (current === name) return;
    current = name;
    mark(name);
    Object.values(built).forEach((t) => t.pause());
    gsap.set(cursor, { opacity: 0 });
    clearNotes();
    if (!built[name]) {
      const tl = gsap.timeline({ paused: true });
      chapters.find((c) => c.name === name).build(script(tl));
      built[name] = tl.timeScale(speed);
    }
    toEnd || reduceMotion ? built[name].progress(1) : built[name].restart();
  }

  return {
    cursor,
    /** Register a chapter. Its script should begin with a view() so it can start cold. */
    chapter(name, build) { chapters.push({ name, build }); return this; },
    play,
    show,
    /** Split a track evenly across chapters; entering a slice plays it. */
    bindScroll(trackEl, { bar = stage.querySelector('.note-bar i') } = {}) {
      const slice = () => trackEl.offsetHeight / chapters.length;
      if (chapters[0]) mark(chapters[0].name);
      chapters.forEach((c, i) => ScrollTrigger.create({
        trigger: trackEl,
        start: () => `top+=${i * slice()} bottom`,
        end: () => `top+=${(i + 1) * slice()} bottom`,
        onEnter: () => play(c.name),
        onEnterBack: () => play(c.name, { toEnd: true }),
        onLeaveBack: () => { if (i === 0) current = null; },
        onUpdate: (st) => bar && gsap.set(bar, { scaleX: st.progress }),
      }));
      return this;
    },
  };
}

function el(tag, cls, parent) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  parent.appendChild(e);
  return e;
}
