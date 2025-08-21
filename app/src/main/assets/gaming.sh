#!/system/bin/sh

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
        log_info " ○ $file_name: $old_value -> $new_value ✔"
            else
        log_info " ○ $file_name: $old_value -> $new_value ✖"
    fi
}

get_cores() {
    grep -c ^processor /proc/cpuinfo
}

get_min_granularity() {
    local base=$1
    echo $(( base / $(get_cores) ))
}

get_wakeup_granularity() {
    get_min_granularity $1
}

# Modify the filenames for logs
INFO_LOG="/data/data/com.notzeetaa.yakt/files/yakt.log"
ERROR_LOG="/data/data/com.notzeetaa.yakt/files/error.log"

# Prepare log files
:> "$INFO_LOG"
:> "$ERROR_LOG"

# Variables
MODULE_PATH="/sys/module"
KERNEL_PATH="/proc/sys/kernel"
MEMORY_PATH="/proc/sys/vm"
KGSL_PATH="/sys/class/kgsl/kgsl-3d0/"

echo -e "[$(date "+%H:%M:%S")] Executing Gaming profile...\n" > $INFO_LOG

cores=$(get_cores)
write_value "$KERNEL_PATH/sched_autogroup_enabled" 0
write_value "$KERNEL_PATH/sched_child_runs_first" 1
write_value "$KERNEL_PATH/sched_nr_migrate" 128
write_value "$KERNEL_PATH/sched_migration_cost_ns" 5000000
write_value "$KERNEL_PATH/sched_min_granularity_ns" $(get_min_granularity 750000)
write_value "$KERNEL_PATH/sched_wakeup_granularity_ns" $(get_wakeup_granularity 1000000)
write_value "$KERNEL_PATH/sched_tunable_scaling" 0
write_value "$KERNEL_PATH/perf_cpu_time_max_percent" $((5 + cores/2))
write_value "$KERNEL_PATH/sched_schedstats" 0
write_value "$MEMORY_PATH/vfs_cache_pressure" 80
write_value "$MEMORY_PATH/stat_interval" 10
write_value "$MEMORY_PATH/compaction_proactiveness" 0
write_value "$MEMORY_PATH/page-cluster" 0
write_value "$MEMORY_PATH/swappiness" 100
write_value "$MEMORY_PATH/dirty_ratio" 80
write_value "$MODULE_PATH/workqueue/parameters/power_efficient" N
write_value "$KGSL_PATH/throttling" 1
write_value "$KERNEL_PATH/sched_schedstats" 0
write_value "$KERNEL_PATH/printk_devkmsg" off
write_value "$KGSL_PATH/force_no_nap" 1
write_value "$KGSL_PATH/bus_split" 1

echo -e "\n[$(date "+%H:%M:%S")] Gaming profile executed" >> $INFO_LOG