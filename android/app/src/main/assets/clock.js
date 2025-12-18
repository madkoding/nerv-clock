// NERV Chronometer - Digital Clock with Multiple Modes
// Modes: NORMAL (current time), RACING (stopwatch), SLOW (25min pomodoro), STOP/PLAY (pause/resume)

class NervClock {
    constructor() {
        this.hour1 = document.getElementById('hour1');
        this.hour2 = document.getElementById('hour2');
        this.min1 = document.getElementById('min1');
        this.min2 = document.getElementById('min2');
        this.sec1 = document.getElementById('sec1');
        this.sec2 = document.getElementById('sec2');
        this.ms1 = document.getElementById('ms1');
        this.ms2 = document.getElementById('ms2');
        this.modeIndicator = document.getElementById('mode-indicator');
        
        this.mode = 'normal';
        this.buttons = document.querySelectorAll('.ctrl-btn');
        this.stopBtn = document.querySelector('.ctrl-btn[data-mode="stop"]');
        
        // Stopwatch state
        this.stopwatchTime = 0;
        this.lastUpdate = Date.now();
        
        // Pomodoro state (25 minutes or 5 minutes in ms)
        this.pomodoroDurations = [25 * 60 * 1000, 5 * 60 * 1000]; // 25min, 5min
        this.pomodoroDurationIndex = 0;
        this.pomodoroTime = this.pomodoroDurations[0];
        this.pomodoroRemaining = this.pomodoroTime;
        this.isDepleted = false;
        
        // Pause state
        this.isPaused = false;
        
        // Track last digit values for glow effect
        this.lastDigits = {};
        
        // Clock display element for color changes
        this.clockDisplay = null;
        
        this.init();
    }

    init() {
        this.clockDisplay = document.querySelector('.clock-display');
        this.updateButtonStates();
        this.update();
        // Update every 40ms for smooth milliseconds
        setInterval(() => this.update(), 40);
        
        this.buttons.forEach(btn => {
            btn.addEventListener('click', (e) => this.handleButton(e.target.dataset.mode));
        });
    }

    handleButton(btnMode) {
        if (btnMode === 'stop') {
            // Toggle pause/play (only works in RACING or SLOW modes)
            if (this.isPaused) {
                // Resume
                this.isPaused = false;
                this.lastUpdate = Date.now();
            } else {
                // Pause
                this.isPaused = true;
            }
            this.updateButtonStates();
        } else {
            this.setMode(btnMode);
        }
    }

    setMode(mode) {
        // Special case: 'stop' toggles pause/play
        if (mode === 'stop') {
            if (this.mode !== 'normal') { // Can't pause normal mode
                this.isPaused = !this.isPaused;
                if (!this.isPaused) {
                    this.lastUpdate = Date.now();
                }
            }
            this.updateButtonStates();
            return;
        }
        
        // Special case: 'slow' toggles between 25min and 5min if already in slow mode
        if (mode === 'slow' && this.mode === 'slow') {
            this.pomodoroDurationIndex = (this.pomodoroDurationIndex + 1) % 2;
            this.pomodoroTime = this.pomodoroDurations[this.pomodoroDurationIndex];
            this.pomodoroRemaining = this.pomodoroTime;
            this.isDepleted = false;
            this.isPaused = false;
            this.lastUpdate = Date.now();
            this.clearTimerState();
            this.updateButtonStates();
            return;
        }
        
        this.mode = mode;
        this.isPaused = false;
        this.isDepleted = false;
        this.lastUpdate = Date.now();
        this.clearTimerState();
        
        // Mode-specific initialization
        if (mode === 'racing') {
            this.stopwatchTime = 0;
        } else if (mode === 'slow') {
            this.pomodoroDurationIndex = 0;
            this.pomodoroTime = this.pomodoroDurations[0];
            this.pomodoroRemaining = this.pomodoroTime;
        }
        
        this.updateButtonStates();
    }
    
    clearTimerState() {
        // Remove color states and depleted message
        if (this.clockDisplay) {
            this.clockDisplay.classList.remove('warning', 'critical', 'depleted');
        }
        // Restore digit visibility
        const digitGroups = document.querySelectorAll('.digit-group, .colon');
        digitGroups.forEach(el => el.style.display = '');
        
        const depletedMsg = document.querySelector('.depleted-message');
        if (depletedMsg) {
            depletedMsg.remove();
        }
    }

