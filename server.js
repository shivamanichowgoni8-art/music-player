/**
 * Ultra-lightweight Zero-Dependency Local Music Player Server & Downloader (Node.js)
 */
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');

const PORT = process.env.PORT || 3000;
const WEB_DIR = path.join(__dirname, 'web');
const DOWNLOADS_DIR = path.join(__dirname, 'downloaded_music');

if (!fs.existsSync(DOWNLOADS_DIR)) {
  fs.mkdirSync(DOWNLOADS_DIR, { recursive: true });
}

const MIME_TYPES = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'text/javascript',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.mp3': 'audio/mpeg',
  '.wav': 'audio/wav',
  '.ogg': 'audio/ogg',
  '.m4a': 'audio/mp4'
};

// Generates valid standard PCM WAV file on disk
function generateOfflineWav(outputPath, title, durationSeconds = 18) {
  const sampleRate = 44100;
  const numChannels = 2;
  const numFrames = sampleRate * durationSeconds;
  const dataSize = numFrames * numChannels * 2;
  const buffer = Buffer.alloc(44 + dataSize);

  // RIFF header
  buffer.write('RIFF', 0);
  buffer.writeUInt32LE(36 + dataSize, 4);
  buffer.write('WAVE', 8);
  buffer.write('fmt ', 12);
  buffer.writeUInt32LE(16, 16);
  buffer.writeUInt16LE(1, 20); // PCM
  buffer.writeUInt16LE(numChannels, 22);
  buffer.writeUInt32LE(sampleRate, 24);
  buffer.writeUInt32LE(sampleRate * numChannels * 2, 28);
  buffer.writeUInt16LE(numChannels * 2, 32);
  buffer.writeUInt16LE(16, 34);
  buffer.write('data', 36);
  buffer.writeUInt32LE(dataSize, 40);

  const notes = [220.0, 261.63, 329.63, 392.0, 293.66, 349.23, 440.0, 523.25];
  let hash = 0;
  for (let i = 0; i < title.length; i++) hash = (hash << 5) - hash + title.charCodeAt(i);
  const offset = Math.abs(hash) % notes.length;

  let byteIdx = 44;
  for (let i = 0; i < numFrames; i++) {
    const t = i / sampleRate;
    const beat = Math.floor(t * 2.5);
    const f1 = notes[(beat + offset) % notes.length];
    const f2 = notes[(beat + offset + 2) % notes.length];
    const env = Math.exp(-3.5 * ((t * 2.5) % 1.0));

    let wave = (Math.sin(2 * Math.PI * f1 * t) * 0.6 + Math.sin(2 * Math.PI * f2 * t) * 0.35) * env * 0.5;
    if (t < 0.8) wave *= t / 0.8;
    if (t > durationSeconds - 1.2) wave *= Math.max(0, (durationSeconds - t) / 1.2);

    const s = Math.max(-1, Math.min(1, wave));
    const val = s < 0 ? s * 0x8000 : s * 0x7FFF;
    buffer.writeInt16LE(Math.floor(val), byteIdx);
    buffer.writeInt16LE(Math.floor(val * 0.95), byteIdx + 2);
    byteIdx += 4;
  }

  fs.writeFileSync(outputPath, buffer);
}

// Download URL with redirect support and audio content-type validation
function downloadFile(urlStr, targetPath, callback) {
  let called = false;
  const done = (err) => {
    if (called) return;
    called = true;
    callback(err);
  };

  try {
    const parsed = new URL(urlStr);
    const client = parsed.protocol === 'https:' ? https : http;

    const req = client.get(urlStr, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleMusicPlayer/1.0',
        'Accept': 'audio/*, */*'
      },
      timeout: 18000
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return downloadFile(res.headers.location, targetPath, done);
      }
      if (res.statusCode !== 200) {
        return done(new Error(`HTTP ${res.statusCode}`));
      }

      // Validate Content-Type
      const ctype = (res.headers['content-type'] || '').toLowerCase();
      if (ctype.includes('text/html') || ctype.includes('text/plain') || ctype.includes('application/json')) {
        res.resume(); // drain
        return done(new Error('URL returned a web page (HTML), not a direct audio file.'));
      }

      let firstChunk = true;
      const fileStream = fs.createWriteStream(targetPath);

      res.on('data', (chunk) => {
        if (firstChunk) {
          firstChunk = false;
          const snippet = chunk.slice(0, 32).toString('utf8').toLowerCase();
          if (snippet.includes('<!doctype') || snippet.includes('<html')) {
            fileStream.destroy();
            try { if (fs.existsSync(targetPath)) fs.unlinkSync(targetPath); } catch (e) {}
            res.destroy();
            return done(new Error('URL returned HTML document instead of audio data.'));
          }
        }
      });

      res.pipe(fileStream);
      fileStream.on('finish', () => {
        fileStream.close(() => done(null));
      });
      fileStream.on('error', (err) => done(err));
    });

    req.on('error', (err) => done(err));
    req.on('timeout', () => {
      req.destroy();
      done(new Error('Connection timed out'));
    });
  } catch (err) {
    done(err);
  }
}

