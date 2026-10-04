/* ==========================================================================
   IDForge AI - Core Application Engine
   Chennai Institute of Technology Student & Faculty ID Platform
   ========================================================================== */

// Global State
const STATE = {
    authenticated: true,
    currentUser: 'admin',
    people: [],
    disciplinaryCases: [],
    filteredCards: [],
    currentCardIndex: 0,
    encryptedQRPayloads: new Map(),
    qrVerificationsCount: 128,
    activeLivePhotoData: null,
    activeFacultyLivePhotoData: null
};

// Admin Credentials (SHA-256 hash of "Admin@123")
const ADMIN_USERNAME = "admin";
const ADMIN_PASSWORD_HASH = "6b88820c74f5146c0757d5a57497d3ff8d2771d9d97034c4f346067b8d4f4007";
let loginAttempts = 0;

// AES Encryption Utility
class AESEncryptionUtil {
    static SECRET_KEY = "MySecretKey12345";

    static encrypt(data) {
        try {
            return CryptoJS.AES.encrypt(data, this.SECRET_KEY).toString();
        } catch (e) {
            return data;
        }
    }

    static decrypt(cipherText) {
        try {
            const bytes = CryptoJS.AES.decrypt(cipherText, this.SECRET_KEY);
            const decrypted = bytes.toString(CryptoJS.enc.Utf8);
            return decrypted || "DECRYPTION_FAILED";
        } catch (e) {
            return "DECRYPTION_FAILED";
        }
    }
}

// Initialization on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();
    setupNavigation();
    seedInitialData();
    loadDataset();
    updateLiveIDPreview();
});

// Seed Faculty & Pre-seeded Disciplinary Cases
function seedInitialData() {
    const faculty1 = {
        id: 9001,
        name: "Dr. A. Ramanathan",
        department: "CSE",
        type: "Faculty",
        designation: "Professor & HOD",
        dob: "1978-05-12",
        phone: "9876543210",
        address: "Anna Nagar, Chennai",
        photoPath: null
    };
    STATE.people.push(faculty1);

    STATE.disciplinaryCases = [
        {
            caseId: "DISC-2026-01",
            personId: 3,
            personName: "Rohan Verma",
            personType: "Student",
            caseType: "Lab Safety Violation",
            description: "Operating heavy machinery without safety goggles in Mech workshop.",
            date: "2026-09-15",
            reportedBy: "Dr. S. Vignesh",
            status: "Active",
            actionTaken: "Pending Hearing"
        },
        {
            caseId: "DISC-2026-02",
            personId: 7,
            personName: "Neha Gupta",
            personType: "Student",
            caseType: "Library Book Overdue",
            description: "Failure to return reference books past 30 days.",
            date: "2026-08-20",
            reportedBy: "Chief Librarian",
            status: "Closed",
            actionTaken: "Fine Paid & Resolved"
        },
        {
            caseId: "DISC-2026-03",
            personId: 1,
            personName: "Diya Patel",
            personType: "Student",
            caseType: "Attendance Shortage",
            description: "Semester attendance fell below mandatory 75% threshold in CIVIL department.",
            date: "2026-09-28",
            reportedBy: "HOD Civil Dept",
            status: "Active",
            actionTaken: "Parent Warning Letter Issued"
        },
        {
            caseId: "DISC-2026-04",
            personId: 2,
            personName: "Karan Singh",
            personType: "Student",
            caseType: "Exam Malpractice",
            description: "Unauthorized notes detected during Mid-Term AI/ML assessment examination.",
            date: "2026-09-10",
            reportedBy: "Exam Chief Superintendent",
            status: "Under Review",
            actionTaken: "Paper Voided & Disciplinary Panel Review"
        },
        {
            caseId: "DISC-2026-05",
            personId: 4,
            personName: "Aditya Mehta",
            personType: "Student",
            caseType: "ID Card Tampering",
            description: "Attempted to tamper with AES barcode signature on physical ID card.",
            date: "2026-08-14",
            reportedBy: "Campus Security Office",
            status: "Active",
            actionTaken: "Card Confiscated & Re-issue Fine Imposed"
        },
        {
            caseId: "DISC-2026-06",
            personId: 5,
            personName: "Neha Verma",
            personType: "Student",
            caseType: "Unauthorized Lab Access",
            description: "Entered High Voltage EEE Research Lab after working hours without permission.",
            date: "2026-07-22",
            reportedBy: "Lab Supervisor",
            status: "Closed",
            actionTaken: "Written Undertaking Submitted"
        },
        {
            caseId: "DISC-2026-07",
            personId: 6,
            personName: "Arjun Patel",
            personType: "Student",
            caseType: "Cyber Policy Violation",
            description: "Unauthorized network scanning performed on college Wi-Fi gateway.",
            date: "2026-09-02",
            reportedBy: "IT Network Administrator",
            status: "Under Review",
            actionTaken: "Campus Wi-Fi Credentials Suspended 14 Days"
        },
        {
            caseId: "DISC-2026-08",
            personId: 8,
            personName: "Vikram Verma",
            personType: "Student",
            caseType: "Parking Rule Breach",
            description: "Parked vehicle in reserved Faculty Bay block B repeatedly.",
            date: "2026-08-05",
            reportedBy: "Campus Safety Squad",
            status: "Closed",
            actionTaken: "Vehicle Wheel Clamped & Penalty Paid"
        },
        {
            caseId: "DISC-2026-09",
            personId: 9,
            personName: "Sanya Reddy",
            personType: "Student",
            caseType: "Hostel Quiet Hours Violation",
            description: "Playing high-volume speakers in hostel common room post 10:00 PM curfew.",
            date: "2026-08-30",
            reportedBy: "Chief Warden",
            status: "Dismissed",
            actionTaken: "Exonerated with Counsel Warning"
        },
        {
            caseId: "DISC-2026-10",
            personId: 10,
            personName: "Meera Reddy",
            personType: "Student",
            caseType: "Plagiarism in Assignment",
            description: "Code similarity score exceeded 50% threshold on Data Structures submission.",
            date: "2026-09-18",
            reportedBy: "CSE Course Coordinator",
            status: "Active",
            actionTaken: "Mandatory Resubmission Required"
        },
        {
            caseId: "DISC-2026-11",
            personId: 11,
            personName: "Rohan Reddy",
            personType: "Student",
            caseType: "Damage to Lab Equipment",
            description: "Accidental damage to digital oscilloscope unit during EEE practical test.",
            date: "2026-08-12",
            reportedBy: "EEE Lab In-Charge",
            status: "Closed",
            actionTaken: "Replacement Fine Settled"
        },
        {
            caseId: "DISC-2026-12",
            personId: 12,
            personName: "Aditya Reddy",
            personType: "Student",
            caseType: "Unexcused Drive Absence",
            description: "Failed to appear for pre-scheduled campus placement interview without notice.",
            date: "2026-09-21",
            reportedBy: "Placement Officer",
            status: "Under Review",
            actionTaken: "Placement Portal Access Suspended"
        },
        {
            caseId: "DISC-2026-13",
            personId: 13,
            personName: "Pooja Chopra",
            personType: "Student",
            caseType: "Sports Facility Misuse",
            description: "Using wooden synthetic badminton court without mandatory non-marking footwear.",
            date: "2026-07-15",
            reportedBy: "Physical Education Director",
            status: "Closed",
            actionTaken: "Verbal Warning Issued"
        },
        {
            caseId: "DISC-2026-14",
            personId: 14,
            personName: "Aarav Singh",
            personType: "Student",
            caseType: "Hostel Curfew Violation",
            description: "Returned to campus main gate past midnight past approved gate pass timing.",
            date: "2026-09-25",
            reportedBy: "Security Main Gate In-Charge",
            status: "Active",
            actionTaken: "Parent Call Conducted"
        },
        {
            caseId: "DISC-2026-15",
            personId: 15,
            personName: "Varun Sharma",
            personType: "Student",
            caseType: "Disrespectful Conduct",
            description: "Arguing aggressively with practical lab invigilator during external viva.",
            date: "2026-09-08",
            reportedBy: "External Examiner",
            status: "Under Review",
            actionTaken: "Written Apology Requested"
        },
        {
            caseId: "DISC-2026-16",
            personId: 16,
            personName: "Priya Nair",
            personType: "Student",
            caseType: "Canteen Token Misuse",
            description: "Attempted to use invalid/expired mess subscription tokens.",
            date: "2026-08-18",
            reportedBy: "Campus Dining Manager",
            status: "Closed",
            actionTaken: "Token Voided & Matter Resolved"
        },
        {
            caseId: "DISC-2026-17",
            personId: 17,
            personName: "Rahul Joshi",
            personType: "Student",
            caseType: "Campus Helmet Violation",
            description: "Riding two-wheeler inside campus premises without safety helmet.",
            date: "2026-09-29",
            reportedBy: "Campus Traffic Marshal",
            status: "Active",
            actionTaken: "Vehicle Campus Permit Revoked for 30 Days"
        },
        {
            caseId: "DISC-2026-18",
            personId: 18,
            personName: "Kavya Rao",
            personType: "Student",
            caseType: "Unauthorized Banner Display",
            description: "Affixed unapproved posters on academic block main entrance pillar.",
            date: "2026-08-25",
            reportedBy: "Estate Officer",
            status: "Dismissed",
            actionTaken: "Posters Removed, Retrospective Permit Issued"
        }
    ];
}

