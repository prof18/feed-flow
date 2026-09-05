#!/usr/bin/env node
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const sourcePath = process.argv[2] || path.resolve(
  'shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/ReaderModeHtmlAndCss.kt',
);
const source = fs.readFileSync(sourcePath, 'utf8');
// Run from the repository root: node .scripts/test-reader-instagram.cjs
const scriptMatch = source.match(/<script>\s*([\s\S]*?)\s*<\/script>/);
assert(scriptMatch, 'Could not extract reader script');

const listeners = {};
const frames = [];
const document = {
  addEventListener(type, listener) { listeners[type] = listener; },
  querySelectorAll(selector) {
    if (selector.includes('instagram.com')) return frames.filter((frame) => frame.instagram);
    if (selector.includes('platform.twitter.com')) return frames.filter((frame) => frame.twitter);
    return [];
  },
};
const window = {
  addEventListener(type, listener) {
    const key = `window:${type}`;
    listeners[key] = listeners[key] || [];
    listeners[key].push(listener);
  },
};
vm.runInNewContext(scriptMatch[1], { window, document, Math, Number, Array });
const onMessages = listeners['window:message'];
assert(Array.isArray(onMessages) && onMessages.length > 0);

function frame(kind) {
  return { [kind]: true, contentWindow: {}, style: {} };
}
function send(origin, data, sourceFrame) {
  for (const onMessage of onMessages) onMessage({ origin, data, source: sourceFrame.contentWindow });
}
function instagramMessage(height, type = 'MEASURE') {
  return JSON.stringify({ type, details: { height } });
}

const first = frame('instagram');
const second = frame('instagram');
frames.push(first, second);
send('https://www.instagram.com', instagramMessage(321.2), first);
assert.equal(first.style.height, '322px');
assert.deepEqual(second.style, {});
send('https://www.instagram.com', instagramMessage(418.01), first);
assert.equal(first.style.height, '419px');
send('https://www.instagram.com', instagramMessage(207), second);
assert.equal(second.style.height, '207px');

for (const event of [
  { origin: 'https://evil.example', data: instagramMessage(999), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(999), source: {} },
  { origin: 'https://www.instagram.com', data: '{bad json', source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: null, source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: '{}', source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: JSON.stringify({ type: 'MEASURE' }), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: JSON.stringify({ type: 'MEASURE', details: {} }), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: JSON.stringify({ type: 'MEASURE', details: { height: 'not-a-number' } }), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(NaN), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(0), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(Infinity), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(-1), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(10001), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(999, 'OTHER'), source: first.contentWindow },
  { origin: 'https://www.instagram.com', data: instagramMessage(999), source: {} },
]) {
  send(event.origin, event.data, { contentWindow: event.source });
}
assert.equal(first.style.height, '419px');

const tweet = frame('twitter');
frames.push(tweet);
send('https://platform.twitter.com', {
  'twttr.embed': { method: 'twttr.private.resize', params: [{ height: 155.1 }] },
}, tweet);
assert.equal(tweet.style.height, '156px');
send('https://www.instagram.com', instagramMessage(999), tweet);
send('https://platform.twitter.com', {
  'twttr.embed': { method: 'twttr.private.resize', params: [{ height: 999 }] },
}, first);
assert.equal(tweet.style.height, '156px');
assert.equal(first.style.height, '419px');

console.log('Instagram reader resize behavioral regression test passed');
