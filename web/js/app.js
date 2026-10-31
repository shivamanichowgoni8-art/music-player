/**
 * Apple Music Desktop Player - Core Logic, Audio Engine, Downloader & Library Manager
 */

(function () {
  'use strict';

  // State Management
  const state = {
    library: [],
    queue: [],
    queueIndex: -1,
    history: [],
    isPlaying: false,
    isShuffle: false,
    repeatMode: 'off', // 'off', 'all', 'one'
    volume: 0.85,
    previousVolume: 0.85,
    currentView: 'songs',
    isGridView: false,
    searchQuery: '',
    currentTrack: null
  };

  // DOM Elements
  const audio = document.getElementById('audio-engine');
  const trackTableBody = document.getElementById('track-table-body');
  const trackGrid = document.getElementById('track-grid');
  const listViewContainer = document.getElementById('list-view-container');
  const gridViewContainer = document.getElementById('grid-view-container');
  const pageTitle = document.getElementById('page-title');
  const pageStats = document.getElementById('page-stats');
  const libraryCountBadge = document.getElementById('library-count');
  const favoritesCountBadge = document.getElementById('favorites-count');
  const queueBadge = document.getElementById('queue-badge');
  const searchInput = document.getElementById('search-input');
  const clearSearchBtn = document.getElementById('clear-search');

  // Playback Bar Elements
  const barTitle = document.getElementById('bar-title');
  const barArtist = document.getElementById('bar-artist');
  const barArtwork = document.getElementById('bar-artwork');
  const soundwaveAnim = document.getElementById('soundwave-anim');
  const barBtnFavorite = document.getElementById('bar-btn-favorite');
  const btnPlayPause = document.getElementById('btn-play-pause');
  const btnPrev = document.getElementById('btn-prev');
  const btnNext = document.getElementById('btn-next');
  const btnShuffle = document.getElementById('btn-shuffle');
  const btnRepeat = document.getElementById('btn-repeat');
  const seekSlider = document.getElementById('seek-slider');
  const currentTimeLabel = document.getElementById('current-time');
  const totalTimeLabel = document.getElementById('total-time');
  const volumeSlider = document.getElementById('volume-slider');
  const btnMute = document.getElementById('btn-mute');

  // Queue Drawer Elements
  const queueDrawer = document.getElementById('queue-drawer');
  const queueList = document.getElementById('queue-list');
  const drawerQueueCount = document.getElementById('drawer-queue-count');
  const btnToggleQueue = document.getElementById('btn-toggle-queue');
  const btnCloseQueue = document.getElementById('btn-close-queue');
  const btnClearQueue = document.getElementById('btn-clear-queue');

  // File Inputs
  const folderInput = document.getElementById('folder-input');
  const fileInput = document.getElementById('file-input');

  // Download Elements
  const dlUrlInput = document.getElementById('dl-url-input');
  const dlTitleInput = document.getElementById('dl-title-input');
  const dlArtistInput = document.getElementById('dl-artist-input');
  const btnDownloadUrl = document.getElementById('btn-download-url');
  const dlProgressBox = document.getElementById('dl-progress-box');
  const dlProgressFill = document.getElementById('dl-progress-fill');
  const dlStatusText = document.getElementById('dl-status-text');
  const dlPercentText = document.getElementById('dl-percent-text');
  const catalogGrid = document.getElementById('catalog-grid');

  // Toast Container
  let toastContainer = document.querySelector('.toast-container');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container';
    document.body.appendChild(toastContainer);
  }

  // Curated catalog for offline & online download
  const CURATED_CATALOG = [
    {
      id: 'cat-1',
      title: 'Midnight Drive',
      artist: 'Neon Dreams',
      album: 'Synthwave Essentials',
      duration: '3:24',
      genre: 'Synthwave',
      url: 'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3'
    },
    {
      id: 'cat-2',
      title: 'Lofi Study Session',
      artist: 'Coffee & Rain',
      album: 'Chill Beats Vol. 1',
      duration: '2:45',
      genre: 'Lofi',
      url: 'https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3'
    },
    {
      id: 'cat-3',
      title: 'Acoustic Breeze',
      artist: 'Sunset Valley',
      album: 'Acoustic Serenade',
      duration: '3:10',
      genre: 'Acoustic',
      url: 'https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3'
    },
    {
      id: 'cat-4',
      title: 'Cyber City Nights',
      artist: 'Vektor 86',
      album: 'Neon Nights',
      duration: '4:02',
      genre: 'Electronic',
      url: 'https://cdn.pixabay.com/download/audio/2023/04/09/audio_247167664f.mp3'
    },
    {
      id: 'cat-5',
      title: 'Peaceful Piano Reflections',
      artist: 'Elena Rostova',
      album: 'Serenity',
      duration: '2:55',
      genre: 'Classical',
      url: 'https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3'
    },
    {
      id: 'cat-6',
      title: 'Deep House Groove',
      artist: 'Skyline Collective',
      album: 'Club Resonance',
      duration: '3:40',
      genre: 'Deep House',
      url: 'https://cdn.pixabay.com/download/audio/2021/08/04/audio_bb630cc098.mp3'
    }
  ];

  /* ==========================================================================
     Procedural Audio Synthesizer (Fallback in case of network unavailability)
     ========================================================================== */
  function generateSyntheticWav(title, durationSeconds = 16) {
    const sampleRate = 44100;
    const numChannels = 2;
    const numFrames = sampleRate * durationSeconds;
    const buffer = new ArrayBuffer(44 + numFrames * numChannels * 2);
    const view = new DataView(buffer);

    writeString(view, 0, 'RIFF');
    view.setUint32(4, 36 + numFrames * numChannels * 2, true);
    writeString(view, 8, 'WAVE');
    writeString(view, 12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true); // PCM
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * numChannels * 2, true);
    view.setUint16(32, numChannels * 2, true);
    view.setUint16(34, 16, true);
    writeString(view, 36, 'data');
    view.setUint32(40, numFrames * numChannels * 2, true);

    const notes = [220.0, 261.63, 329.63, 392.0, 293.66, 349.23, 440.0, 523.25];
    const offset = Math.abs(hashCode(title)) % notes.length;

    let idx = 44;
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
      view.setInt16(idx, val, true);
      view.setInt16(idx + 2, val * 0.95, true);
      idx += 4;
    }

    const blob = new Blob([buffer], { type: 'audio/wav' });
    return URL.createObjectURL(blob);
  }

  function writeString(view, offset, string) {
    for (let i = 0; i < string.length; i++) {
      view.setUint8(offset + i, string.charCodeAt(i));
    }
  }

  function hashCode(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = (hash << 5) - hash + str.charCodeAt(i);
      hash |= 0;
    }
    return hash;
  }

  /* ==========================================================================
     Canvas Artwork Generator
     ========================================================================== */
  const PALETTES = [
    ['#FF2D55', '#5856D6'],
    ['#FF375F', '#FF9F0A'],
    ['#30D158', '#0A84FF'],
    ['#BF5AF2', '#FF2D55'],
    ['#64D2FF', '#0040DD'],
    ['#FF453A', '#FFD60A']
  ];

  function drawArtwork(canvas, title, artist) {
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;

    const hash = Math.abs(hashCode((title || '') + (artist || '')));
    const pal = PALETTES[hash % PALETTES.length];

    const grad = ctx.createLinearGradient(0, 0, w, h);
    grad.addColorStop(0, pal[0]);
    grad.addColorStop(1, pal[1]);

    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.roundRect(0, 0, w, h, w * 0.18);
    ctx.fill();

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.lineWidth = w * 0.02;
    ctx.beginPath();
    ctx.arc(w / 2, h / 2, w * 0.36, 0, Math.PI * 2);
    ctx.stroke();

    ctx.beginPath();
    ctx.arc(w / 2, h / 2, w * 0.22, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.font = `bold ${Math.floor(w * 0.42)}px -apple-system, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('♫', w / 2, h / 2 + (w * 0.02));
  }

  /* ==========================================================================
     Toast & Modal Notifications
     ========================================================================== */
  function showToast(message, type = 'normal') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span>${type === 'success' ? '✔' : (type === 'danger' ? '🗑' : 'ℹ')}</span> <span>${escapeHtml(message)}</span>`;
    toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3200);
  }

  function showConfirmModal(title, message, onConfirm) {
    const existing = document.querySelector('.modal-overlay');
    if (existing) existing.remove();

    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal-card">
        <div class="modal-title">${escapeHtml(title)}</div>
        <div class="modal-desc">${escapeHtml(message)}</div>
        <div class="modal-buttons">
          <button class="btn-modal-cancel">Cancel</button>
          <button class="btn-modal-delete">Delete</button>
        </div>
      </div>
    `;

    overlay.querySelector('.btn-modal-cancel').addEventListener('click', () => overlay.remove());
    overlay.querySelector('.btn-modal-delete').addEventListener('click', () => {
      overlay.remove();
      if (onConfirm) onConfirm();
    });

    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) overlay.remove();
    });

    document.body.appendChild(overlay);
  }

  /* ==========================================================================
     Formatting Utilities
     ========================================================================== */
  function formatTime(seconds) {
    if (isNaN(seconds) || seconds < 0) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  function cleanFileName(filename) {
    return filename.replace(/\.[^/.]+$/, '').replace(/^[0-9]+[\s.\-_]+/, '').trim();
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, m => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[m]));
  }

  /* ==========================================================================
     Audio Playback Functions
     ========================================================================== */
  function playTrack(track) {
    if (!track) return;
    state.currentTrack = track;

    if (state.queueIndex >= 0 && state.queue[state.queueIndex]) {
      state.history.push(state.queue[state.queueIndex]);
    }

    state.queueIndex = state.queue.findIndex(t => t.id === track.id);
    if (state.queueIndex === -1) {
      state.queue.push(track);
      state.queueIndex = state.queue.length - 1;
    }

    // Resolve URL (handle relative URLs if opened directly or via localhost)
    let finalUrl = track.url;
    if (finalUrl && finalUrl.startsWith('/') && window.location.protocol === 'file:') {
      finalUrl = 'http://localhost:3000' + finalUrl;
    }

    try {
      audio.preload = 'auto';
      audio.pause();
      audio.src = finalUrl;
      audio.load();

      const playPromise = audio.play();
      if (playPromise !== undefined) {
        playPromise.then(() => {
          state.isPlaying = true;
          updatePlaybackUI();
          renderTrackTable();
          renderTrackGrid();
          renderQueue();
        }).catch(err => {
          if (err.name === 'AbortError') {
            return; // Clean rapid skipping without error
          }
          console.warn('Playback gesture/format notice:', err.message);
          state.isPlaying = false;
          updatePlaybackUI();
          showToast(`Click ▶ to start playing "${track.title}"`, 'normal');
        });
      }
    } catch (err) {
      console.error('Audio playback error:', err);
      state.isPlaying = false;
      updatePlaybackUI();
    }

    updatePlaybackUI();
    renderTrackTable();
    renderTrackGrid();
    renderQueue();
  }

  function togglePlayPause() {
    if (!state.currentTrack) {
      if (state.queue.length > 0) {
        playTrack(state.queue[0]);
      } else if (state.library.length > 0) {
        playTrack(state.library[0]);
      }
      return;
    }

    if (audio.paused) {
      audio.play().then(() => {
        state.isPlaying = true;
        updatePlaybackUI();
      });
    } else {
      audio.pause();
      state.isPlaying = false;
      updatePlaybackUI();
    }
  }

  function playNext() {
    if (state.queue.length === 0) return;

    if (state.repeatMode === 'one' && state.queueIndex >= 0) {
      audio.currentTime = 0;
      audio.play();
      return;
    }

    if (state.queueIndex < state.queue.length - 1) {
      state.queueIndex++;
      playTrack(state.queue[state.queueIndex]);
    } else if (state.repeatMode === 'all') {
      state.queueIndex = 0;
      playTrack(state.queue[0]);
    } else {
      audio.pause();
      state.isPlaying = false;
      updatePlaybackUI();
    }
  }

  function playPrev() {
    if (audio.currentTime > 3) {
      audio.currentTime = 0;
      return;
    }

    if (state.history.length > 0) {
      const prev = state.history.pop();
      playTrack(prev);
      return;
    }

    if (state.queueIndex > 0) {
      state.queueIndex--;
      playTrack(state.queue[state.queueIndex]);
    } else if (state.repeatMode === 'all' && state.queue.length > 0) {
      state.queueIndex = state.queue.length - 1;
      playTrack(state.queue[state.queueIndex]);
    }
  }

  function updatePlaybackUI() {
    const t = state.currentTrack;
    if (t) {
      barTitle.textContent = t.title;
      barArtist.textContent = t.artist || 'Unknown Artist';
      drawArtwork(barArtwork, t.title, t.artist);
      barBtnFavorite.textContent = t.isFavorite ? '♥' : '♡';
      barBtnFavorite.classList.toggle('active', !!t.isFavorite);
    } else {
      barTitle.textContent = 'Not Playing';
      barArtist.textContent = 'Select a song to start';
      drawArtwork(barArtwork, 'Music', '');
      barBtnFavorite.textContent = '♡';
      barBtnFavorite.classList.remove('active');
    }

    btnPlayPause.textContent = state.isPlaying ? '⏸' : '▶';
    soundwaveAnim.classList.toggle('playing', state.isPlaying);

    btnShuffle.classList.toggle('active', state.isShuffle);
    btnRepeat.classList.toggle('active', state.repeatMode !== 'off');
    btnRepeat.textContent = state.repeatMode === 'one' ? '🔂' : '🔁';

    queueBadge.textContent = state.queue.length;
    drawerQueueCount.textContent = `${state.queue.length} tracks`;
  }

  /* ==========================================================================
     Delete / Remove Song Feature
     ========================================================================== */
  function deleteTrack(track) {
    if (!track) return;

    showConfirmModal(
      'Delete Song',
      `Are you sure you want to remove "${track.title}" from your library and delete it from your device storage?`,
      () => {
        const title = track.title;

        // 1. If currently playing, stop or play next
        if (state.currentTrack && state.currentTrack.id === track.id) {
          audio.pause();
          audio.src = '';
          state.isPlaying = false;
          state.currentTrack = null;
          updatePlaybackUI();
          if (state.queue.length > 1) {
            playNext();
          }
        }

        // 2. Remove from library, queue, history
        state.library = state.library.filter(t => t.id !== track.id);
        state.queue = state.queue.filter(t => t.id !== track.id);
        state.history = state.history.filter(t => t.id !== track.id);
        state.queueIndex = state.queue.findIndex(t => t.id === state.currentTrack?.id);

        // 3. Notify backend server to delete file from disk if on device
        if (track.filename) {
          fetch('/api/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filename: track.filename })
          }).catch(() => {});
        }

        renderTrackTable();
        renderTrackGrid();
        renderQueue();
        updatePlaybackUI();
        showToast(`Removed "${title}" from library`, 'danger');
      }
    );
  }

  /* ==========================================================================
     Download Music into Device Storage Feature
     ========================================================================== */
  function downloadTrackToDevice(title, artist, album, url) {
    dlProgressBox.style.display = 'block';
    dlProgressFill.style.width = '10%';
    dlStatusText.textContent = `Connecting to download "${title}"...`;
    dlPercentText.textContent = '10%';

    let progress = 10;
    const progressTimer = setInterval(() => {
      progress += Math.floor(Math.random() * 18) + 8;
      if (progress > 90) progress = 90;
      dlProgressFill.style.width = `${progress}%`;
      dlPercentText.textContent = `${progress}%`;
    }, 150);

    // Call server API to download/save onto user's device hard drive
    fetch('/api/download', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, artist, album, url })
    })
      .then(res => res.json())
      .then(data => {
        clearInterval(progressTimer);
        dlProgressFill.style.width = '100%';
        dlPercentText.textContent = '100%';

        if (data.success && data.track) {
          const newTrack = data.track;
          dlStatusText.textContent = `✔ Saved to device storage: ${newTrack.filename}`;

          // Add to local state
          const existingIdx = state.library.findIndex(t => t.filename === newTrack.filename);
          if (existingIdx !== -1) {
            state.library.splice(existingIdx, 1);
          }
          state.library.unshift(newTrack);
          state.queue.unshift(newTrack);

          renderTrackTable();
          renderTrackGrid();
          renderQueue();
          renderDeviceDownloads();
          updatePlaybackUI();

          // Show action buttons: Play Now or Go to Library
          const dlActionsRow = document.getElementById('dl-actions-row');
          if (dlActionsRow) {
            dlActionsRow.style.display = 'flex';
            const btnPlay = document.getElementById('btn-dl-play-now');
            const btnLib = document.getElementById('btn-dl-go-library');
            if (btnPlay) {
              btnPlay.onclick = () => {
                playTrack(newTrack);
              };
            }
            if (btnLib) {
              btnLib.onclick = () => {
                switchView('songs');
              };
            }
          }

          // Automatically play the newly downloaded track so user hears it right away!
          playTrack(newTrack);
          showToast(`Now playing downloaded track: "${newTrack.title}"`, 'success');

          // Trigger browser file download so user also gets it in browser Downloads folder
          if (newTrack.downloadUrl) {
            const a = document.createElement('a');
            a.href = newTrack.downloadUrl;
            a.download = newTrack.filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
          }
        } else {
          fallbackOfflineDownload(title, artist, album);
        }
      })
      .catch(() => {
        // Fallback for standalone/offline browser run without Node server
        clearInterval(progressTimer);
        fallbackOfflineDownload(title, artist, album);
      });
  }

  function fallbackOfflineDownload(title, artist, album) {
    dlProgressFill.style.width = '100%';
    dlPercentText.textContent = '100%';
    dlStatusText.textContent = `✔ Generated & saved to device: ${title}.wav`;

    const syntheticUrl = generateSyntheticWav(title, 20);
    const filename = `${artist ? artist + ' - ' : ''}${title}.wav`;

    const a = document.createElement('a');
    a.href = syntheticUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    const newTrack = {
      id: 'local-' + Date.now(),
      filename: filename,
      title: title,
      artist: artist || 'Downloaded',
      album: album || 'Offline Storage',
      duration: 20,
      url: syntheticUrl,
      downloadUrl: syntheticUrl,
      isFavorite: false,
      isDeviceStorage: true
    };

    state.library.unshift(newTrack);
    state.queue.unshift(newTrack);

    renderTrackTable();
    renderTrackGrid();
    renderQueue();
    renderDeviceDownloads();
    updatePlaybackUI();

    const dlActionsRow = document.getElementById('dl-actions-row');
    if (dlActionsRow) {
      dlActionsRow.style.display = 'flex';
      const btnPlay = document.getElementById('btn-dl-play-now');
      const btnLib = document.getElementById('btn-dl-go-library');
      if (btnPlay) {
        btnPlay.onclick = () => {
          playTrack(newTrack);
        };
      }
      if (btnLib) {
        btnLib.onclick = () => {
          switchView('songs');
        };
      }
    }

    // Automatically play the newly downloaded track
    playTrack(newTrack);
    showToast(`Now playing downloaded track: "${title}"`, 'success');
  }

  /* ==========================================================================
     Sync with Server / Load Device Tracks
     ========================================================================== */
  function fetchServerTracks() {
    fetch('/api/tracks')
      .then(res => res.json())
      .then(data => {
        if (data.tracks && Array.isArray(data.tracks) && data.tracks.length > 0) {
          // Prepend device storage tracks
          data.tracks.forEach(devTrack => {
            if (!state.library.some(t => t.filename === devTrack.filename)) {
              state.library.unshift(devTrack);
              state.queue.unshift(devTrack);
            }
          });
          renderTrackTable();
          renderTrackGrid();
          renderQueue();
          renderDeviceDownloads();
          updatePlaybackUI();
        }
      })
      .catch(() => {
        // Standalone offline mode
      });
  }

  /* ==========================================================================
     Render Device Downloads List
     ========================================================================== */
  function renderDeviceDownloads() {
    const listEl = document.getElementById('device-downloads-list');
    const countEl = document.getElementById('device-downloads-count');
    if (!listEl) return;

    const devTracks = state.library.filter(t => t.isDeviceStorage || t.filename);
    if (countEl) countEl.textContent = devTracks.length;

    listEl.innerHTML = '';
    if (devTracks.length === 0) {
      listEl.innerHTML = '<div style="color:var(--text-muted); font-size:0.85rem; padding: 8px 0;">No downloaded tracks on device yet. Click "Get" on any song below or enter a link above!</div>';
      return;
    }

    devTracks.forEach(track => {
      const isCurrent = state.currentTrack && state.currentTrack.id === track.id;
      const isPlayingCurrent = isCurrent && state.isPlaying;
      const card = document.createElement('div');
      card.className = `device-dl-card ${isCurrent ? 'active' : ''}`;
      card.style.cursor = 'pointer';
      card.innerHTML = `
        <canvas width="42" height="42" style="border-radius:6px; flex-shrink:0;" id="dev-art-${track.id}"></canvas>
        <div style="flex:1; overflow:hidden;">
          <div style="font-size:0.88rem; font-weight:600; color:${isCurrent ? 'var(--accent-pink)' : 'var(--text-white)'}; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(track.title)}</div>
          <div style="font-size:0.76rem; color:var(--text-muted);">${escapeHtml(track.artist || 'Downloaded')} • ${formatTime(track.duration || 180)}</div>
        </div>
        <div class="device-dl-actions">
          <button class="btn-dl-play" data-id="${track.id}">${isPlayingCurrent ? '⏸ Pause' : '▶ Play'}</button>
          <button class="btn-dl-del" data-id="${track.id}" title="Delete from device">🗑</button>
        </div>
      `;

      card.addEventListener('click', (e) => {
        if (e.target.closest('.btn-dl-del')) return;
        if (isCurrent) {
          togglePlayPause();
        } else {
          playTrack(track);
        }
      });

      card.querySelector('.btn-dl-play').addEventListener('click', (e) => {
        e.stopPropagation();
        if (isCurrent) {
          togglePlayPause();
        } else {
          playTrack(track);
        }
      });

      card.querySelector('.btn-dl-del').addEventListener('click', (e) => {
        e.stopPropagation();
        deleteTrack(track);
      });

      listEl.appendChild(card);
      const canvas = card.querySelector(`#dev-art-${track.id}`);
      drawArtwork(canvas, track.title, track.artist);
    });
  }

  /* ==========================================================================
     Render Views (Table, Grid, Queue, Catalog)
     ========================================================================== */
  function getFilteredTracks() {
    let list = [...state.library];

    if (state.currentView === 'favorites') {
      list = list.filter(t => t.isFavorite);
    } else if (state.currentView === 'recent') {
      list = list.slice(-10).reverse();
    }

    if (state.searchQuery) {
      const q = state.searchQuery.toLowerCase();
      list = list.filter(t =>
        t.title.toLowerCase().includes(q) ||
        (t.artist && t.artist.toLowerCase().includes(q)) ||
        (t.album && t.album.toLowerCase().includes(q))
      );
    }

    return list;
  }

  function renderTrackTable() {
    const tracks = getFilteredTracks();
    trackTableBody.innerHTML = '';

    tracks.forEach((track, i) => {
      const isCurrent = state.currentTrack && state.currentTrack.id === track.id;
      const isPlayingCurrent = isCurrent && state.isPlaying;
      const tr = document.createElement('tr');
      tr.className = `track-row ${isCurrent ? 'active' : ''}`;

      tr.innerHTML = `
        <td class="col-num" title="Click to play">
          <span class="num-text">${isPlayingCurrent ? '♫' : (i + 1)}</span>
          <span class="row-play-btn">${isPlayingCurrent ? '⏸' : '▶'}</span>
        </td>
        <td class="col-art"><canvas class="row-art-canvas" width="36" height="36" id="art-${track.id}"></canvas></td>
        <td class="col-title" title="${escapeHtml(track.title)}">${escapeHtml(track.title)}</td>
        <td class="col-artist" title="${escapeHtml(track.artist || '')}">${escapeHtml(track.artist || 'Unknown Artist')}</td>
        <td class="col-album" title="${escapeHtml(track.album || '')}">${escapeHtml(track.album || 'Unknown Album')}</td>
        <td class="col-fav">
          <button class="btn-row-fav ${track.isFavorite ? 'active' : ''}" data-id="${track.id}">
            ${track.isFavorite ? '♥' : '♡'}
          </button>
        </td>
        <td class="col-duration">${formatTime(track.duration || 0)}</td>
        <td class="col-actions">
          <div class="row-actions-group">
            <button class="btn-row-play" data-id="${track.id}" title="${isPlayingCurrent ? 'Pause' : 'Play Song'}">${isPlayingCurrent ? '⏸' : '▶'}</button>
            <button class="btn-row-menu" data-id="${track.id}" title="Add to Queue">+</button>
            <button class="btn-row-delete" data-id="${track.id}" title="Delete Song">🗑</button>
          </div>
        </td>
      `;

      // Single click on row plays immediately!
      tr.addEventListener('click', (e) => {
        if (e.target.closest('.btn-row-fav') || e.target.closest('.btn-row-menu') || e.target.closest('.btn-row-delete')) {
          return;
        }
        if (isCurrent) {
          togglePlayPause();
        } else {
          state.queue = [...tracks];
          playTrack(track);
        }
      });

      const btnRowPlay = tr.querySelector('.btn-row-play');
      if (btnRowPlay) {
        btnRowPlay.addEventListener('click', (e) => {
          e.stopPropagation();
          if (isCurrent) {
            togglePlayPause();
          } else {
            state.queue = [...tracks];
            playTrack(track);
          }
        });
      }

      trackTableBody.appendChild(tr);

      const canvas = tr.querySelector(`#art-${track.id}`);
      drawArtwork(canvas, track.title, track.artist);
    });

    updateStats(tracks.length);
  }

  function renderTrackGrid() {
    const tracks = getFilteredTracks();
    trackGrid.innerHTML = '';

    tracks.forEach(track => {
      const card = document.createElement('div');
      card.className = `grid-card ${state.currentTrack && state.currentTrack.id === track.id ? 'active' : ''}`;

      card.innerHTML = `
        <button class="card-delete-btn" data-id="${track.id}" title="Delete Song">🗑</button>
        <div class="card-artwork-wrapper">
          <canvas class="card-art-canvas" width="140" height="140" id="grid-art-${track.id}"></canvas>
          <div class="card-play-overlay">▶</div>
        </div>
        <div class="card-title" title="${escapeHtml(track.title)}">${escapeHtml(track.title)}</div>
        <div class="card-subtitle" title="${escapeHtml(track.artist || '')}">${escapeHtml(track.artist || 'Unknown Artist')}</div>
      `;

      card.addEventListener('click', (e) => {
        if (e.target.closest('.card-delete-btn')) return;
        state.queue = [...tracks];
        playTrack(track);
      });

      const btnDelete = card.querySelector('.card-delete-btn');
      btnDelete.addEventListener('click', (e) => {
        e.stopPropagation();
        deleteTrack(track);
      });

      trackGrid.appendChild(card);

      const canvas = card.querySelector(`#grid-art-${track.id}`);
      drawArtwork(canvas, track.title, track.artist);
    });
  }

  function renderQueue() {
    queueList.innerHTML = '';

    if (state.queue.length === 0) {
      queueList.innerHTML = '<div style="text-align:center; padding:30px 10px; color:var(--text-muted); font-size:0.85rem;">Queue is empty</div>';
      return;
    }

    state.queue.forEach((track, idx) => {
      const item = document.createElement('div');
      item.className = `queue-item ${state.queueIndex === idx ? 'active' : ''}`;

      item.innerHTML = `
        <canvas class="queue-item-art" width="36" height="36" id="q-art-${track.id}-${idx}"></canvas>
        <div class="queue-item-info">
          <div class="queue-item-title">${escapeHtml(track.title)}</div>
          <div class="queue-item-artist">${escapeHtml(track.artist || 'Unknown')}</div>
        </div>
        <button class="btn-remove-queue" data-index="${idx}" title="Remove from queue">✕</button>
      `;

      item.addEventListener('click', (e) => {
        if (e.target.classList.contains('btn-remove-queue')) return;
        state.queueIndex = idx;
        playTrack(track);
      });

      queueList.appendChild(item);

      const canvas = item.querySelector(`#q-art-${track.id}-${idx}`);
      drawArtwork(canvas, track.title, track.artist);
    });

    queueList.querySelectorAll('.btn-remove-queue').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const removeIdx = parseInt(btn.dataset.index, 10);
        state.queue.splice(removeIdx, 1);
        if (removeIdx < state.queueIndex) {
          state.queueIndex--;
        } else if (removeIdx === state.queueIndex) {
          if (state.queueIndex >= state.queue.length) state.queueIndex = state.queue.length - 1;
        }
        renderQueue();
        updatePlaybackUI();
      });
    });
  }

  function renderCatalog() {
    catalogGrid.innerHTML = '';
    CURATED_CATALOG.forEach(song => {
      const card = document.createElement('div');
      card.className = 'catalog-card';

      // Check if song already exists in library
      const existing = state.library.find(t => 
        t.title.toLowerCase() === song.title.toLowerCase() ||
        (t.filename && t.filename.toLowerCase().includes(song.title.toLowerCase()))
      );
      const isCurrentPlaying = existing && state.currentTrack && state.currentTrack.id === existing.id && state.isPlaying;

      card.innerHTML = `
        <canvas class="catalog-art" width="54" height="54" id="cat-art-${song.id}"></canvas>
        <div class="catalog-info">
          <div class="catalog-title">${escapeHtml(song.title)}</div>
          <div class="catalog-artist">${escapeHtml(song.artist)}</div>
          <div class="catalog-meta">${song.genre} • ${song.duration}</div>
        </div>
        <button class="btn-get ${existing ? 'btn-downloaded' : ''}" data-id="${song.id}">
          ${existing ? (isCurrentPlaying ? '⏸ Playing' : '▶ Play') : '⤓ Get'}
        </button>
      `;

      const btnGet = card.querySelector('.btn-get');
      btnGet.addEventListener('click', () => {
        if (existing) {
          playTrack(existing);
        } else {
          downloadTrackToDevice(song.title, song.artist, song.album, song.url);
        }
      });

      catalogGrid.appendChild(card);
      const canvas = card.querySelector(`#cat-art-${song.id}`);
      drawArtwork(canvas, song.title, song.artist);
    });
  }

  function updateStats(filteredCount) {
    const total = state.library.length;
    let totalSecs = 0;
    state.library.forEach(t => totalSecs += (t.duration || 0));

    libraryCountBadge.textContent = total;
    const favCount = state.library.filter(t => t.isFavorite).length;
    favoritesCountBadge.textContent = favCount;

    pageStats.textContent = `${filteredCount} song${filteredCount === 1 ? '' : 's'} • ${formatTime(totalSecs)} total playtime`;
  }

  /* ==========================================================================
     Local Folder & Audio Files Import
     ========================================================================== */
  function handleFilesSelected(files) {
    const audioFiles = Array.from(files).filter(f =>
      f.type.startsWith('audio/') ||
      /\.(mp3|wav|ogg|m4a|aac|flac)$/i.test(f.name)
    );

    if (audioFiles.length === 0) {
      showToast('No supported audio files found (.mp3, .wav, .m4a)', 'danger');
      return;
    }

    let loadedCount = 0;
    let firstAdded = null;

    audioFiles.forEach(file => {
      const url = URL.createObjectURL(file);
      const title = cleanFileName(file.name);

      const track = {
        id: 'file-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6),
        filename: file.name,
        title: title,
        artist: 'Local Track',
        album: 'My Music',
        duration: 180,
        url: url,
        file: file,
        isFavorite: false
      };

      const tempAudio = new Audio(url);
      tempAudio.addEventListener('loadedmetadata', () => {
        if (!isNaN(tempAudio.duration) && tempAudio.duration > 0) {
          track.duration = Math.round(tempAudio.duration);
          renderTrackTable();
          renderTrackGrid();
        }
      });

      state.library.unshift(track);
      state.queue.unshift(track);
      if (!firstAdded) firstAdded = track;
      loadedCount++;
    });

    renderTrackTable();
    renderTrackGrid();
    renderQueue();
    renderDeviceDownloads();
    updatePlaybackUI();
    showToast(`Added ${loadedCount} song${loadedCount > 1 ? 's' : ''} to library`, 'success');
    switchView('songs');

    if (firstAdded) {
      playTrack(firstAdded);
    }
  }

  /* ==========================================================================
     Navigation & View Switching
     ========================================================================== */
  function switchView(viewName) {
    state.currentView = viewName;

    document.querySelectorAll('.nav-item').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.view === viewName);
    });

    const viewLibrary = document.getElementById('view-library');
    const viewDownload = document.getElementById('view-download');

    if (viewName === 'download') {
      viewLibrary.style.display = 'none';
      viewLibrary.classList.remove('active');
      viewDownload.style.display = 'block';
      viewDownload.classList.add('active');
      renderCatalog();
      renderDeviceDownloads();
    } else if (viewName === 'queue') {
      toggleQueueDrawer(true);
      return;
    } else {
      viewLibrary.style.display = 'block';
      viewLibrary.classList.add('active');
      viewDownload.style.display = 'none';
      viewDownload.classList.remove('active');

      if (viewName === 'favorites') {
        pageTitle.textContent = 'Favorites';
      } else if (viewName === 'recent') {
        pageTitle.textContent = 'Recently Added';
      } else if (viewName === 'albums') {
        pageTitle.textContent = 'Albums';
      } else if (viewName === 'artists') {
        pageTitle.textContent = 'Artists';
      } else {
        pageTitle.textContent = 'Songs';
      }

      renderTrackTable();
      renderTrackGrid();
    }
  }

  function toggleQueueDrawer(forceOpen) {
    if (forceOpen === true) {
      queueDrawer.classList.add('open');
    } else {
      queueDrawer.classList.toggle('open');
    }
  }

  /* ==========================================================================
     Event Listeners
     ========================================================================== */
  function initEventListeners() {
    document.querySelectorAll('.nav-item').forEach(btn => {
      btn.addEventListener('click', () => switchView(btn.dataset.view));
    });

    folderInput.addEventListener('change', (e) => handleFilesSelected(e.target.files));
    fileInput.addEventListener('change', (e) => handleFilesSelected(e.target.files));

    window.addEventListener('dragover', (e) => e.preventDefault());
    window.addEventListener('drop', (e) => {
      e.preventDefault();
      if (e.dataTransfer && e.dataTransfer.files.length > 0) {
        handleFilesSelected(e.dataTransfer.files);
      }
    });

    searchInput.addEventListener('input', (e) => {
      state.searchQuery = e.target.value;
      clearSearchBtn.style.display = state.searchQuery ? 'block' : 'none';
      renderTrackTable();
      renderTrackGrid();
    });

    clearSearchBtn.addEventListener('click', () => {
      searchInput.value = '';
      state.searchQuery = '';
      clearSearchBtn.style.display = 'none';
      renderTrackTable();
      renderTrackGrid();
    });

    document.getElementById('btn-list-view').addEventListener('click', () => {
      state.isGridView = false;
      document.getElementById('btn-list-view').classList.add('active');
      document.getElementById('btn-grid-view').classList.remove('active');
      listViewContainer.style.display = 'block';
      gridViewContainer.style.display = 'none';
    });

    document.getElementById('btn-grid-view').addEventListener('click', () => {
      state.isGridView = true;
      document.getElementById('btn-grid-view').classList.add('active');
      document.getElementById('btn-list-view').classList.remove('active');
      listViewContainer.style.display = 'none';
      gridViewContainer.style.display = 'block';
      renderTrackGrid();
    });

    document.getElementById('btn-play-all').addEventListener('click', () => {
      const tracks = getFilteredTracks();
      if (tracks.length > 0) {
        state.queue = [...tracks];
        playTrack(tracks[0]);
      }
    });

    document.getElementById('btn-shuffle-all').addEventListener('click', () => {
      const tracks = getFilteredTracks();
      if (tracks.length > 0) {
        state.queue = [...tracks].sort(() => Math.random() - 0.5);
        state.isShuffle = true;
        btnShuffle.classList.add('active');
        playTrack(state.queue[0]);
      }
    });

    btnPlayPause.addEventListener('click', togglePlayPause);
    btnNext.addEventListener('click', playNext);
    btnPrev.addEventListener('click', playPrev);

    btnShuffle.addEventListener('click', () => {
      state.isShuffle = !state.isShuffle;
      if (state.isShuffle) {
        const current = state.currentTrack;
        state.queue = state.queue.filter(t => t.id !== current?.id).sort(() => Math.random() - 0.5);
        if (current) state.queue.unshift(current);
        state.queueIndex = 0;
      }
      btnShuffle.classList.toggle('active', state.isShuffle);
      renderQueue();
    });

    btnRepeat.addEventListener('click', () => {
      if (state.repeatMode === 'off') state.repeatMode = 'all';
      else if (state.repeatMode === 'all') state.repeatMode = 'one';
      else state.repeatMode = 'off';
      updatePlaybackUI();
    });

    let isSeeking = false;

    seekSlider.addEventListener('mousedown', () => { isSeeking = true; });
    seekSlider.addEventListener('touchstart', () => { isSeeking = true; }, { passive: true });

    seekSlider.addEventListener('input', (e) => {
      isSeeking = true;
      if (audio.duration && !isNaN(audio.duration)) {
        const seekTo = (e.target.value / 100) * audio.duration;
        currentTimeLabel.textContent = formatTime(seekTo);
      }
    });

    const commitSeek = () => {
      if (audio.duration && !isNaN(audio.duration)) {
        const seekTo = (seekSlider.value / 100) * audio.duration;
        audio.currentTime = seekTo;
      }
      isSeeking = false;
    };

    seekSlider.addEventListener('change', commitSeek);
    seekSlider.addEventListener('mouseup', commitSeek);
    seekSlider.addEventListener('touchend', commitSeek);

    audio.addEventListener('timeupdate', () => {
      if (!isSeeking && audio.duration && !isNaN(audio.duration)) {
        const pct = (audio.currentTime / audio.duration) * 100;
        seekSlider.value = pct;
        currentTimeLabel.textContent = formatTime(audio.currentTime);
        totalTimeLabel.textContent = formatTime(audio.duration);
      }
    });

    audio.addEventListener('ended', playNext);

    audio.addEventListener('error', () => {
      const err = audio.error;
      console.error('Audio engine error:', err ? `${err.code}: ${err.message}` : 'Unknown error', audio.src);
      state.isPlaying = false;
      updatePlaybackUI();
      renderTrackTable();
      renderTrackGrid();
      showToast('Audio playback error: Unsupported format or missing file', 'danger');
    });

    volumeSlider.addEventListener('input', (e) => {
      state.volume = e.target.value / 100;
      audio.volume = state.volume;
      btnMute.textContent = state.volume === 0 ? '🔇' : (state.volume < 0.5 ? '🔉' : '🔊');
    });

    btnMute.addEventListener('click', () => {
      if (audio.volume > 0) {
        state.previousVolume = audio.volume;
        audio.volume = 0;
        volumeSlider.value = 0;
        btnMute.textContent = '🔇';
      } else {
        const restore = state.previousVolume > 0.05 ? state.previousVolume : 0.85;
        audio.volume = restore;
        volumeSlider.value = restore * 100;
        btnMute.textContent = restore < 0.5 ? '🔉' : '🔊';
      }
    });

    barBtnFavorite.addEventListener('click', () => {
      if (state.currentTrack) {
        state.currentTrack.isFavorite = !state.currentTrack.isFavorite;
        updatePlaybackUI();
        renderTrackTable();
      }
    });

    // Table delegated clicks for Favorite, Queue, and Delete buttons
    trackTableBody.addEventListener('click', (e) => {
      const favBtn = e.target.closest('.btn-row-fav');
      if (favBtn) {
        const id = favBtn.dataset.id;
        const track = state.library.find(t => t.id === id);
        if (track) {
          track.isFavorite = !track.isFavorite;
          renderTrackTable();
          updatePlaybackUI();
        }
        return;
      }

      const menuBtn = e.target.closest('.btn-row-menu');
      if (menuBtn) {
        const id = menuBtn.dataset.id;
        const track = state.library.find(t => t.id === id);
        if (track) {
          state.queue.push(track);
          renderQueue();
          updatePlaybackUI();
          showToast(`Added "${track.title}" to Up Next`, 'normal');
        }
        return;
      }

      const deleteBtn = e.target.closest('.btn-row-delete');
      if (deleteBtn) {
        const id = deleteBtn.dataset.id;
        const track = state.library.find(t => t.id === id);
        if (track) {
          deleteTrack(track);
        }
      }
    });

    btnToggleQueue.addEventListener('click', () => toggleQueueDrawer());
    btnCloseQueue.addEventListener('click', () => toggleQueueDrawer(false));
    btnClearQueue.addEventListener('click', () => {
      state.queue = state.currentTrack ? [state.currentTrack] : [];
      state.queueIndex = state.currentTrack ? 0 : -1;
      renderQueue();
      updatePlaybackUI();
      showToast('Cleared Up Next queue', 'normal');
    });

    // URL Downloader form
    btnDownloadUrl.addEventListener('click', () => {
      const url = dlUrlInput.value.trim();
      const title = dlTitleInput.value.trim() || 'Downloaded Audio';
      const artist = dlArtistInput.value.trim() || 'Online Audio';

      if (!url) {
        showToast('Please enter an audio link to download', 'danger');
        return;
      }

      downloadTrackToDevice(title, artist, 'Web Download', url);
      dlUrlInput.value = '';
      dlTitleInput.value = '';
      dlArtistInput.value = '';
    });

    document.getElementById('btn-refresh-device-downloads')?.addEventListener('click', () => {
      fetchServerTracks();
      showToast('Refreshed device downloads', 'normal');
    });

    window.addEventListener('keydown', (e) => {
      if (e.target.tagName === 'INPUT') return;
      if (e.code === 'Space') {
        e.preventDefault();
        togglePlayPause();
      } else if (e.code === 'ArrowRight') {
        playNext();
      } else if (e.code === 'ArrowLeft') {
        playPrev();
      } else if (e.key === 's' || e.key === 'S') {
        btnShuffle.click();
      } else if (e.key === 'r' || e.key === 'R') {
        btnRepeat.click();
      }
    });
  }

  /* ==========================================================================
     Initialize Default Demo Tracks & Server Sync
     ========================================================================== */
  function initDefaultDemoTracks() {
    const demo1 = {
      id: 'demo-1',
      title: 'Chill Horizon',
      artist: 'Lofi Sunset',
      album: 'Ambient Vibes',
      duration: 18,
      url: generateSyntheticWav('Chill Horizon', 18),
      isFavorite: true
    };

    const demo2 = {
      id: 'demo-2',
      title: 'Velvet Sunset',
      artist: 'Acoustic Breeze',
      album: 'Summer Reverie',
      duration: 22,
      url: generateSyntheticWav('Velvet Sunset', 22),
      isFavorite: false
    };

    const demo3 = {
      id: 'demo-3',
      title: 'Midnight Melody',
      artist: 'Synth Dreamer',
      album: 'Nightscapes',
      duration: 20,
      url: generateSyntheticWav('Midnight Melody', 20),
      isFavorite: false
    };

    state.library = [demo1, demo2, demo3];
    state.queue = [demo1, demo2, demo3];

    renderTrackTable();
    renderTrackGrid();
    renderQueue();
    renderCatalog();
    updatePlaybackUI();

    // Check for any downloaded files on device disk via server
    fetchServerTracks();
  }

  // App Bootstrap
  document.addEventListener('DOMContentLoaded', () => {
    initEventListeners();
    initDefaultDemoTracks();
  });

})();
