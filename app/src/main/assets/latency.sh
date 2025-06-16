#!/system/bin/sh

# ======================== Latency Profile ========================
# Log file location

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
    # shellcheck disable=SC3043
    local old_value
    # shellcheck disable=SC2155
    # shellcheck disable=SC3043
    # shellcheck disable=SC2046
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
    else
        status_symbol="✖"
    fi

    log_info " ○ $file_name: $old_value -> $new_value $status_symbol $status_text"
}

# Modify the filenames for logs
INFO_LOG="/data/data/com.notzeetaa.yakt/files/yakt.log"
ERROR_LOG="/data/data/com.notzeetaa.yakt/files/error.log"

# Prepare log files
:> "$INFO_LOG"
:> "$ERROR_LOG"

# Detect system language
LANG=$(getprop persist.sys.locale | cut -d'-' -f1)
case "$LANG" in
    "pt")  # Portuguese
        STR_EXECUTING="Executando perfil de Latência..."
        STR_EXECUTED="Perfil de Latência executado"
        STR_FILE_NOT_EXIST="Arquivo %s não existe."
        ;;
    "ru")  # Russian
        STR_EXECUTING="Выполнение профиля Latency..."
        STR_EXECUTED="Профиль Latency выполнен"
        STR_FILE_NOT_EXIST="Файл %s не существует."
        ;;
    *)    # Default to English
        STR_EXECUTING="Executing Latency profile..."
        STR_EXECUTED="Latency profile executed"
        STR_FILE_NOT_EXIST="File %s does not exist."
        ;;
esac

# Variables
MODULE_PATH="/sys/module"
KERNEL_PATH="/proc/sys/kernel"
MEMORY_PATH="/proc/sys/vm"
KGSL_PATH="/sys/class/kgsl/kgsl-3d0/"

echo -e "[$(date "+%H:%M:%S")] $STR_EXECUTING\n" > $INFO_LOG

write_value "$KERNEL_PATH/sched_autogroup_enabled" 1
write_value "$KERNEL_PATH/sched_child_runs_first" 1
write_value "$KERNEL_PATH/sched_nr_migrate" 4
write_value "$KERNEL_PATH/sched_migration_cost_ns" 5000000
write_value "$KERNEL_PATH/sched_tunable_scaling" 0
write_value "$KERNEL_PATH/perf_cpu_time_max_percent" 3
write_value "$KERNEL_PATH/sched_schedstats" 0
write_value "$MEMORY_PATH/vfs_cache_pressure" 80
write_value "$MEMORY_PATH/stat_interval" 10
write_value "$MEMORY_PATH/compaction_proactiveness" 0
write_value "$MEMORY_PATH/page-cluster" 0
write_value "$MEMORY_PATH/swappiness" 60
write_value "$MEMORY_PATH/dirty_ratio" 15
write_value "$MODULE_PATH/workqueue/parameters/power_efficient" Y
write_value "$KGSL_PATH/throttling" 1
write_value "$KERNEL_PATH/sched_schedstats" 0
write_value "$KERNEL_PATH/printk_devkmsg" off

echo -e "\n[$(date "+%H:%M:%S")] $STR_EXECUTED" >> $INFO_LOG