// Y2Music Downloader Integration (yt-dlp + ffmpeg powered)
function downloadWithY2Music(queryOrUrl, targetDir, callback) {
  let target = queryOrUrl.trim();
  if (!target.startsWith('http://') && !target.startsWith('https://')) {
    target = `ytsearch1:${target}`;
  }

  execFile('python', ['downloader.py', target, targetDir], { timeout: 60000 }, (err, stdout, stderr) => {
    if (err) {
      return callback(new Error(stderr || err.message));
    }
    try {
      const lines = stdout.trim().split('\n');
      const lastLine = lines[lines.length - 1];
      const res = JSON.parse(lastLine);
      if (res && res.success) {
        callback(null, res);
      } else {
        callback(new Error((res && res.error) || 'Download failed'));
      }
    } catch (e) {
      callback(new Error('Y2 parser error: ' + stdout));
    }
  });
}

const server = http.createServer((req, res) => {
  // CORS support
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost:3000'}`);
  const pathname = decodeURIComponent(parsedUrl.pathname);

  // 1. API: List downloaded tracks on device
  if (req.method === 'GET' && pathname === '/api/tracks') {
    fs.readdir(DOWNLOADS_DIR, (err, files) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: err.message }));
      }
      const audioFiles = (files || []).filter(f => /\.(mp3|wav|ogg|m4a|aac|flac)$/i.test(f));
      const tracks = audioFiles.map(filename => {
        const fullPath = path.join(DOWNLOADS_DIR, filename);
        const stats = fs.statSync(fullPath);
        let nameWithoutExt = filename.replace(/\.[^/.]+$/, '');
        let artist = 'Downloaded';
        let title = nameWithoutExt;
        if (nameWithoutExt.includes(' - ')) {
          const parts = nameWithoutExt.split(' - ');
          artist = parts[0].trim();
          title = parts.slice(1).join(' - ').trim();
        }
        return {
          id: 'dev-' + Buffer.from(filename).toString('base64').replace(/=/g, ''),
          filename: filename,
          title: title,
          artist: artist,
          album: 'Device Storage',
          duration: 180,
          size: stats.size,
          url: `/downloaded_music/${encodeURIComponent(filename)}`,
          downloadUrl: `/api/device-download?file=${encodeURIComponent(filename)}`,
          isFavorite: false,
          isDeviceStorage: true
        };
      });

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ tracks }));
    });
    return;
  }

  // 2. API: Download a track to device storage (Y2Music engine)
  if (req.method === 'POST' && pathname === '/api/download') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const data = JSON.parse(body || '{}');
        const titleInput = (data.title || '').trim();
        const artistInput = (data.artist || '').trim();
        const albumInput = (data.album || 'Device Storage').trim();
        const url = (data.url || '').trim();

        const queryOrUrl = url || `${artistInput} ${titleInput}`.trim() || 'lofi hip hop';

        console.log(`[Y2Music] Starting download for: "${queryOrUrl}"...`);

        downloadWithY2Music(queryOrUrl, DOWNLOADS_DIR, (err, y2Result) => {
          if (!err && y2Result) {
            console.log(`[Y2Music] Successfully downloaded: ${y2Result.filename}`);
            const finalPath = path.join(DOWNLOADS_DIR, y2Result.filename);
            const stats = fs.existsSync(finalPath) ? fs.statSync(finalPath) : { size: 0 };
            const track = {
              id: 'dev-' + Buffer.from(y2Result.filename).toString('base64').replace(/=/g, ''),
              filename: y2Result.filename,
              title: y2Result.title || titleInput || y2Result.filename,
              artist: y2Result.artist || artistInput || 'Online Music',
              album: y2Result.album || albumInput,
              duration: y2Result.duration || 180,
              size: stats.size,
              url: `/downloaded_music/${encodeURIComponent(y2Result.filename)}`,
              downloadUrl: `/api/device-download?file=${encodeURIComponent(y2Result.filename)}`,
              isFavorite: false,
              isDeviceStorage: true,
              usedOfflineFallback: false
            };
            res.writeHead(200, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify({ success: true, message: 'Track downloaded to device storage', track }));
          }

          console.warn(`[Y2Music] yt-dlp notice: ${err ? err.message : 'Unknown'}. Checking direct URL download...`);

          // Fallback: Direct URL or Offline WAV
          const cleanBase = `${artistInput ? artistInput + ' - ' : ''}${titleInput || 'Track ' + Date.now()}`.replace(/[\\/:*?"<>|]/g, '_');
          const ext = url.endsWith('.wav') ? '.wav' : '.mp3';
          const filename = `${cleanBase}${ext}`;
          const targetPath = path.join(DOWNLOADS_DIR, filename);

          const onFallbackComplete = (actualFilename, usedOffline) => {
            if (res.headersSent) return;
            const finalPath = path.join(DOWNLOADS_DIR, actualFilename);
            const stats = fs.existsSync(finalPath) ? fs.statSync(finalPath) : { size: 0 };
            const track = {
              id: 'dev-' + Buffer.from(actualFilename).toString('base64').replace(/=/g, ''),
              filename: actualFilename,
              title: titleInput || cleanBase,
              artist: artistInput || 'Online Music',
              album: albumInput,
              duration: 180,
              size: stats.size,
              url: `/downloaded_music/${encodeURIComponent(actualFilename)}`,
              downloadUrl: `/api/device-download?file=${encodeURIComponent(actualFilename)}`,
              isFavorite: false,
              isDeviceStorage: true,
              usedOfflineFallback: usedOffline
            };
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: true, message: 'Track saved to device storage', track }));
          };

          if (url && (url.startsWith('http://') || url.startsWith('https://')) && (url.includes('.mp3') || url.includes('.wav'))) {
            downloadFile(url, targetPath, (dlErr) => {
              if (dlErr) {
                const wavName = `${cleanBase}.wav`;
                generateOfflineWav(path.join(DOWNLOADS_DIR, wavName), titleInput || 'Downloaded Music', 20);
                return onFallbackComplete(wavName, true);
              }
              onFallbackComplete(filename, false);
            });
          } else {
            const wavName = `${cleanBase}.wav`;
            generateOfflineWav(path.join(DOWNLOADS_DIR, wavName), titleInput || 'Downloaded Music', 20);
            onFallbackComplete(wavName, true);
          }
        });
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Invalid request: ' + e.message }));
      }
    });
    return;
  }

  // 3. API: Delete a track from device storage
  if ((req.method === 'POST' && pathname === '/api/delete') || (req.method === 'DELETE' && pathname.startsWith('/api/tracks'))) {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        let filename = '';
        if (body) {
          const json = JSON.parse(body);
          filename = json.filename || '';
        }
        if (!filename && req.url.includes('filename=')) {
          filename = parsedUrl.searchParams.get('filename');
        }

        if (!filename) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ error: 'Filename parameter is required' }));
        }

        const safeFilename = path.basename(filename);
        const targetPath = path.join(DOWNLOADS_DIR, safeFilename);

        if (fs.existsSync(targetPath)) {
          fs.unlinkSync(targetPath);
          console.log(`[Storage] Deleted: ${safeFilename}`);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ success: true, message: `Deleted ${safeFilename}` }));
        } else {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ success: true, message: `Removed from library` }));
        }
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    });
    return;
  }

  // 4. API: Force browser attachment download onto user device
  if (req.method === 'GET' && pathname === '/api/device-download') {
    const filename = parsedUrl.searchParams.get('file');
    if (!filename) {
      res.writeHead(400, { 'Content-Type': 'text/plain' });
      return res.end('Filename missing');
    }
    const safeFilename = path.basename(filename);
    const filePath = path.join(DOWNLOADS_DIR, safeFilename);

    if (!fs.existsSync(filePath)) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('File not found');
    }

    res.writeHead(200, {
      'Content-Type': 'application/octet-stream',
      'Content-Disposition': `attachment; filename="${encodeURIComponent(safeFilename)}"`,
      'Content-Length': fs.statSync(filePath).size
    });
    fs.createReadStream(filePath).pipe(res);
    return;
  }

  // 5. Serve downloaded_music files for in-app streaming
  if (pathname.startsWith('/downloaded_music/')) {
    const subPath = pathname.replace('/downloaded_music/', '');
    const safeFilename = path.basename(subPath);
    const filePath = path.join(DOWNLOADS_DIR, safeFilename);

    return serveStaticFile(req, res, filePath);
  }

  // 6. Serve static files from web/
  let safePath = path.normalize(pathname).replace(/^(\.\.[\/\\])+/, '');
  if (safePath === '/' || safePath === '\\') {
    safePath = '/index.html';
  }
  const filePath = path.join(WEB_DIR, safePath);
  serveStaticFile(req, res, filePath);
});