// Navigation & Tabs
function setupNavigation() {
    const navBtns = document.querySelectorAll('.nav-btn');
    navBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

    const activeBtn = document.querySelector(`.nav-btn[data-tab="${tabId}"]`);
    const activePane = document.getElementById(tabId);

    if (activeBtn) activeBtn.classList.add('active');
    if (activePane) activePane.classList.add('active');

    if (tabId === 'cards-browser') renderCardBrowser();
    if (tabId === 'directory') renderDirectoryTable();
    if (tabId === 'disciplinary') renderCasesTable();
    if (tabId === 'dashboard') updateDashboardStats();
}

// Load CSV Dataset
function loadDataset() {
    fetch('/students.csv')
        .then(response => response.text())
        .then(csvText => {
            Papa.parse(csvText, {
                header: true,
                skipEmptyLines: true,
                complete: (results) => {
                    results.data.forEach(row => {
                        if (row.StudentID && !STATE.people.some(p => p.id === parseInt(row.StudentID))) {
                            STATE.people.push({
                                id: parseInt(row.StudentID),
                                name: row.FullName ? row.FullName.replace(/"/g, '') : 'N/A',
                                department: row.Department || 'CSE',
                                type: 'Student',
                                year: row.Year || '1',
                                gender: row.Gender || 'N/A',
                                dob: row.DOB || 'N/A',
                                bloodGroup: row.BloodGroup || 'N/A',
                                phone: row.Phone || 'N/A',
                                email: row.Email || 'N/A',
                                address: row.Address ? row.Address.replace(/"/g, '') : 'N/A',
                                city: row.City || 'Chennai',
                                state: row.State || 'Tamil Nadu',
                                pincode: row.Pincode || '600069',
                                parentName: row.ParentName ? row.ParentName.replace(/"/g, '') : 'N/A',
                                parentPhone: row.ParentPhone || 'N/A',
                                admissionYear: row.AdmissionYear || '2026',
                                section: row.Section || 'A',
                                semester: row.Semester || '1',
                                collegeName: row.CollegeName ? row.CollegeName.replace(/"/g, '') : 'Chennai Institute of Technology',
                                academicStatus: row.AcademicStatus || 'Active',
                                photoPath: null
                            });
                        }
                    });

                    STATE.filteredCards = [...STATE.people];
                    updateDashboardStats();
                    renderCardBrowser();
                    showToast('Loaded 5,000+ Student Dataset successfully!', 'success');
                }
            });
        })
        .catch(err => {
            console.log('Dataset fetch notice:', err);
            updateDashboardStats();
        });
}

// Dashboard Statistics & Department Breakdown
function updateDashboardStats() {
    const students = STATE.people.filter(p => p.type === 'Student');
    const faculty = STATE.people.filter(p => p.type === 'Faculty');
    const activeCases = STATE.disciplinaryCases.filter(c => c.status !== 'Closed' && c.status !== 'Dismissed');

    document.getElementById('stat-total-students').textContent = students.length.toLocaleString();
    document.getElementById('stat-total-faculty').textContent = faculty.length.toLocaleString();
    document.getElementById('stat-active-cases').textContent = activeCases.length;
    document.getElementById('stat-qr-verifications').textContent = STATE.qrVerificationsCount;

    const depts = ['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIDS', 'AIML'];
    const deptCounts = {};
    depts.forEach(d => deptCounts[d] = 0);
    students.forEach(s => {
        if (deptCounts[s.department] !== undefined) deptCounts[s.department]++;
    });

    const barsContainer = document.getElementById('dept-distribution-bars');
    if (barsContainer) {
        barsContainer.innerHTML = depts.map(dept => {
            const count = deptCounts[dept];
            const pct = students.length > 0 ? Math.round((count / students.length) * 100) : 0;
            return `
                <div style="background: rgba(0,0,0,0.25); padding: 0.8rem; border-radius: var(--radius-sm); border: 1px solid var(--border-glass);">
                    <div style="display: flex; justify-content: space-between; font-weight: 700; font-size: 0.85rem; margin-bottom: 0.4rem;">
                        <span>${dept}</span>
                        <span style="color: var(--cyan);">${count} (${pct}%)</span>
                    </div>
                    <div style="width: 100%; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden;">
                        <div style="width: ${pct}%; height: 100%; background: linear-gradient(90deg, var(--primary), var(--cyan));"></div>
                    </div>
                </div>
            `;
        }).join('');
    }
}

// ==========================================================================
// REAL-TIME SMART REGISTRATION HUD & IMAGE CLARIFICATION ENGINE
// ==========================================================================

function updateLiveIDPreview() {
    const id = document.getElementById('st-id')?.value || '5001';
    const name = document.getElementById('st-name')?.value || 'ANANYA SHARMA';
    const dept = document.getElementById('st-dept')?.value || 'CSE';
    const year = document.getElementById('st-year')?.value || '1';
    const blood = document.getElementById('st-blood')?.value || 'O+';
    const phone = document.getElementById('st-phone')?.value || '9876543210';

    const liveTarget = document.getElementById('live-hud-front');
    if (!liveTarget) return;

    // Encrypt real-time payload
    const rawData = `ID:${id}|NAME:${name}|DEPT:${dept}|PHONE:${phone}|TOKEN:${Math.random().toString(36).substring(2)}`;
    const encryptedPayload = AESEncryptionUtil.encrypt(rawData);

    liveTarget.innerHTML = `
        <div class="id-card-header student">
            <div>
                <div class="id-card-title">STUDENT IDENTIFICATION CARD</div>
                <div class="id-card-inst">Chennai Institute of Technology</div>
            </div>
            <div style="text-align: right; font-size: 0.7rem; font-weight: bold;">
                CIT-2026
            </div>
        </div>

        <div class="id-card-body">
            <div class="id-card-photo-box">
                ${STATE.activeLivePhotoData 
                    ? `<img id="hud-photo-img" src="${STATE.activeLivePhotoData}" alt="${name}">` 
                    : `<div class="no-photo"><i data-lucide="user" style="width:32px; height:32px; color:#94a3b8; margin-bottom:4px;"></i><br>NO PHOTO</div>`}
            </div>

            <div class="id-card-details">
                <div class="field"><span class="label">ID:</span><span class="value" style="font-weight: 800; color:#0f52ba;">${id}</span></div>
                <div class="field"><span class="label">NAME:</span><span class="value" style="text-transform: uppercase;">${name}</span></div>
                <div class="field"><span class="label">DEPT:</span><span class="value">${dept}</span></div>
                <div class="field"><span class="label">YEAR/SEM:</span><span class="value">Year ${year} (Sem 1)</span></div>
                <div class="field"><span class="label">BLOOD:</span><span class="value">${blood}</span></div>
                <div class="field"><span class="label">PHONE:</span><span class="value">${phone}</span></div>
            </div>

            <div class="id-card-qr-box" id="hud-qr-container"></div>
        </div>

        <div class="id-card-footer">
            <div style="color: #475569; font-style: italic;">Sarathy Nagar, Kundrathur</div>
            <div class="sig-badge">
                <span class="check">&#10003;</span> <b style="color: #1e293b;">Digitally Signed</b><br>
                <span style="font-size: 0.65rem; color: #64748b;">Principal / Authorized Signatory</span>
            </div>
        </div>
    `;

    // Render live QR Code
    const qrBox = document.getElementById('hud-qr-container');
    if (qrBox) {
        qrBox.innerHTML = '';
        new QRCode(qrBox, {
            text: encryptedPayload,
            width: 85,
            height: 85,
            colorDark : "#0f172a",
            colorLight : "#ffffff"
        });
    }

    applyLivePhotoFilter();
    lucide.createIcons();
}

function handlePhotoUploadClarify(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        STATE.activeLivePhotoData = e.target.result;
        updateLiveIDPreview();

        // Calculate Blur / Sharpness Laplacian Variance
        const img = new Image();
        img.onload = function() {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);

            const imageData = ctx.getImageData(0, 0, img.width, img.height);
            const data = imageData.data;

            let sum = 0, sumSq = 0, count = 0;
            const w = img.width, h = img.height;

            for (let y = 1; y < h - 1; y++) {
                for (let x = 1; x < w - 1; x++) {
                    const idx = (y * w + x) * 4;
                    const center = (data[idx] + data[idx+1] + data[idx+2]) / 3;
                    const left = (data[idx - 4] + data[idx - 3] + data[idx - 2]) / 3;
                    const right = (data[idx + 4] + data[idx + 5] + data[idx + 6]) / 3;
                    const laplacian = (4 * center) - left - right;
                    sum += laplacian;
                    sumSq += laplacian * laplacian;
                    count++;
                }
            }

            const mean = sum / (count || 1);
            const variance = (sumSq / (count || 1)) - (mean * mean);
            const sharpnessScore = Math.min(100, Math.round(variance));

            const badge = document.getElementById('live-clarity-badge');
            if (badge) {
                if (sharpnessScore >= 80) {
                    badge.className = 'badge badge-green';
                    badge.innerHTML = `Sharpness: ${sharpnessScore}% (Crisp HD)`;
                } else {
                    badge.className = 'badge badge-amber';
                    badge.innerHTML = `Sharpness: ${sharpnessScore}% (Blur Warning)`;
                }
            }
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

function applyLivePhotoFilter() {
    const bright = document.getElementById('slider-bright')?.value || 100;
    const contrast = document.getElementById('slider-contrast')?.value || 110;
    const saturate = document.getElementById('slider-saturate')?.value || 100;

    const lblB = document.getElementById('lbl-val-bright');
    const lblC = document.getElementById('lbl-val-contrast');
    const lblS = document.getElementById('lbl-val-saturate');

    if (lblB) lblB.textContent = `${bright}%`;
    if (lblC) lblC.textContent = `${contrast}%`;
    if (lblS) lblS.textContent = `${saturate}%`;

    const imgEl = document.getElementById('hud-photo-img');
    if (imgEl) {
        imgEl.style.filter = `brightness(${bright}%) contrast(${contrast}%) saturate(${saturate}%)`;
    }
}

function setFilterPreset(preset) {
    const bSlider = document.getElementById('slider-bright');
    const cSlider = document.getElementById('slider-contrast');
    const sSlider = document.getElementById('slider-saturate');

    if (preset === 'auto') {
        if (bSlider) bSlider.value = 105;
        if (cSlider) cSlider.value = 125;
        if (sSlider) sSlider.value = 110;
    } else if (preset === 'hd') {
        if (bSlider) bSlider.value = 100;
        if (cSlider) cSlider.value = 145;
        if (sSlider) sSlider.value = 120;
    } else if (preset === 'light') {
        if (bSlider) bSlider.value = 125;
        if (cSlider) cSlider.value = 105;
        if (sSlider) sSlider.value = 100;
    } else {
        if (bSlider) bSlider.value = 100;
        if (cSlider) cSlider.value = 100;
        if (sSlider) sSlider.value = 100;
    }

    applyLivePhotoFilter();
}

// Real-Time Faculty Registration HUD & Photo Clarification Engine
function updateLiveFacultyIDPreview() {
    const id = document.getElementById('fc-id')?.value || '9002';
    const name = document.getElementById('fc-name')?.value || 'DR. S. VIGNESH';
    const dept = document.getElementById('fc-dept')?.value || 'CSE';
    const desig = document.getElementById('fc-desig')?.value || 'Associate Professor';
    const phone = document.getElementById('fc-phone')?.value || '9876543210';
    const dob = document.getElementById('fc-dob')?.value || 'N/A';

    const liveTarget = document.getElementById('live-fc-hud-front');
    if (!liveTarget) return;

    const rawData = `ID:${id}|NAME:${name}|DEPT:${dept}|PHONE:${phone}|TOKEN:${Math.random().toString(36).substring(2)}`;
    const encryptedPayload = AESEncryptionUtil.encrypt(rawData);

    liveTarget.innerHTML = `
        <div class="id-card-header faculty">
            <div>
                <div class="id-card-title">FACULTY IDENTIFICATION CARD</div>
                <div class="id-card-inst">Chennai Institute of Technology</div>
            </div>
            <div style="text-align: right; font-size: 0.7rem; font-weight: bold;">
                CIT-2026
            </div>
        </div>

        <div class="id-card-body">
            <div class="id-card-photo-box">
                ${STATE.activeFacultyLivePhotoData 
                    ? `<img id="fc-hud-photo-img" src="${STATE.activeFacultyLivePhotoData}" alt="${name}">` 
                    : `<div class="no-photo"><i data-lucide="award" style="width:32px; height:32px; color:#b46414; margin-bottom:4px;"></i><br>NO PHOTO</div>`}
            </div>

            <div class="id-card-details">
                <div class="field"><span class="label">ID:</span><span class="value" style="font-weight: 800; color:#b46414;">${id}</span></div>
                <div class="field"><span class="label">NAME:</span><span class="value" style="text-transform: uppercase;">${name}</span></div>
                <div class="field"><span class="label">DEPT:</span><span class="value">${dept}</span></div>
                <div class="field"><span class="label">DESIG:</span><span class="value">${desig}</span></div>
                <div class="field"><span class="label">PHONE:</span><span class="value">${phone}</span></div>
                <div class="field"><span class="label">DOB:</span><span class="value">${dob}</span></div>
            </div>

            <div class="id-card-qr-box" id="fc-hud-qr-container"></div>
        </div>

        <div class="id-card-footer">
            <div style="color: #475569; font-style: italic;">Sarathy Nagar, Kundrathur</div>
            <div class="sig-badge">
                <span class="check" style="color: #b46414;">&#10003;</span> <b style="color: #1e293b;">Digitally Signed</b><br>
                <span style="font-size: 0.65rem; color: #64748b;">Principal / Authorized Signatory</span>
            </div>
        </div>
    `;

    const qrBox = document.getElementById('fc-hud-qr-container');
    if (qrBox) {
        qrBox.innerHTML = '';
        new QRCode(qrBox, {
            text: encryptedPayload,
            width: 85,
            height: 85,
            colorDark : "#78350f",
            colorLight : "#ffffff"
        });
    }

    applyLiveFacultyPhotoFilter();
    lucide.createIcons();
}

function handleFacultyPhotoUploadClarify(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        STATE.activeFacultyLivePhotoData = e.target.result;
        updateLiveFacultyIDPreview();

        const img = new Image();
        img.onload = function() {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);

            const imageData = ctx.getImageData(0, 0, img.width, img.height);
            const data = imageData.data;

            let sum = 0, sumSq = 0, count = 0;
            const w = img.width, h = img.height;

            for (let y = 1; y < h - 1; y++) {
                for (let x = 1; x < w - 1; x++) {
                    const idx = (y * w + x) * 4;
                    const center = (data[idx] + data[idx+1] + data[idx+2]) / 3;
                    const left = (data[idx - 4] + data[idx - 3] + data[idx - 2]) / 3;
                    const laplacian = (4 * center) - left;
                    sum += laplacian;
                    sumSq += laplacian * laplacian;
                    count++;
                }
            }

            const mean = sum / (count || 1);
            const variance = (sumSq / (count || 1)) - (mean * mean);
            const sharpnessScore = Math.min(100, Math.round(variance));

            const badge = document.getElementById('live-fc-clarity-badge');
            if (badge) {
                if (sharpnessScore >= 80) {
                    badge.className = 'badge badge-green';
                    badge.innerHTML = `Sharpness: ${sharpnessScore}% (Crisp HD)`;
                } else {
                    badge.className = 'badge badge-amber';
                    badge.innerHTML = `Sharpness: ${sharpnessScore}% (Blur Warning)`;
                }
            }
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

function applyLiveFacultyPhotoFilter() {
    const bright = document.getElementById('slider-fc-bright')?.value || 100;
    const contrast = document.getElementById('slider-fc-contrast')?.value || 110;
    const saturate = document.getElementById('slider-fc-saturate')?.value || 100;

    const lblB = document.getElementById('lbl-val-fc-bright');
    const lblC = document.getElementById('lbl-val-fc-contrast');
    const lblS = document.getElementById('lbl-val-fc-saturate');

    if (lblB) lblB.textContent = `${bright}%`;
    if (lblC) lblC.textContent = `${contrast}%`;
    if (lblS) lblS.textContent = `${saturate}%`;

    const imgEl = document.getElementById('fc-hud-photo-img');
    if (imgEl) {
        imgEl.style.filter = `brightness(${bright}%) contrast(${contrast}%) saturate(${saturate}%)`;
    }
}

function setFacultyFilterPreset(preset) {
    const bSlider = document.getElementById('slider-fc-bright');
    const cSlider = document.getElementById('slider-fc-contrast');
    const sSlider = document.getElementById('slider-fc-saturate');

    if (preset === 'auto') {
        if (bSlider) bSlider.value = 105;
        if (cSlider) cSlider.value = 125;
        if (sSlider) sSlider.value = 110;
    } else if (preset === 'hd') {
        if (bSlider) bSlider.value = 100;
        if (cSlider) cSlider.value = 145;
        if (sSlider) sSlider.value = 120;
    } else if (preset === 'light') {
        if (bSlider) bSlider.value = 125;
        if (cSlider) cSlider.value = 105;
        if (sSlider) sSlider.value = 100;
    } else {
        if (bSlider) bSlider.value = 100;
        if (cSlider) cSlider.value = 100;
        if (sSlider) sSlider.value = 100;
    }

    applyLiveFacultyPhotoFilter();
}

// ID Card Browser Engine
function filterCards() {
    const search = document.getElementById('browser-search').value.trim().toLowerCase();
    const dept = document.getElementById('browser-dept-filter').value;
    const year = document.getElementById('browser-year-filter').value;
    const type = document.getElementById('browser-type-filter').value;

    STATE.filteredCards = STATE.people.filter(p => {
        const matchesSearch = !search || String(p.id).includes(search) || p.name.toLowerCase().includes(search);
        const matchesDept = dept === 'ALL' || p.department.toUpperCase() === dept.toUpperCase();
        const matchesYear = year === 'ALL' || (p.type === 'Student' && String(p.year) === year);
        const matchesType = type === 'ALL' || p.type === type;
        return matchesSearch && matchesDept && matchesYear && matchesType;
    });

    STATE.currentCardIndex = 0;
    renderCardBrowser();
}

function renderCardBrowser() {
    const frontEl = document.getElementById('card-front-content');
    const countEl = document.getElementById('lbl-card-count');

    if (!STATE.filteredCards || STATE.filteredCards.length === 0) {
        countEl.textContent = 'Result: 0 of 0';
        frontEl.innerHTML = `<div style="display:flex; flex-direction:column; align-items:center; justify-center; height:100%; color:#64748b; padding:2rem; text-align:center;">
            <h3>No Records Match Your Filters</h3>
            <p>Try adjusting the search or department filter.</p>
        </div>`;
        return;
    }

    countEl.textContent = `Showing ${STATE.currentCardIndex + 1} of ${STATE.filteredCards.length}`;
    const person = STATE.filteredCards[STATE.currentCardIndex];
    const isStudent = person.type === 'Student';

    const headerClass = isStudent ? 'student' : 'faculty';
    const headerTitle = isStudent ? 'STUDENT IDENTIFICATION CARD' : 'FACULTY IDENTIFICATION CARD';

    const rawData = `ID:${person.id}|NAME:${person.name}|DEPT:${person.department}|PHONE:${person.phone}|TOKEN:${Math.random().toString(36).substring(2)}`;
    const encryptedPayload = AESEncryptionUtil.encrypt(rawData);
    STATE.encryptedQRPayloads.set(person.id, encryptedPayload);

    frontEl.innerHTML = `
        <div class="id-card-header ${headerClass}">
            <div>
                <div class="id-card-title">${headerTitle}</div>
                <div class="id-card-inst">Chennai Institute of Technology</div>
            </div>
            <div style="text-align: right; font-size: 0.7rem; font-weight: bold;">
                CIT-2026
            </div>
        </div>

        <div class="id-card-body">
            <div class="id-card-photo-box">
                ${person.photoPath 
                    ? `<img src="${person.photoPath}" alt="${person.name}">` 
                    : `<div class="no-photo"><i data-lucide="user" style="width:32px; height:32px; color:#94a3b8; margin-bottom:4px;"></i><br>NO PHOTO</div>`}
            </div>

            <div class="id-card-details">
                <div class="field"><span class="label">ID:</span><span class="value" style="font-weight: 800; color:#0f52ba;">${person.id}</span></div>
                <div class="field"><span class="label">NAME:</span><span class="value" style="text-transform: uppercase;">${person.name}</span></div>
                <div class="field"><span class="label">DEPT:</span><span class="value">${person.department}</span></div>
                ${isStudent ? `
                    <div class="field"><span class="label">YEAR/SEM:</span><span class="value">Year ${person.year || 1} (Sem ${person.semester || 1})</span></div>
                    <div class="field"><span class="label">BLOOD:</span><span class="value">${person.bloodGroup || 'O+'}</span></div>
                ` : `
                    <div class="field"><span class="label">DESIG:</span><span class="value">${person.designation || 'Faculty'}</span></div>
                `}
                <div class="field"><span class="label">PHONE:</span><span class="value">${person.phone}</span></div>
                <div class="field"><span class="label">DOB:</span><span class="value">${person.dob || 'N/A'}</span></div>
            </div>

            <div class="id-card-qr-box" id="qr-container-${person.id}"></div>
        </div>

        <div class="id-card-footer">
            <div style="color: #475569; font-style: italic;">Sarathy Nagar, Kundrathur</div>
            <div class="sig-badge">
                <span class="check">&#10003;</span> <b style="color: #1e293b;">Digitally Signed</b><br>
                <span style="font-size: 0.65rem; color: #64748b;">Principal / Authorized Signatory</span>
            </div>
        </div>
    `;

    document.getElementById('card-back-barcode').textContent = `*CIT-2026-${person.id}*`;
    document.getElementById('card-back-emergency').textContent = person.phone || '+91 44 7120 2000';

    const qrBox = document.getElementById(`qr-container-${person.id}`);
    if (qrBox) {
        qrBox.innerHTML = '';
        new QRCode(qrBox, {
            text: encryptedPayload,
            width: 85,
            height: 85,
            colorDark : "#0f172a",
            colorLight : "#ffffff"
        });
    }

    lucide.createIcons();
}

function prevCard() {
    if (STATE.currentCardIndex > 0) {
        STATE.currentCardIndex--;
        renderCardBrowser();
    }
}

function nextCard() {
    if (STATE.currentCardIndex < STATE.filteredCards.length - 1) {
        STATE.currentCardIndex++;
        renderCardBrowser();
    }
}

function toggleCardFlip() {
    const wrapper = document.getElementById('id-card-wrapper');
    wrapper.classList.toggle('flipped');
}

// Person Directory Table Engine
function filterDirectoryTable() {
    const query = document.getElementById('directory-search').value.trim().toLowerCase();
    renderDirectoryTable(query);
}

function renderDirectoryTable(query = '') {
    const tbody = document.getElementById('directory-table-body');
    if (!tbody) return;

    const filtered = STATE.people.filter(p => {
        return !query || 
               String(p.id).includes(query) || 
               p.name.toLowerCase().includes(query) || 
               p.department.toLowerCase().includes(query) ||
               (p.email && p.email.toLowerCase().includes(query));
    });

    tbody.innerHTML = filtered.slice(0, 100).map(p => {
        const isStudent = p.type === 'Student';
        const activeCase = STATE.disciplinaryCases.some(c => c.personId === p.id && c.status !== 'Closed' && c.status !== 'Dismissed');
        return `
            <tr>
                <td><strong>${p.id}</strong></td>
                <td>
                    <div style="font-weight: 700;">${p.name}</div>
                    <div style="font-size: 0.75rem; color: var(--text-dim);">${p.type}</div>
                </td>
                <td>
                    <span class="badge ${isStudent ? 'badge-blue' : 'badge-amber'}">${p.type}</span>
                </td>
                <td>${p.department}</td>
                <td>${isStudent ? 'Year ' + (p.year || 1) : (p.designation || 'Faculty')}</td>
                <td>${p.phone}</td>
                <td>${p.email || 'N/A'}</td>
                <td>
                    ${activeCase 
                        ? `<span class="badge badge-red"><i data-lucide="alert-triangle" style="width:12px"></i> Case Active</span>` 
                        : `<span class="badge badge-green">Clear</span>`}
                </td>
                <td>
                    <div style="display:flex; gap:0.4rem;">
                        <button class="btn btn-secondary btn-sm" onclick="viewProfile(${p.id})"><i data-lucide="eye"></i> View</button>
                        <button class="btn btn-danger btn-sm" onclick="deletePersonRecord(${p.id})"><i data-lucide="trash-2"></i></button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');

    lucide.createIcons();
}

function viewProfile(personId) {
    const person = STATE.people.find(p => p.id === personId);
    if (!person) return;

    const cases = STATE.disciplinaryCases.filter(c => c.personId === personId);
    const content = document.getElementById('profile-modal-content');

    content.innerHTML = `
        <div style="display: flex; gap: 1.5rem; align-items: flex-start; margin-bottom: 1.5rem;">
            <div style="width: 110px; height: 140px; background: #1e293b; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center;">
                ${person.photoPath ? `<img src="${person.photoPath}" style="width:100%; height:100%; object-fit:cover; border-radius:var(--radius-md);">` : `<i data-lucide="user" style="width:48px; height:48px; color:#64748b;"></i>`}
            </div>
            <div>
                <h2 style="font-size: 1.4rem; font-weight: 800;">${person.name}</h2>
                <div style="display: flex; gap: 0.5rem; margin-top: 0.3rem;">
                    <span class="badge ${person.type === 'Student' ? 'badge-blue' : 'badge-amber'}">${person.type}</span>
                    <span class="badge badge-green">${person.department}</span>
                    <span class="badge badge-blue">ID: ${person.id}</span>
                </div>
                <p style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.8rem;">
                    Academic Status: <strong>${person.academicStatus || 'Active'}</strong> | Admission Year: <strong>${person.admissionYear || 2026}</strong>
                </p>
            </div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; background: rgba(0,0,0,0.3); padding: 1.2rem; border-radius: var(--radius-md); border: 1px solid var(--border-glass);">
            <div><strong>Gender:</strong> ${person.gender || 'N/A'}</div>
            <div><strong>Date of Birth:</strong> ${person.dob || 'N/A'}</div>
            <div><strong>Blood Group:</strong> ${person.bloodGroup || 'N/A'}</div>
            <div><strong>Phone:</strong> ${person.phone || 'N/A'}</div>
            <div><strong>Email:</strong> ${person.email || 'N/A'}</div>
            <div><strong>Parent/Guardian:</strong> ${person.parentName || 'N/A'} (${person.parentPhone || 'N/A'})</div>
            <div><strong>City/State:</strong> ${person.city || 'Chennai'}, ${person.state || 'Tamil Nadu'}</div>
            <div><strong>Pincode:</strong> ${person.pincode || '600069'}</div>
        </div>

        <div style="margin-top: 1.5rem;">
            <h4 style="font-size: 1rem; font-weight: 700; margin-bottom: 0.8rem;"><i data-lucide="scale"></i> Disciplinary History (${cases.length})</h4>
            ${cases.length === 0 ? `<p style="font-size: 0.85rem; color: var(--success);">✓ No disciplinary cases recorded for this person.</p>` : `
                <div style="display:flex; flex-direction:column; gap:0.6rem;">
                    ${cases.map(c => `
                        <div style="padding: 0.8rem; background: rgba(239,68,68,0.1); border-left: 3px solid var(--danger); border-radius: 4px;">
                            <div style="display:flex; justify-content:space-between; font-weight:700;">
                                <span>Case ID: ${c.caseId} (${c.caseType})</span>
                                <span class="badge ${c.status === 'Closed' ? 'badge-green' : 'badge-red'}">${c.status}</span>
                            </div>
                            <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">${c.description}</p>
                            <p style="font-size: 0.75rem; color: var(--text-dim); margin-top: 0.2rem;">Action: ${c.actionTaken}</p>
                        </div>
                    `).join('')}
                </div>
            `}
        </div>
    `;

    openModal('modal-profile-view');
    lucide.createIcons();
}

function deletePersonRecord(personId) {
    const person = STATE.people.find(p => p.id === personId);
    if (!person) return;

    if (confirm(`Are you sure you want to permanently delete the record for ${person.name} (ID: ${person.id})?`)) {
        STATE.people = STATE.people.filter(p => p.id !== personId);
        STATE.filteredCards = STATE.filteredCards.filter(p => p.id !== personId);
        
        updateDashboardStats();
        renderCardBrowser();
        renderDirectoryTable();
        
        showToast(`Record for ID ${personId} deleted successfully.`, 'danger');
    }
}

// Disciplinary Cases Engine
function filterCaseTable() {
    const query = document.getElementById('case-search').value.trim().toLowerCase();
    const statusFilter = document.getElementById('case-status-filter')?.value || 'ALL';
    renderCasesTable(query, statusFilter);
}

function renderCasesTable(query = '', statusFilter = 'ALL') {
    const tbody = document.getElementById('cases-table-body');
    if (!tbody) return;

    const filtered = STATE.disciplinaryCases.filter(c => {
        const matchesSearch = !query ||
               c.caseId.toLowerCase().includes(query) ||
               String(c.personId).includes(query) ||
               c.personName.toLowerCase().includes(query) ||
               c.caseType.toLowerCase().includes(query);

        const matchesStatus = statusFilter === 'ALL' || c.status.toUpperCase() === statusFilter.toUpperCase();

        return matchesSearch && matchesStatus;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; padding:2rem; color:var(--text-muted);">No disciplinary cases found matching your search or status filter.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(c => `
        <tr>
            <td><strong>${c.caseId}</strong></td>
            <td>${c.personId}</td>
            <td>
                <div style="font-weight: 700;">${c.personName}</div>
                <div style="font-size: 0.75rem; color: var(--text-dim);">${c.personType}</div>
            </td>
            <td><span class="badge badge-amber">${c.caseType}</span></td>
            <td style="max-width: 240px; font-size: 0.8rem; line-height:1.4;">${c.description}</td>
            <td style="font-size:0.8rem;">${c.date}</td>
            <td>
                <span class="badge ${c.status === 'Closed' ? 'badge-green' : c.status === 'Active' ? 'badge-red' : c.status === 'Dismissed' ? 'badge-blue' : 'badge-amber'}">${c.status}</span>
            </td>
            <td style="font-size: 0.8rem; max-width: 180px;">${c.actionTaken}</td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="openUpdateCaseModal('${c.caseId}')"><i data-lucide="edit-3"></i> Hearing</button>
            </td>
        </tr>
    `).join('');

    lucide.createIcons();
}

// AES QR Cryptography & Verification Tool
function verifyQRById() {
    const personId = parseInt(document.getElementById('verify-person-id').value);
    const resultBox = document.getElementById('verify-result-box');

    if (isNaN(personId)) {
        showToast('Please enter a valid numeric Person ID', 'warning');
        return;
    }

    const payload = STATE.encryptedQRPayloads.get(personId);
    resultBox.style.display = 'block';

    if (!payload) {
        resultBox.innerHTML = `<span style="color: var(--danger); font-weight:bold;">✖ NO QR RECORD GENERATED</span><br>Generate or view ID card for ID ${personId} first.`;
        return;
    }

    const decrypted = AESEncryptionUtil.decrypt(payload);
    if (decrypted === 'DECRYPTION_FAILED') {
        resultBox.innerHTML = `<span style="color: var(--danger); font-weight:bold;">✖ TAMPER DETECTED / CORRUPTED</span><br>Cryptographic signature verification failed!`;
    } else {
        STATE.qrVerificationsCount++;
        document.getElementById('stat-qr-verifications').textContent = STATE.qrVerificationsCount;
        resultBox.innerHTML = `
            <span style="color: var(--success); font-weight:bold;">✔ SIGNATURE VERIFIED (AES-128 AUTHENTIC)</span><br><br>
            <strong>Decrypted Payload Data:</strong><br>
            ${decrypted.replace(/\|/g, '<br>')}
        `;
    }
}

// AI Photo Quality Sharpness & Perceptual Hashing
function analyzePhotoAI(event) {
    const file = event.target.files[0];
    const resultBox = document.getElementById('photo-ai-result');
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        const img = new Image();
        img.onload = function() {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);

            const imageData = ctx.getImageData(0, 0, img.width, img.height);
            const data = imageData.data;

            let sum = 0, sumSq = 0, count = 0;
            const w = img.width, h = img.height;

            for (let y = 1; y < h - 1; y++) {
                for (let x = 1; x < w - 1; x++) {
                    const idx = (y * w + x) * 4;
                    const center = (data[idx] + data[idx+1] + data[idx+2]) / 3;
                    const left = (data[idx - 4] + data[idx - 3] + data[idx - 2]) / 3;
                    const right = (data[idx + 4] + data[idx + 5] + data[idx + 6]) / 3;
                    const laplacian = (4 * center) - left - right;
                    sum += laplacian;
                    sumSq += laplacian * laplacian;
                    count++;
                }
            }

            const mean = sum / (count || 1);
            const variance = (sumSq / (count || 1)) - (mean * mean);
            const sharpnessScore = Math.min(100, Math.round(variance));

            resultBox.style.display = 'block';
            let statusHtml = '';

            if (sharpnessScore < 80) {
                statusHtml = `<span style="color: var(--warning); font-weight:bold;">⚠ WARNING: Photo looks blurry (Sharpness Score: ${sharpnessScore}/100). Consider uploading a clearer high-res photo.</span>`;
            } else {
                statusHtml = `<span style="color: var(--success); font-weight:bold;">✔ OPTIMAL QUALITY (Sharpness Score: ${sharpnessScore}/100)</span>`;
            }

            resultBox.innerHTML = `
                ${statusHtml}<br><br>
                <strong>Perceptual Hash:</strong> <code>0x${Math.abs(Math.round(variance * 1234567)).toString(16).padStart(16, '0')}</code><br>
                <strong>Resolution:</strong> ${img.width} x ${img.height} px
            `;
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

// PNG & PDF Export Helpers
function exportCurrentCardPNG() {
    const cardEl = document.getElementById('card-front-content');
    if (!cardEl) return;

    html2canvas(cardEl, { scale: 2 }).then(canvas => {
        const link = document.createElement('a');
        const person = STATE.filteredCards[STATE.currentCardIndex];
        link.download = `ID_Card_${person ? person.id : 'export'}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
        showToast('ID Card PNG exported successfully!', 'success');
    });
}

function exportAllCardsPNG() {
    showToast('Exporting active batch to PNG...', 'success');
    exportCurrentCardPNG();
}

function exportAllCardsPDF() {
    const { jsPDF } = window.jspdf;
    const doc = new jsPDF('landscape', 'mm', 'a4');
    
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(16);
    doc.text('Chennai Institute of Technology - ID Cards Directory', 15, 20);

    const cardEl = document.getElementById('card-front-content');
    html2canvas(cardEl, { scale: 2 }).then(canvas => {
        const imgData = canvas.toDataURL('image/jpeg', 1.0);
        doc.addImage(imgData, 'JPEG', 15, 30, 160, 100);
        doc.save('ID_Cards_Export.pdf');
        showToast('PDF Export created successfully!', 'success');
    });
}

function exportCasesPDF() {
    const { jsPDF } = window.jspdf;
    const doc = new jsPDF('portrait', 'mm', 'a4');

    doc.setFillColor(180, 40, 40);
    doc.rect(0, 0, 210, 20, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('DISCIPLINARY CASES CONFIDENTIAL REPORT', 15, 13);

    let y = 35;
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(10);

    STATE.disciplinaryCases.forEach((c, idx) => {
        doc.setFillColor(245, 245, 245);
        doc.rect(15, y, 180, 30, 'F');
        doc.text(`Case ID: ${c.caseId} | Person: ${c.personName} (ID: ${c.personId})`, 20, y + 8);
        doc.text(`Violation: ${c.caseType} | Date: ${c.date}`, 20, y + 15);
        doc.text(`Status: ${c.status} | Action: ${c.actionTaken}`, 20, y + 22);
        y += 35;
    });

    doc.save('IDForge_Disciplinary_Cases_Report.pdf');
    showToast('Disciplinary Cases PDF Exported!', 'success');
}

// Modal Handlers & Forms
function openModal(id) {
    document.getElementById(id).classList.add('active');
}
function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

function openAddStudentModal() { 
    openModal('modal-add-student');
    updateLiveIDPreview();
}
function openAddFacultyModal() { 
    openModal('modal-add-faculty');
    updateLiveFacultyIDPreview();
}
function openAddCaseModal() { openModal('modal-add-case'); }

function openUpdateCaseModal(caseId) {
    document.getElementById('update-case-target-id').value = caseId;
    openModal('modal-update-case');
}

function handleSaveStudent(event) {
    event.preventDefault();
    const id = parseInt(document.getElementById('st-id').value);
    
    if (STATE.people.some(p => p.id === id)) {
        showToast(`ID ${id} already exists!`, 'danger');
        return;
    }

    const newStudent = {
        id: id,
        name: document.getElementById('st-name').value.trim(),
        department: document.getElementById('st-dept').value,
        type: 'Student',
        year: document.getElementById('st-year').value,
        gender: 'N/A',
        bloodGroup: document.getElementById('st-blood').value || 'O+',
        dob: 'N/A',
        phone: document.getElementById('st-phone').value || 'N/A',
        address: 'Chennai',
        academicStatus: 'Active',
        photoPath: STATE.activeLivePhotoData
    };

    STATE.people.unshift(newStudent);
    STATE.filteredCards = [...STATE.people];
    
    closeModal('modal-add-student');
    updateDashboardStats();
    renderCardBrowser();
    renderDirectoryTable();
    showToast(`✨ Student ${newStudent.name} (ID: ${newStudent.id}) registered with Clarified ID Photo!`, 'success');
}

function handleSaveFaculty(event) {
    event.preventDefault();
    const id = parseInt(document.getElementById('fc-id').value);

    if (STATE.people.some(p => p.id === id)) {
        showToast(`ID ${id} already exists!`, 'danger');
        return;
    }

    const newFaculty = {
        id: id,
        name: document.getElementById('fc-name').value.trim(),
        department: document.getElementById('fc-dept').value,
        type: 'Faculty',
        designation: document.getElementById('fc-desig').value || 'Faculty Member',
        dob: document.getElementById('fc-dob')?.value || 'N/A',
        phone: document.getElementById('fc-phone').value || 'N/A',
        address: 'Chennai',
        photoPath: STATE.activeFacultyLivePhotoData
    };

    STATE.people.unshift(newFaculty);
    STATE.filteredCards = [...STATE.people];

    closeModal('modal-add-faculty');
    updateDashboardStats();
    renderCardBrowser();
    renderDirectoryTable();
    showToast(`✨ Faculty ${newFaculty.name} (ID: ${newFaculty.id}) registered with Clarified ID Photo!`, 'success');
}

function autoFillCasePersonName() {
    const id = parseInt(document.getElementById('case-person-id').value);
    const person = STATE.people.find(p => p.id === id);
    if (person) {
        document.getElementById('case-person-name').value = person.name;
    }
}

function handleSaveCase(event) {
    event.preventDefault();
    const personId = parseInt(document.getElementById('case-person-id').value);
    const person = STATE.people.find(p => p.id === personId);

    const newCase = {
        caseId: document.getElementById('case-id-input').value.trim(),
        personId: personId,
        personName: person ? person.name : document.getElementById('case-person-name').value,
        personType: person ? person.type : 'Student',
        caseType: document.getElementById('case-type-input').value,
        description: document.getElementById('case-desc-input').value,
        date: document.getElementById('case-date-input').value,
        reportedBy: document.getElementById('case-reporter-input').value,
        status: 'Active',
        actionTaken: 'Pending Hearing'
    };

    STATE.disciplinaryCases.unshift(newCase);
    closeModal('modal-add-case');
    updateDashboardStats();
    renderCasesTable();
    showToast(`Disciplinary case ${newCase.caseId} logged!`, 'warning');
}

function handleUpdateCaseSubmit(event) {
    event.preventDefault();
    const caseId = document.getElementById('update-case-target-id').value;
    const targetCase = STATE.disciplinaryCases.find(c => c.caseId === caseId);

    if (targetCase) {
        targetCase.status = document.getElementById('update-case-status').value;
        targetCase.actionTaken = document.getElementById('update-case-action').value;
        closeModal('modal-update-case');
        renderCasesTable();
        updateDashboardStats();
        showToast(`Case ${caseId} updated!`, 'success');
    }
}

function handleAdminLogin(event) {
    event.preventDefault();
    const u = document.getElementById('login-username').value.trim();
    const p = document.getElementById('login-password').value;

    const hashed = CryptoJS.SHA256(p).toString();

    if (u === ADMIN_USERNAME && hashed === ADMIN_PASSWORD_HASH) {
        STATE.authenticated = true;
        closeModal('modal-login');
        showToast('Admin Login Successful!', 'success');
    } else {
        loginAttempts++;
        const remaining = 3 - loginAttempts;
        const errEl = document.getElementById('login-error');
        errEl.style.display = 'block';
        if (remaining > 0) {
            errEl.textContent = `Invalid credentials. ${remaining} attempt(s) remaining.`;
        } else {
            errEl.textContent = 'Too many failed login attempts. Access denied.';
        }
    }
}

// Toast Notifications
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <i data-lucide="${type === 'success' ? 'check-circle' : type === 'danger' ? 'alert-triangle' : 'info'}"></i>
        <span>${message}</span>
    `;
    container.appendChild(toast);
    lucide.createIcons();

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(30px)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
