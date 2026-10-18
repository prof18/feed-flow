// One queued response chunk; no native JavaScript bridge.
globalThis.__feedFlowCreateTransfer = function (config) {
  const controller = new AbortController();
  const origin = location.origin;
  let pending = null;
  let wake = null;
  let reader = null;
  let terminal = null;
  let metadata = null;
  let sequence = 0;
  let total = 0;
  const message = value => ({ nonce: config.nonce, ...value });
  const timer = setTimeout(() => fail('timeout'), config.timeoutMs);

  function fail(reason) {
    if (terminal) return;
    terminal = message({ kind: 'error', reason, status: metadata?.status, retryAfter: metadata?.retryAfter });
    pending = null;
    clearTimeout(timer);
    controller.abort();
    if (reader) reader.cancel().catch(() => {});
    if (wake) { wake(); wake = null; }
  }

  function isOwner(nonce) {
    if (nonce !== config.nonce) return false;
    if (location.origin !== origin) { fail('navigation'); return false; }
    return true;
  }

  async function produce() {
    try {
      const target = new URL(config.url);
      target.hash = '';
      if (target.protocol !== 'https:' || target.origin !== origin || target.username || target.password) {
        fail('origin'); return;
      }
      const response = await fetch(target.href, {
        credentials: 'same-origin', cache: 'no-store', redirect: 'error', signal: controller.signal,
      });
      if (terminal) return;
      metadata = { effectiveUrl: response.url, status: response.status,
        contentType: response.headers.get('content-type'), retryAfter: response.headers.get('retry-after') };
      if (response.url !== target.href) { fail('redirect'); return; }
      if (response.headers.get('cf-mitigated')?.trim() === 'challenge') { fail('challenge'); return; }
      if (!response.ok) { fail('http'); return; }
      if (!response.body) { fail('empty'); return; }
      reader = response.body.getReader();
      for (;;) {
        const result = await reader.read();
        if (terminal) return;
        if (result.done) break;
        if (total + result.value.length > config.maxBytes) { fail('too-large'); return; }
        for (let offset = 0; offset < result.value.length; offset += config.chunkBytes) {
          if (terminal) return;
          const bytes = result.value.subarray(offset, offset + config.chunkBytes);
          let binary = '';
          for (const byte of bytes) binary += String.fromCharCode(byte);
          total += bytes.length;
          pending = message({ kind: 'chunk', sequence: sequence++, base64: btoa(binary) });
          await new Promise(resolve => { wake = resolve; });
        }
      }
      if (!terminal) metadata = { ...metadata, total, complete: true };
    } catch (_) { fail('transport'); }
  }
  void produce();
  return {
    next(nonce) {
      if (!isOwner(nonce)) return message({ kind: 'error', reason: 'owner' });
      if (terminal) return terminal;
      if (pending) {
        const result = pending;
        pending = null;
        if (wake) { wake(); wake = null; }
        return result;
      }
      if (metadata?.complete) {
        clearTimeout(timer);
        terminal = message({ kind: 'done', ...metadata });
        return terminal;
      }
      return message({ kind: 'waiting' });
    },
    cancel(nonce) { if (isOwner(nonce)) fail('canceled'); },
  };
};
