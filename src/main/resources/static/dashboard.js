let token = localStorage.getItem('token');
const username = localStorage.getItem('username');

// Redirect to login if no token
if (!token) window.location.href = '/index.html';

function refreshSessionUI() {
    document.getElementById('usernameDisplay').textContent = '👤 ' + username;
}

window.onload = function () {
    refreshSessionUI();
    loadFiles();
};

function fileSelected() {
    const file = document.getElementById('fileInput').files[0];
    document.getElementById('selectedFile').textContent = file ? '✅ ' + file.name : '';
}

async function uploadFile() {
    const fileInput = document.getElementById('fileInput');
    const msg = document.getElementById('uploadMsg');

    if (!fileInput.files[0]) {
        showMsg(msg, 'Please select a file first', 'error'); return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    try {
        const res = await fetch('/files/upload', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + token },
            body: formData
        });
        const result = await res.text();
        showMsg(msg, result, res.ok ? 'success' : 'error');
        if (res.ok) {
            fileInput.value = '';
            document.getElementById('selectedFile').textContent = '';
            loadFiles();
        }
    } catch (e) {
        showMsg(msg, 'Upload failed: ' + e.message, 'error');
    }
}

async function loadFiles() {
    try {
        const res = await fetch('/files/list', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        const files = await res.json();
        renderFiles(files);
    } catch (e) {
        console.error('Failed to load files', e);
    }
}

function renderFiles(files) {
    const container = document.getElementById('fileTableContainer');
    if (!files || files.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon"></div>
                <p>No files yet. Upload your first file above.</p>
            </div>`;
        return;
    }

    let rows = files.map(f => `
        <tr>
            <td class="file-name">📄 ${f.originalName}</td>
            <td>${new Date(f.uploadTime).toLocaleString()}</td>
            <td>
                <button class="download-btn" onclick="downloadFile(${f.id}, '${f.originalName}')">
                    ⬇ Download
                </button>
            </td>
        </tr>
    `).join('');

    container.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>File Name</th>
                    <th>Uploaded</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>`;
}

async function downloadFile(id, name) {
    try {
        const res = await fetch('/files/download/' + id, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (!res.ok) { alert('Access denied or file not found'); return; }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = name; a.click();
        URL.revokeObjectURL(url);
    } catch (e) {
        alert('Download failed: ' + e.message);
    }
}

function logout() {
    localStorage.clear();
    window.location.href = '/index.html';
}

function showMsg(el, text, type) {
    el.textContent = text;
    el.className = 'msg ' + type;
}