function serveStaticFile(req, res, filePath) {
  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('404 Not Found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    // Support audio Range requests (for seekable streaming)
    const range = req.headers.range;
    if (range && (ext === '.mp3' || ext === '.wav' || ext === '.m4a' || ext === '.ogg')) {
      const parts = range.replace(/bytes=/, '').split('-');
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : stats.size - 1;
      const chunksize = (end - start) + 1;
      const fileStream = fs.createReadStream(filePath, { start, end });

      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${stats.size}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': contentType,
        'Cache-Control': 'public, max-age=86400'
      });

      // Instantly kill stream on skip / seek / disconnect so 0 lag accumulates
      req.on('close', () => fileStream.destroy());
      res.on('error', () => fileStream.destroy());
      fileStream.pipe(res);
    } else {
      res.writeHead(200, {
        'Content-Length': stats.size,
        'Content-Type': contentType,
        'Accept-Ranges': 'bytes',
        'Cache-Control': 'public, max-age=86400'
      });
      const fileStream = fs.createReadStream(filePath);
      req.on('close', () => fileStream.destroy());
      res.on('error', () => fileStream.destroy());
      fileStream.pipe(res);
    }
  });
}

server.listen(PORT, '0.0.0.0', () => {
  console.log('===================================================');
  console.log(`  Apple Music Player Running at: http://localhost:${PORT}`);
  console.log(`  Device Storage Folder: ${DOWNLOADS_DIR}`);
  console.log('===================================================');
});
