export function pad(num) {
    return num.toString().padStart(2, '0');
}

export class NervClock {
    constructor() {
        this.mode = 'normal';
        this.pomodoroDurations = [25 * 60 * 1000, 5 * 60 * 1000];
        this.pomodoroDurationIndex = 0;
        this.pomodoroTime = this.pomodoroDurations[0];
        this.pomodoroRemaining = this.pomodoroTime;
        this.isPaused = false;
        this.isDepleted = false;
        this.stopwatchTime = 0;
        this.lastDigits = {};
    }

    setMode(mode) {
        if (mode === 'stop' && this.mode !== 'normal') {
            this.isPaused = !this.isPaused;
            if (!this.isPaused) {
                this.lastUpdate = Date.now();
            }
            return;
        }

        if (mode === 'slow' && this.mode === 'slow') {
            this.pomodoroDurationIndex = (this.pomodoroDurationIndex + 1) % 2;
            this.pomodoroTime = this.pomodoroDurations[this.pomodoroDurationIndex];
            this.pomodoroRemaining = this.pomodoroTime;
            this.isDepleted = false;
            this.isPaused = false;
            return;
        }

        this.mode = mode;
        this.isPaused = false;
        this.isDepleted = false;

        if (mode === 'racing') {
            this.stopwatchTime = 0;
        } else if (mode === 'slow') {
            this.pomodoroDurationIndex = 0;
            this.pomodoroTime = this.pomodoroDurations[0];
            this.pomodoroRemaining = this.pomodoroTime;
        }
    }

    handleButton(btnMode) {
        if (btnMode === 'stop') {
            if (this.isPaused) {
                this.isPaused = false;
            } else {
                this.isPaused = true;
            }
        } else {
            this.setMode(btnMode);
        }
    }
}
