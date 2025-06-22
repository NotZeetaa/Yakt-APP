#!/system/bin/sh

INFO_LOG="/data/data/com.notzeetaa.yakt/files/yakt.log"
ERROR_LOG="/data/data/com.notzeetaa.yakt/files/error.log"

# Function to append a message to the specified log file
log_message() {
    # shellcheck disable=SC3043
    local log_file="$1"
    # shellcheck disable=SC3043
    local message="$2"
    echo "$message" >> "$log_file"
}

# Function to log info messages
log_info() {
    log_message "$INFO_LOG" "$1"
}

# Function to write a value to a specified file
write_value() {
    # shellcheck disable=SC3043
    local file_path="$1"
    # shellcheck disable=SC3043
    local new_value="$2"
    local old_value
    local file_name=$(basename "$file_path")

    # Check if the file exists
    if [ ! -f "$file_path" ]; then
        log_error "File $file_name does not exist."
        return 1
    fi

    # Make the file writable
    chmod +w "$file_path" 2>/dev/null

    # Read current value
    old_value=$(cat "$file_path")

    # Write new value, log error if it fails
    if echo "$new_value" > "$file_path" 2>/dev/null; then
        log_info "$file_name: $old_value -> $new_value ✔"
    else
        log_info "$file_name: $old_value -> $new_value ✖"
    fi
}

MODE="$1"
PROFILE="$2"

if [ "$MODE" = "GPU" ] && [ "$CHECK" = "false" ]; then
    write_value "/sys/class/kgsl/kgsl-3d0/throttling" 0
    echo "[$(date "+%H:%M:%S")] GPU throttling disabled" >> $INFO_LOG
elif [ "$MODE" = "GPU" ] && [ "$CHECK" = "true" ]; then
    write_value "/sys/class/kgsl/kgsl-3d0/throttling" 1
    echo "[$(date "+%H:%M:%S")] GPU throttling enabled" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "google" ]; then
    settings put global private_dns_mode hostname
    settings put global private_dns_specifier dns.google
    echo "Switched to Google dns" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "cloudflare" ]; then
    settings put global private_dns_mode hostname
    settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com
    echo "Switched to CloudFlare dns" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "adguard" ]; then
    settings put global private_dns_mode hostname
    settings put global private_dns_specifier dns.adguard.com
    echo "Switched to Adguard dns" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "automatic" ]; then
    settings put global private_dns_mode opportunistic
    echo "Switched to Automatic dns" >> $INFO_LOG
elif [ "$MODE" = "DEX" ] && [ "$PROFILE" = "Speed" ]; then
    # shellcheck disable=SC2129
    echo "[$(date "+%H:%M:%S")] Dex optimization started (speed)" >> $INFO_LOG
    su -c 'pm compile -m speed -a' 2>> $INFO_LOG
    echo "[$(date "+%H:%M:%S")] Dex optimization finished" >> $INFO_LOG
elif [ "$MODE" = "DEX" ] && [ "$PROFILE" = "Extreme" ]; then
    # shellcheck disable=SC2129
    echo "[$(date "+%H:%M:%S")] Dex optimization started (extreme)" >> $INFO_LOG
    su -c 'pm compile -m everything -a' 2>> $INFO_LOG
    echo "[$(date "+%H:%M:%S")] Dex optimization finished" >> $INFO_LOG
elif [ "$MODE"  =  "oneplus" ] && [ "$PROFILE" = "true" ]; then
    pm disable-user --user 0 com.oplus.battery
    echo "[$(date "+%H:%M:%S")] Disabled OnePlus FPS cap" >> $INFO_LOG
elif [ "$MODE"  =  "oneplus" ] && [ "$PROFILE" = "false" ]; then
    pm enable com.oplus.battery
    echo "[$(date "+%H:%M:%S")] Enabled OnePlus FPS cap" >> $INFO_LOG
elif [ "$MODE"  =  "framerate" ] && [ "$PROFILE" = "true" ]; then
    su -c 'setprop debug.graphics.game_default_frame_rate.disabled 1'
    echo "[$(date "+%H:%M:%S")] Disabled the default framerate for games" >> $INFO_LOG
elif [ "$MODE"  =  "framerate" ] && [ "$PROFILE" = "false" ]; then
    su -c 'setprop debug.graphics.game_default_frame_rate.disabled 0'
    echo "[$(date "+%H:%M:%S")] Enabled the default framerate for games" >> $INFO_LOG
fi