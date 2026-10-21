document.addEventListener('DOMContentLoaded', () => {
  const tabs = document.querySelectorAll('.tab-btn');
  const tabContents = document.querySelectorAll('.tab-content');
  const numAqi = document.getElementById('numAqi');
  const valAqi = document.getElementById('valAqi');
  const inputLat = document.getElementById('inputLat');
  const inputLon = document.getElementById('inputLon');
  const inputRadius = document.getElementById('inputRadius');
  const selectUser = document.getElementById('selectUser');
  const notifLat = document.getElementById('notifLat');
  const notifLon = document.getElementById('notifLon');
  const notifUser = document.getElementById('notifUser');
  const overridesTableBody = document.getElementById('overridesTableBody');
  const activeOverrideCount = document.getElementById('activeOverrideCount');
  const jsonOutput = document.getElementById('jsonOutput');
  const toastContainer = document.getElementById('toastContainer');

  tabs.forEach(btn => {
    btn.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      const targetId = btn.getAttribute('data-tab');
      document.getElementById(targetId)?.classList.add('active');
      if (targetId === 'tab-users') loadUsers();
      if (targetId === 'tab-notifications') loadAlertLogs();
      if (targetId === 'tab-simulator') loadOverrides();
    });
  });

  valAqi?.addEventListener('input', e => { numAqi.value = e.target.value; });
  numAqi?.addEventListener('input', e => { valAqi.value = e.target.value; });

  document.querySelectorAll('.city-chips .chip[data-lat]').forEach(chip => {
    chip.addEventListener('click', () => {
      const lat = chip.getAttribute('data-lat');
      const lon = chip.getAttribute('data-lon');
      inputLat.value = lat;
      inputLon.value = lon;
      notifLat.value = lat;
      notifLon.value = lon;
      showToast(`Selected ${chip.textContent.trim()} (${lat}, ${lon})`);
    });
  });

  document.getElementById('btnBrowserLoc')?.addEventListener('click', () => {
    if (!navigator.geolocation) {
      showToast('Geolocation not supported by browser', 'error');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      pos => {
        const lat = pos.coords.latitude.toFixed(4);
        const lon = pos.coords.longitude.toFixed(4);
        inputLat.value = lat;
        inputLon.value = lon;
        notifLat.value = lat;
        notifLon.value = lon;
        showToast(`GPS Location set: (${lat}, ${lon})`);
      },
      err => showToast(`GPS Error: ${err.message}`, 'error')
    );
  });

  document.querySelectorAll('.preset-card[data-preset]').forEach(card => {
    card.addEventListener('click', async () => {
      const presetKey = card.getAttribute('data-preset');
      const lat = parseFloat(inputLat.value) || 28.6139;
      const lon = parseFloat(inputLon.value) || 77.2090;
      const radiusKm = parseFloat(inputRadius.value) || 10.0;
      const userId = selectUser.value || null;

      try {
        const res = await fetch('/api/testing/overrides/preset', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ presetKey, latitude: lat, longitude: lon, radiusKm, targetUserId: userId })
        });
        if (!res.ok) throw new Error((await res.json()).message || 'Failed to apply preset');
        const data = await res.json();
        showToast(`Preset applied: ${data.label || presetKey}`, 'success');
        loadOverrides();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });

  document.getElementById('overrideForm')?.addEventListener('submit', async e => {
    e.preventDefault();
    const payload = {
      latitude: parseFloat(inputLat.value),
      longitude: parseFloat(inputLon.value),
      radiusKm: parseFloat(inputRadius.value) || 10.0,
      targetUserId: selectUser.value || null,
      label: `Custom Override (AQI ${numAqi.value})`,
      usAqi: parseInt(numAqi.value, 10),
      pm25: parseFloat(document.getElementById('valPm25').value),
      grassPollen: parseFloat(document.getElementById('valGrassPollen').value),
      birchPollen: parseFloat(document.getElementById('valBirchPollen').value),
      temperature2m: parseFloat(document.getElementById('valTemp').value),
      apparentTemperature: parseFloat(document.getElementById('valFeels').value),
      relativeHumidity2m: parseInt(document.getElementById('valHumidity').value, 10),
      weatherCode: parseInt(document.getElementById('valWeatherCode').value, 10),
      uvIndex: parseFloat(document.getElementById('valUv').value),
      windSpeed10m: parseFloat(document.getElementById('valWind').value),
      precipitation: parseFloat(document.getElementById('valPrecip').value),
      precipitationProbability: parseInt(document.getElementById('valPrecipProb').value, 10),
      waveHeight: parseFloat(document.getElementById('valWaveHeight').value) || null,
      seaSurfaceTemperature: parseFloat(document.getElementById('valSeaTemp').value) || null
    };

    try {
      const res = await fetch('/api/testing/overrides', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error((await res.json()).message || 'Failed to save override');
      showToast(`Custom simulation applied for (${payload.latitude}, ${payload.longitude})`, 'success');
      loadOverrides();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  document.getElementById('btnGlobalReset')?.addEventListener('click', async () => {
    if (!confirm('Clear all simulation overrides and reset backend to live Open-Meteo data?')) return;
    try {
      await fetch('/api/testing/overrides/clear', { method: 'POST' });
      showToast('All overrides cleared. Back to live data!', 'success');
      loadOverrides();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  async function loadOverrides() {
    try {
      const res = await fetch('/api/testing/overrides');
      const list = await res.json();
      activeOverrideCount.textContent = `${list.length} Active Override${list.length === 1 ? '' : 's'}`;

      if (!list.length) {
        overridesTableBody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No active overrides. Showing live Open-Meteo weather.</td></tr>`;
        return;
      }

      overridesTableBody.innerHTML = list.map(o => `
        <tr>
          <td><strong>${o.label || 'Simulation'}</strong></td>
          <td>${o.latitude.toFixed(4)}, ${o.longitude.toFixed(4)}</td>
          <td>${o.radiusKm} km</td>
          <td><span class="badge ${o.usAqi >= 300 ? 'badge-critical' : (o.usAqi >= 100 ? 'badge-warning' : 'badge-info')}">AQI ${o.usAqi ?? 'Live'}</span></td>
          <td>${o.temperature2m != null ? `${o.temperature2m}°C (feels ${o.apparentTemperature ?? o.temperature2m}°C)` : 'Live'}</td>
          <td>${o.weatherCode != null ? `Code ${o.weatherCode}` : 'Live'}</td>
          <td>${o.targetUserId ? `<span title="${o.targetUserId}">Specific User</span>` : 'All Users'}</td>
          <td><button class="btn btn-danger btn-xs" onclick="deleteOverride('${o.id}')">Delete</button></td>
        </tr>
      `).join('');
    } catch (err) {
      console.error(err);
    }
  }

  window.deleteOverride = async function(id) {
    try {
      await fetch(`/api/testing/overrides/${id}`, { method: 'DELETE' });
      showToast('Override deleted', 'success');
      loadOverrides();
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  document.getElementById('btnEvalAlerts')?.addEventListener('click', async () => {
    const lat = parseFloat(notifLat.value) || 28.6139;
    const lon = parseFloat(notifLon.value) || 77.2090;
    const userId = notifUser.value || null;
    const container = document.getElementById('evaluatedAlertsContainer');

    try {
      const res = await fetch('/api/testing/notifications/evaluate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ latitude: lat, longitude: lon, userId })
      });
      const alerts = await res.json();
      if (!alerts.length) {
        container.innerHTML = `<div class="empty-state">No weather alerts triggered for (${lat}, ${lon}). Conditions are within safe thresholds.</div>`;
        return;
      }
      container.innerHTML = alerts.map(a => `
        <div class="alert-card ${a.severity.toLowerCase()}">
          <div class="alert-card-head">
            <span class="alert-card-title">${a.headline}</span>
            <span class="badge badge-${a.severity.toLowerCase()}">${a.severity}</span>
          </div>
          <div class="alert-card-msg">${a.message}</div>
        </div>
      `).join('');
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  document.getElementById('btnTriggerAlert')?.addEventListener('click', async () => {
    const lat = parseFloat(notifLat.value) || 28.6139;
    const lon = parseFloat(notifLon.value) || 77.2090;
    const userId = notifUser.value || null;

    try {
      const res = await fetch('/api/testing/notifications/trigger', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ latitude: lat, longitude: lon, userId })
      });
      const data = await res.json();
      showToast(`⚡ Push Alert Dispatched: ${data.headline}`, 'success');
      loadAlertLogs();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  async function loadAlertLogs() {
    try {
      const res = await fetch('/api/testing/alert-logs');
      const list = await res.json();
      const body = document.getElementById('alertLogsTableBody');
      if (!list.length) {
        body.innerHTML = `<tr><td colspan="5" class="text-center text-muted">No alerts logged yet.</td></tr>`;
        return;
      }
      body.innerHTML = list.map(l => `
        <tr>
          <td>${new Date(l.sentAt).toLocaleTimeString()}</td>
          <td><span class="badge badge-${l.severity.toLowerCase()}">${l.severity}</span></td>
          <td><code>${l.alertType}</code></td>
          <td>${l.headline}</td>
          <td>${l.latitude ? `${l.latitude.toFixed(2)}, ${l.longitude.toFixed(2)}` : '—'}</td>
        </tr>
      `).join('');
    } catch (err) {
      console.error(err);
    }
  }

  async function loadUsers() {
    try {
      const res = await fetch('/api/testing/users');
      const list = await res.json();
      const userSelects = [selectUser, notifUser];
      userSelects.forEach(sel => {
        if (!sel) return;
        const curVal = sel.value;
        sel.innerHTML = '<option value="">All Users / Guest</option>' + list.map(u => `<option value="${u.id}">${u.email} (${u.username || 'No name'})</option>`).join('');
        sel.value = curVal;
      });

      const body = document.getElementById('usersTableBody');
      if (!list.length) {
        body.innerHTML = `<tr><td colspan="7" class="text-center text-muted">No registered users found. Sign up on app or backend first!</td></tr>`;
        return;
      }
      body.innerHTML = list.map(u => `
        <tr>
          <td><code title="${u.id}">${u.id.substring(0, 8)}...</code></td>
          <td><strong>${u.email}</strong></td>
          <td>${u.username || '—'}</td>
          <td>${u.personas.join(', ') || 'None'}</td>
          <td>AQI &gt; ${u.aqiThreshold}</td>
          <td>UV &gt; ${u.uvThreshold}</td>
          <td>${u.alertsEnabled ? '✅ Enabled' : '❌ Disabled'}</td>
        </tr>
      `).join('');
    } catch (err) {
      console.error(err);
    }
  }

  async function inspectEndpoint(url, label) {
    document.getElementById('inspectEndpointLabel').textContent = label;
    jsonOutput.textContent = 'Loading live API response...';
    try {
      const res = await fetch(url);
      const data = await res.json();
      jsonOutput.textContent = JSON.stringify(data, null, 2);
    } catch (err) {
      jsonOutput.textContent = `Error fetching ${url}: ${err.message}`;
    }
  }

  document.getElementById('btnInspectWeatherApi')?.addEventListener('click', () => {
    const lat = inputLat.value;
    const lon = inputLon.value;
    inspectEndpoint(`/api/testing/inspect-weather?lat=${lat}&lon=${lon}`, `GET /api/weather?lat=${lat}&lon=${lon}`);
  });

  document.getElementById('btnInspectHomepageApi')?.addEventListener('click', () => {
    const lat = inputLat.value;
    const lon = inputLon.value;
    const uid = selectUser.value ? `&userId=${selectUser.value}` : '';
    inspectEndpoint(`/api/testing/inspect-homepage?lat=${lat}&lon=${lon}${uid}`, `GET /api/homepage?lat=${lat}&lon=${lon}`);
  });

  document.getElementById('btnInspectPredictiveApi')?.addEventListener('click', () => {
    const lat = notifLat.value;
    const lon = notifLon.value;
    inspectEndpoint(`/api/testing/notifications/evaluate`, `POST /api/testing/notifications/evaluate`);
  });

  document.getElementById('btnInspectSimulated')?.addEventListener('click', () => {
    document.querySelector('[data-tab="tab-inspector"]')?.click();
    document.getElementById('btnInspectWeatherApi')?.click();
  });

  document.getElementById('btnCopyJson')?.addEventListener('click', () => {
    navigator.clipboard.writeText(jsonOutput.textContent);
    showToast('JSON copied to clipboard!');
  });

  document.getElementById('btnRefreshOverrides')?.addEventListener('click', loadOverrides);
  document.getElementById('btnRefreshAlertLogs')?.addEventListener('click', loadAlertLogs);
  document.getElementById('btnRefreshUsers')?.addEventListener('click', loadUsers);

  function showToast(msg, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = msg;
    toastContainer.appendChild(toast);
    setTimeout(() => { toast.remove(); }, 3500);
  }

  loadOverrides();
  loadUsers();
});
