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

# Function to log error messages
log_error() {
    log_message "$ERROR_LOG" "$1"
}

# Function to write a value to a specified file
write_value() {
    # shellcheck disable=SC3043
    local file_path="$1"
    # shellcheck disable=SC3043
    local new_value="$2"
    local old_value
    local file_name=$(basename "$file_path")
    local status_symbol status_text

    # Check if the file exists
    if [ ! -f "$file_path" ]; then
        log_error "$(printf "$STR_FILE_NOT_EXIST" "$file_name")"
        return 1
    fi

    # Make the file writable
    chmod +w "$file_path" 2>/dev/null

    # Read current value
    old_value=$(cat "$file_path")

    # Write new value, log error if it fails
    if echo "$new_value" > "$file_path" 2>/dev/null; then
        status_symbol="✔"
        status_text="$STR_SUCCESS"
    else
        status_symbol="✖"
        status_text="$STR_FAILED"
    fi

    log_info "$file_name: $old_value -> $new_value $status_symbol $status_text"
}

# Detect system language
LANG=$(getprop persist.sys.locale | cut -d'-' -f1)
case "$LANG" in
    "pt")  # Portuguese
        STR_GPU_DISABLED="GPU throttling desativado"
        STR_GPU_ENABLED="GPU throttling ativado"
        STR_DNS_GOOGLE="Mudou para DNS do Google"
        STR_DNS_CLOUDFLARE="Mudou para DNS do Cloudflare"
        STR_DNS_ADGUARD="Mudou para DNS do Adguard"
        STR_DNS_AUTOMATIC="Mudou para DNS automático"
        STR_DEX_SPEED="Otimização Dex iniciada (velocidade)"
        STR_DEX_EXTREME="Otimização Dex iniciada (extremo)"
        STR_DEX_FINISHED="Otimização Dex concluída"
        STR_OP_DISABLED="Desativado limite de FPS da OnePlus"
        STR_OP_ENABLED="Ativado limite de FPS da OnePlus"
        STR_FRAMERATE_DISABLED="Desativado o framerate padrão para jogos"
        STR_FRAMERATE_ENABLED="Ativado o framerate padrão para jogos"
        STR_FILE_NOT_EXIST="Arquivo %s não existe."
        STR_SUCCESS="(sucesso)"
        STR_FAILED="(falha)"
        ;;
    "ru")  # Russian
        STR_GPU_DISABLED="GPU throttling отключен"
        STR_GPU_ENABLED="GPU throttling включен"
        STR_DNS_GOOGLE="Переключено на Google DNS"
        STR_DNS_CLOUDFLARE="Переключено на Cloudflare DNS"
        STR_DNS_ADGUARD="Переключено на Adguard DNS"
        STR_DNS_AUTOMATIC="Переключено на автоматический DNS"
        STR_DEX_SPEED="Оптимизация Dex начата (скорость)"
        STR_DEX_EXTREME="Оптимизация Dex начата (экстремально)"
        STR_DEX_FINISHED="Оптимизация Dex завершена"
        STR_OP_DISABLED="Отключен FPS ограничение OnePlus"
        STR_OP_ENABLED="Включен FPS ограничение OnePlus"
        STR_FRAMERATE_DISABLED="Отключена стандартная частота кадров для игр"
        STR_FRAMERATE_ENABLED="Включена стандартная частота кадров для игр"
        STR_FILE_NOT_EXIST="Файл %s не существует."
        STR_SUCCESS="(успех)"
        STR_FAILED="(ошибка)"
        ;;
    *)    # Default to English
        STR_GPU_DISABLED="GPU throttling disabled"
        STR_GPU_ENABLED="GPU throttling enabled"
        STR_DNS_GOOGLE="Switched to Google dns"
        STR_DNS_CLOUDFLARE="Switched to CloudFlare dns"
        STR_DNS_ADGUARD="Switched to Adguard dns"
        STR_DNS_AUTOMATIC="Switched to Automatic dns"
        STR_DEX_SPEED="Dex optimization started (speed)"
        STR_DEX_EXTREME="Dex optimization started (extreme)"
        STR_DEX_FINISHED="Dex optimization finished"
        STR_OP_DISABLED="Disabled OnePlus FPS cap"
        STR_OP_ENABLED="Enabled OnePlus FPS cap"
        STR_FRAMERATE_DISABLED="Disabled the default framerate for games"
        STR_FRAMERATE_ENABLED="Enabled the default framerate for games"
        STR_FILE_NOT_EXIST="File %s does not exist."
        STR_SUCCESS="(success)"
        STR_FAILED="(failed)"
        ;;
