#!/system/bin/sh

# Configuration
GAME_LIST="/data/local/game_list.conf"
LOG_FILE="/data/data/com.notzeetaa.yakt/files/yakt.log"
SLEEP_INTERVAL=5
CURRENT_MODE="none"
LAST_GAME=""

# Performance and normal scripts (adjust these paths as needed)
P="/data/local/performance_script.sh"
N="/data/local/normal_script.sh"

# Check if game list exists
if [ ! -f "$GAME_LIST" ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: Game list not found at $GAME_LIST" >> "$LOG_FILE"
    exit 1
fi

# Initialize log
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Performance tweaker started" >> "$LOG_FILE"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Monitoring $(grep -v '^#' "$GAME_LIST" | wc -l) games" >> "$LOG_FILE"

# Kernel parameters
set_gaming_mode() {
    echo "performance" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor 2>/dev/null
    echo "performance" > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null
    echo "deadline" > /sys/block/sda/queue/scheduler 2>/dev/null
    echo "0" > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Applied gaming kernel parameters" >> "$LOG_FILE"
}

set_battery_mode() {
    echo "powersave" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null
    echo "powersave" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor 2>/dev/null
    echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null
    echo "cfq" > /sys/block/sda/queue/scheduler 2>/dev/null
    echo "1" > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Applied battery kernel parameters" >> "$LOG_FILE"
}

# Get foreground package using multiple methods
get_foreground_package() {
    local pkg=""

    # Method 1: dumpsys activity (most reliable)
    pkg=$(dumpsys activity activities 2>/dev/null | \
        grep -E "mResumedActivity|mFocusedActivity" | \
        head -1 | \
        grep -oE "[a-zA-Z0-9._]+/[a-zA-Z0-9._]+" | \
        cut -d'/' -f1)

    # Method 2: dumpsys window (alternative)
    if [ -z "$pkg" ]; then
        pkg=$(dumpsys window windows 2>/dev/null | \
            grep -E "mCurrentFocus|mFocusedApp" | \
            head -1 | \
            grep -oE "[a-zA-Z0-9._]+/[a-zA-Z0-9._]+" | \
            cut -d'/' -f1)
    fi

    echo "$pkg"
}

# Improved top detection - only detect if app is in foreground/visible state
is_game_running_top() {
    # Get the list of games from the config file
    local game_pattern=$(grep -v '^#' "$GAME_LIST" | tr '\n' '|' | sed 's/|$//')

    if [ -z "$game_pattern" ]; then
        return 1
    fi

    # Use top to check for running games, but only if they're likely to be in foreground
    # Filter for processes with higher CPU usage (indicating active use)
    if top -n 1 -b 2>/dev/null | grep -E "$game_pattern" | awk '$9 > 1.0 {print $NF}' | grep -q -E "$game_pattern"; then
        # Get the specific game name that was found with significant CPU usage
        LAST_GAME=$(top -n 1 -b 2>/dev/null | grep -E "$game_pattern" | awk '$9 > 1.0 {print $NF}' | head -1)
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Game detected via top (active): $LAST_GAME" >> "$LOG_FILE"
        return 0
    fi

    return 1
}

# Check if game is running using dumpsys
is_game_running_dumpsys() {
    local current_pkg=$(get_foreground_package)

    if [ -n "$current_pkg" ]; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Current foreground: $current_pkg" >> "$LOG_FILE"

        # Skip comments and empty lines in game list
        if grep -v '^#' "$GAME_LIST" | grep -qF "$current_pkg"; then
            LAST_GAME="$current_pkg"
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] Game detected via dumpsys: $current_pkg" >> "$LOG_FILE"
            return 0
        fi
    else
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] No foreground package detected via dumpsys" >> "$LOG_FILE"
    fi

    return 1
}

# Alternative method: check if game process is in foreground state
is_game_foreground_ps() {
    local game_pattern=$(grep -v '^#' "$GAME_LIST" | tr '\n' '|' | sed 's/|$//')

    if [ -z "$game_pattern" ]; then
        return 1
    fi

    # Check if any game process has foreground priority or is top activity
    # This uses ps to check process state and priority
    if ps -A -o pid,state,pri,name | grep -E "$game_pattern" | grep -E "R|S" | head -1 | grep -q -E "$game_pattern"; then
        LAST_GAME=$(ps -A -o pid,state,pri,name | grep -E "$game_pattern" | grep -E "R|S" | awk '{print $4}' | head -1)
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Game detected via ps (foreground state): $LAST_GAME" >> "$LOG_FILE"
        return 0
    fi

    return 1
}

# Combined game detection
is_game_running() {
    # First try dumpsys method (most accurate for foreground)
    if is_game_running_dumpsys; then
        return 0
    fi

    # Then try ps method (checks process state)
    if is_game_foreground_ps; then
        return 0
    fi

    # Finally try top method with CPU usage filter
    if is_game_running_top; then
        return 0
    fi

    return 1
}

# Debug function to see what's happening
debug_detection() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] === DEBUG ===" >> "$LOG_FILE"

    # Show current foreground
    local current_pkg=$(get_foreground_package)
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Foreground package: $current_pkg" >> "$LOG_FILE"

    # Show top processes
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Top processes:" >> "$LOG_FILE"
    top -n 1 -b 2>/dev/null | head -10 >> "$LOG_FILE"

    # Show game processes
    local game_pattern=$(grep -v '^#' "$GAME_LIST" | tr '\n' '|' | sed 's/|$//')
    if [ -n "$game_pattern" ]; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Game processes:" >> "$LOG_FILE"
        ps -A | grep -E "$game_pattern" >> "$LOG_FILE"
    fi
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] === END DEBUG ===" >> "$LOG_FILE"
}

# Main loop
while true; do
    # Uncomment for debugging if needed
    # debug_detection

    if is_game_running; then
        if [ "$CURRENT_MODE" != "gaming" ]; then
            set_gaming_mode
            if [ -f "$P" ]; then
                bash "$P"
            fi
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] Enabled GAMING mode for: $LAST_GAME" >> "$LOG_FILE"
            CURRENT_MODE="gaming"
        fi
    else
        if [ "$CURRENT_MODE" != "battery" ]; then
            set_battery_mode
            if [ -f "$N" ]; then
                bash "$N"
            fi
            [ -n "$LAST_GAME" ] && echo "[$(date '+%Y-%m-%d %H:%M:%S')] Game closed: $LAST_GAME" >> "$LOG_FILE"
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] Enabled BATTERY mode" >> "$LOG_FILE"
            CURRENT_MODE="battery"
            LAST_GAME=""
        fi
    fi
    sleep $SLEEP_INTERVAL
done