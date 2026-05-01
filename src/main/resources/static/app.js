function showTab(tab) {
    document.getElementById('loginForm').classList.toggle('hidden', tab !== 'login');
    document.getElementById('registerForm').classList.toggle('hidden', tab !== 'register');
    document.getElementById('forgotForm').classList.toggle('hidden', tab !== 'forgot');
    document.querySelectorAll('.tab').forEach((t, i) => {
        t.classList.toggle('active',
            (tab === 'login'    && i === 0) ||
            (tab === 'register' && i === 1) ||
            (tab === 'forgot'   && i === 2)
        );
    });
}

async function login() {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value.trim();
    const msg = document.getElementById('loginMsg');

    if (!username || !password) {
        showMsg(msg, 'Please fill in all fields', 'error'); return;
    }

    try {
        const res = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const result = await res.text();

        if (result.startsWith('ey')) {
            localStorage.setItem('token', result);
            const payload = JSON.parse(atob(result.split('.')[1]));
            localStorage.setItem('accessLevel', payload.accessLevel);
            localStorage.setItem('username', username);
            showMsg(msg, 'Login successful! Redirecting...', 'success');
            setTimeout(() => window.location.href = '/dashboard.html', 1000);
        } else {
            showMsg(msg, result, 'error');
        }
    } catch (e) {
        showMsg(msg, 'Connection error. Is the server running?', 'error');
    }
}

async function register() {
    const username = document.getElementById('regUsername').value.trim();
    const password1 = document.getElementById('regPassword1').value.trim();
    const password2 = document.getElementById('regPassword2').value.trim();
    const securityQuestion1 = document.getElementById('regSecurityQuestion1').value;
    const securityAnswer1 = document.getElementById('regSecurityAnswer1').value.trim();
    const securityQuestion2 = document.getElementById('regSecurityQuestion2').value;
    const securityAnswer2 = document.getElementById('regSecurityAnswer2').value.trim();
    const msg = document.getElementById('registerMsg');

    if (!username || !password1 || !password2 || !securityQuestion1 || !securityAnswer1 || !securityQuestion2 || !securityAnswer2) {
        showMsg(msg, 'Please fill in all fields including both security questions', 'error'); return;
    }

    try {
        const res = await fetch('/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password1, password2, securityQuestion1, securityAnswer1, securityQuestion2, securityAnswer2 })
        });
        const result = await res.text();
        if (result.includes('successfully')) {
            showMsg(msg, result + ' — You can now login!', 'success');
        } else {
            showMsg(msg, result, 'error');
        }
    } catch (e) {
        showMsg(msg, 'Connection error. Is the server running?', 'error');
    }
}

async function loadQuestion() {
    const username = document.getElementById('forgotUsername').value.trim();
    const msg = document.getElementById('forgotMsg');
    if (!username) { showMsg(msg, 'Enter your username first', 'error'); return; }

    try {
        const res = await fetch('/auth/security-questions?username=' + encodeURIComponent(username));
        if (!res.ok) {
            showMsg(msg, 'No account found with that username.', 'error');
            document.getElementById('questionBox').classList.add('hidden');
            return;
        }

        const data = await res.json();
        const select = document.getElementById('recoveryQuestion');
        select.innerHTML = `
            <option value="securityQuestion1">${data.securityQuestion1 || 'Not set'}</option>
            <option value="securityQuestion2">${data.securityQuestion2 || 'Not set'}</option>
        `;
        document.getElementById('questionBox').classList.remove('hidden');
        msg.className = 'msg';
    } catch (e) {
        showMsg(msg, 'Connection error', 'error');
    }
}

async function recoverByQuestion() {
    const username = document.getElementById('forgotUsername').value.trim();
    const questionKey = document.getElementById('recoveryQuestion').value;
    const answer = document.getElementById('forgotAnswer').value.trim();
    const newPassword = document.getElementById('forgotNewPassword').value.trim();
    const msg = document.getElementById('forgotMsg');

    if (!username || !questionKey || !answer || !newPassword) {
        showMsg(msg, 'Please complete all recovery fields', 'error'); return;
    }

    try {
        const res = await fetch('/auth/recover-by-question', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, questionKey, securityAnswer: answer, newPassword })
        });
        const result = await res.text();
        if (result.includes('successful')) {
            showMsg(msg, + result, 'success');
            document.getElementById('questionBox').classList.add('hidden');
            setTimeout(() => showTab('login'), 2500);
        } else {
            showMsg(msg, + result, 'error');
        }
    } catch (e) {
        showMsg(msg, 'Connection error', 'error');
    }
}

function showMsg(el, text, type) {
    el.textContent = text;
    el.className = 'msg ' + type;
}