esac

MODE="$1"
PROFILE="$2"

if [ "$MODE" = "GPU" ] && [ "$CHECK" = "false" ]; then
    write_value "/sys/class/kgsl/kgsl-3d0/throttling" 0
    echo "[$(date "+%H:%M:%S")] $STR_GPU_DISABLED" >> $INFO_LOG
elif [ "$MODE" = "GPU" ] && [ "$CHECK" = "true" ]; then
    write_value "/sys/class/kgsl/kgsl-3d0/throttling" 1
    echo "[$(date "+%H:%M:%S")] $STR_GPU_ENABLED" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "google" ]; then
    settings put global private_dns_mode hostname
    settings put global private_dns_specifier dns.google
    echo "[$(date "+%H:%M:%S")] $STR_DNS_GOOGLE" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "cloudflare" ]; then
    settings put global private_dns_mode hostname
    settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com
    echo "[$(date "+%H:%M:%S")] $STR_DNS_CLOUDFLARE" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "adguard" ]; then
    settings put global private_dns_mode hostname
    settings put global private_dns_specifier dns.adguard.com
    echo "[$(date "+%H:%M:%S")] $STR_DNS_ADGUARD" >> $INFO_LOG
elif [ "$MODE" = "DNS" ] && [ "$PROFILE" = "automatic" ]; then
    settings put global private_dns_mode opportunistic
    echo "[$(date "+%H:%M:%S")] $STR_DNS_AUTOMATIC" >> $INFO_LOG
elif [ "$MODE" = "DEX" ] && [ "$PROFILE" = "Speed" ]; then
    # shellcheck disable=SC2129
    echo "[$(date "+%H:%M:%S")] $STR_DEX_SPEED" >> $INFO_LOG
    su -c 'pm compile -m speed -a' 2>> $INFO_LOG
    echo "[$(date "+%H:%M:%S")] $STR_DEX_FINISHED" >> $INFO_LOG
elif [ "$MODE" = "DEX" ] && [ "$PROFILE" = "Extreme" ]; then
    # shellcheck disable=SC2129
    echo "[$(date "+%H:%M:%S")] $STR_DEX_EXTREME" >> $INFO_LOG
    su -c 'pm compile -m everything -a' 2>> $INFO_LOG
    echo "[$(date "+%H:%M:%S")] $STR_DEX_FINISHED" >> $INFO_LOG
elif [ "$MODE"  =  "oneplus" ] && [ "$PROFILE" = "true" ]; then
    pm disable-user --user 0 com.oplus.battery
    echo "[$(date "+%H:%M:%S")] $STR_OP_DISABLED" >> $INFO_LOG
elif [ "$MODE"  =  "oneplus" ] && [ "$PROFILE" = "false" ]; then
    pm enable com.oplus.battery
    echo "[$(date "+%H:%M:%S")] $STR_OP_ENABLED" >> $INFO_LOG
elif [ "$MODE"  =  "framerate" ] && [ "$PROFILE" = "true" ]; then
    su -c 'setprop debug.graphics.game_default_frame_rate.disabled 1'
    echo "[$(date "+%H:%M:%S")] $STR_FRAMERATE_DISABLED" >> $INFO_LOG
elif [ "$MODE"  =  "framerate" ] && [ "$PROFILE" = "false" ]; then
    su -c 'setprop debug.graphics.game_default_frame_rate.disabled 0'
    echo "[$(date "+%H:%M:%S")] $STR_FRAMERATE_ENABLED" >> $INFO_LOG
fi