    updateButtonStates() {
        const self = this;
        
        // Update button active states
        this.buttons.forEach(btn => {
            const btnMode = btn.dataset.mode;
            if (btnMode === 'stop') {
                btn.classList.toggle('active', self.isPaused);
            } else {
                btn.classList.toggle('active', btnMode === self.mode);
            }
        });

        // STOP button is disabled in NORMAL mode
        if (this.stopBtn) {
            if (this.mode === 'normal') {
                this.stopBtn.classList.add('disabled');
                this.stopBtn.textContent = 'STOP';
            } else {
                this.stopBtn.classList.remove('disabled');
                this.stopBtn.textContent = this.isPaused ? 'PLAY' : 'STOP';
            }
        }

        // Update mode indicator
        if (this.modeIndicator) {
            let displayText = this.mode.toUpperCase();
            if (this.mode === 'slow') {
                const mins = this.pomodoroTime / 60000;
                displayText = `SLOW ${mins}m`;
            }
            if (this.isPaused) displayText += ' [PAUSED]';
            this.modeIndicator.textContent = displayText;
            this.modeIndicator.className = 'mode-indicator ' + this.mode;
        }
    }

    update() {
        const now = Date.now();
        const delta = now - this.lastUpdate;
        this.lastUpdate = now;
        
        let h, m, s, ms;
        
        if (this.mode === 'normal') {
            // Current time mode
            const date = new Date();
            h = this.pad(date.getHours());
            m = this.pad(date.getMinutes());
            s = this.pad(date.getSeconds());
            ms = this.pad(Math.floor(date.getMilliseconds() / 10));
        } else if (this.mode === 'racing') {
            // Stopwatch mode
            if (!this.isPaused) {
                this.stopwatchTime += delta;
            }
            const total = this.stopwatchTime;
            const hours = Math.floor(total / 3600000) % 100;
            const minutes = Math.floor(total / 60000) % 60;
            const seconds = Math.floor(total / 1000) % 60;
            const centiseconds = Math.floor((total % 1000) / 10);
            
            h = this.pad(hours);
            m = this.pad(minutes);
            s = this.pad(seconds);
            ms = this.pad(centiseconds);
        } else if (this.mode === 'slow') {
            // Pomodoro countdown mode
            if (!this.isPaused && this.pomodoroRemaining > 0) {
                this.pomodoroRemaining -= delta;
                if (this.pomodoroRemaining < 0) this.pomodoroRemaining = 0;
            }
            
            const remaining = this.pomodoroRemaining;
            const minutes = Math.floor(remaining / 60000) % 60;
            const hours = Math.floor(remaining / 3600000) % 100;
            const seconds = Math.floor(remaining / 1000) % 60;
            const centiseconds = Math.floor((remaining % 1000) / 10);
            
            // Update color based on remaining time
            if (this.clockDisplay) {
                const totalMinutes = remaining / 60000;
                this.clockDisplay.classList.remove('warning', 'critical', 'depleted');
                
                if (remaining === 0) {
                    // Depleted state
                    if (!this.isDepleted) {
                        this.isDepleted = true;
                        this.showDepletedMessage();
                    }
                    this.clockDisplay.classList.add('depleted');
                    return; // Don't update digits, show message instead
                } else if (totalMinutes <= 1) {
                    this.clockDisplay.classList.add('critical');
                } else if (totalMinutes <= 4) {
                    this.clockDisplay.classList.add('warning');
                }
            }
            
            h = this.pad(hours);
            m = this.pad(minutes);
            s = this.pad(seconds);
            ms = this.pad(centiseconds);
        }
        
        this.display(h, m, s, ms);
    }
    
    showDepletedMessage() {
        // Hide clock digits
        const digitGroups = document.querySelectorAll('.digit-group, .colon');
        digitGroups.forEach(el => el.style.display = 'none');
        
        // Create depleted message with Evangelion style (jp, stripes, en)
        const msg = document.createElement('div');
        msg.className = 'depleted-message';
        msg.innerHTML = `
            <div class="depleted-stripe left"></div>
            <div class="depleted-content">
                <div class="depleted-jp">電力枯渇</div>
                <div class="depleted-en">DEPLETED</div>
            </div>
            <div class="depleted-stripe right"></div>
        `;
        
        if (this.clockDisplay) {
            this.clockDisplay.appendChild(msg);
        }
    }
    
