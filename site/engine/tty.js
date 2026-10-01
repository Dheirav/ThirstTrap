// tty.js — a terminal that types itself when scrolled into view.
//
//   typeSession(document.querySelector('.tty-body'), [
//     { cmd: 'pipx install observal-cli' },
//     { out: 'installed package observal-cli 1.4.0', delay: 400 },
//     { out: '<b class="ok">✓</b> logged in as maya', html: true },
//     { wait: 800 },
//     { cmd: 'observal pull platform/reviewer', then: () => highlight() },
//   ], { loop: true });
//
// Each step can carry `then`, called when that line finishes — use it to make
// the page react to the "agent's" commands (the self-editing section trick).
import { reduceMotion } from './scroll-stage.js';

const esc = (s) => s.replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[c]);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export function typeSession(body, steps, { prompt = '$', cps = 28, loop = false, loopGap = 2600, maxLines = 60, threshold = 0.35 } = {}) {
  let started = false;
  const line = (html, cls = '') => {
    const p = document.createElement('p');
    if (cls) p.className = cls;
    p.innerHTML = html;
    body.appendChild(p);
    while (body.children.length > maxLines) body.firstChild.remove();
    body.scrollTop = body.scrollHeight;
    return p;
  };

  async function run() {
    do {
      body.replaceChildren();
      for (const s of steps) {
        if (s.wait) { await sleep(reduceMotion ? 0 : s.wait); continue; }
        if (s.cmd != null) {
          const p = line(`<span class="prompt">${esc(prompt)}</span> <span class="typed"></span><i class="caret"></i>`, 'cmd');
          const t = p.querySelector('.typed');
          if (reduceMotion) t.textContent = s.cmd;
          else for (let i = 1; i <= s.cmd.length; i++) { t.textContent = s.cmd.slice(0, i); await sleep((1000 / cps) * (0.6 + Math.random() * 0.8)); }
          p.querySelector('.caret').remove();
          await sleep(reduceMotion ? 0 : s.pause ?? 250);
        } else if (s.out != null) {
          await sleep(reduceMotion ? 0 : s.delay ?? 120);
          line(s.html ? s.out : esc(s.out), s.cls ?? 'out');
        }
        s.then?.();
      }
      if (loop && !reduceMotion) await sleep(loopGap);
    } while (loop && !reduceMotion);
  }

  const io = new IntersectionObserver(([e]) => {
    if (e.isIntersecting && !started) { started = true; io.disconnect(); run(); }
  }, { threshold });
  io.observe(body.closest('.tty') || body);
}