    hideDepletedMessage() {
        const digitGroups = document.querySelectorAll('.digit-group, .colon');
        digitGroups.forEach(el => el.style.display = '');
        
        const msg = document.querySelector('.depleted-message');
        if (msg) msg.remove();
    }

    setDigit(el, value) {
        if (!el) return;
        
        const id = el.id;
        const oldValue = this.lastDigits[id];
        
        // Si el dígito cambió, mostrar el anterior como fantasma
        if (oldValue !== undefined && oldValue !== value) {
            const ghost = el.querySelector('.ghost');
            if (ghost) {
                // Mostrar el dígito anterior en el ghost
                ghost.textContent = oldValue;
                ghost.classList.remove('fade-out');
                // Forzar reflow para reiniciar animación
                void ghost.offsetWidth;
                ghost.classList.add('fade-out');
            }
        }
        
        // Actualizar el dígito actual (mantener el ghost, actualizar texto)
        const textNode = el.lastChild;
        if (textNode && textNode.nodeType === 3) {
            textNode.textContent = value;
        } else {
            el.appendChild(document.createTextNode(value));
        }
        
        this.lastDigits[id] = value;
    }

    display(h, m, s, ms) {
        this.setDigit(this.hour1, h[0]);
        this.setDigit(this.hour2, h[1]);
        this.setDigit(this.min1, m[0]);
        this.setDigit(this.min2, m[1]);
        this.setDigit(this.sec1, s[0]);
        this.setDigit(this.sec2, s[1]);
        this.setDigit(this.ms1, ms[0]);
        this.setDigit(this.ms2, ms[1]);
    }

    pad(num) {
        return num.toString().padStart(2, '0');
    }
}

// Close button handler (Tauri desktop only)
function setupCloseButton() {
    const closeBtn = document.getElementById('close-btn');
    if (!closeBtn) return;
    
    // Only show close button in Tauri (desktop apps)
    if (!window.__TAURI__) {
        return; // Don't show in web browser
    }
    
    // Show the button
    closeBtn.classList.add('visible');
    
    closeBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        // Try Tauri API first
        if (window.__TAURI__) {
            try {
                const { getCurrentWindow } = window.__TAURI__.window;
                await getCurrentWindow().close();
                return;
            } catch (err) {
                console.log('Tauri close failed:', err);
            }
        }
        
        // Fallback: try window.close() for browser/electron
        try {
            window.close();
        } catch (err) {
            console.log('Window close not available');
        }
    });
}

// Pin button handler (Tauri desktop only) - toggle always on top
function setupPinButton() {
    const pinBtn = document.getElementById('pin-btn');
    if (!pinBtn) return;
    
    // Only show pin button in Tauri (desktop apps)
    if (!window.__TAURI__) {
        return;
    }
    
    // Show the button
    pinBtn.classList.add('visible');
    
    // Start as pinned (always on top is default)
    pinBtn.classList.add('pinned');
    
    pinBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        if (window.__TAURI__) {
            try {
                const { getCurrentWindow } = window.__TAURI__.window;
                const win = getCurrentWindow();
                
                // Toggle always on top
                const isPinned = pinBtn.classList.contains('pinned');
                await win.setAlwaysOnTop(!isPinned);
                
                // Update button state
                pinBtn.classList.toggle('pinned');
                pinBtn.title = isPinned ? 'Pin on top' : 'Unpin';
            } catch (err) {
                console.log('Toggle always on top failed:', err);
            }
        }
    });
}

// Setup window dragging for Tauri desktop
function setupWindowDrag() {
    if (!window.__TAURI__) return;
    
    const container = document.querySelector('.nerv-container');
    if (!container) return;
    
    container.addEventListener('mousedown', async (e) => {
        // Don't drag if clicking on buttons or close button
        if (e.target.closest('.ctrl-btn') || 
            e.target.closest('.close-btn') || 
            e.target.closest('.pin-btn') ||
            e.target.closest('.control-bar')) {
            return;
        }
        
        // Only left mouse button
        if (e.button !== 0) return;
        
        try {
            const { getCurrentWindow } = window.__TAURI__.window;
            await getCurrentWindow().startDragging();
        } catch (err) {
            console.log('Drag failed:', err);
        }
    });
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.nervClock = new NervClock();
    setupCloseButton();
    setupPinButton();
    setupWindowDrag();
});

if (document.readyState !== 'loading') {
    window.nervClock = new NervClock();
    setupCloseButton();
    setupPinButton();
    setupWindowDrag();
}
