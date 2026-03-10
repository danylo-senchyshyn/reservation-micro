#!/usr/bin/env bash
# collect-docker-stats.sh
#
# Samples Docker container resource usage (CPU %, memory, network I/O, block I/O)
# at a fixed interval and writes the results to a CSV file.
#
# Usage:
#   ./load-tests/collect-docker-stats.sh                        # default: 2 s interval
#   ./load-tests/collect-docker-stats.sh 5                      # 5 s interval
#   ./load-tests/collect-docker-stats.sh 2 results/stats.csv    # custom output file
#
# Press Ctrl+C to stop. The CSV can be opened in Excel / imported into Python for charts.

set -euo pipefail

INTERVAL="${1:-2}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT="${2:-$(dirname "$0")/results/docker-stats-${TIMESTAMP}.csv}"

CONTAINERS=(
  reservation-service
  payment-service
  notification-service
  user-service
  gateway-service
  kafka
  postgres
)

mkdir -p "$(dirname "$OUTPUT")"

# CSV header
echo "timestamp,container,cpu_pct,mem_usage_mb,mem_limit_mb,mem_pct,net_in_mb,net_out_mb,block_in_mb,block_out_mb" \
  > "$OUTPUT"

echo "Collecting Docker stats every ${INTERVAL}s → $OUTPUT"
echo "Press Ctrl+C to stop."

cleanup() {
  echo ""
  echo "Stopped. Output written to: $OUTPUT"
}
trap cleanup INT TERM

while true; do
  TS=$(date +%Y-%m-%dT%H:%M:%S)

  # docker stats --no-stream outputs one line per container
  # Format: NAME | CPU% | MEM USAGE / LIMIT | MEM% | NET I/O | BLOCK I/O
  docker stats --no-stream --format \
    "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" \
    "${CONTAINERS[@]}" 2>/dev/null \
  | while IFS=$'\t' read -r name cpu mem_usage mem_pct net_io block_io; do

      # Strip trailing % signs and convert memory / network units to MB
      cpu_val="${cpu//%/}"

      # mem_usage is like "123MiB / 1GiB" – extract numerator and unit
      mem_num=$(echo "$mem_usage" | awk '{print $1}' | sed 's/[A-Za-z]//g')
      mem_unit=$(echo "$mem_usage" | awk '{print $1}' | sed 's/[0-9.]//g')
      mem_lim_num=$(echo "$mem_usage" | awk '{print $3}' | sed 's/[A-Za-z]//g')
      mem_lim_unit=$(echo "$mem_usage" | awk '{print $3}' | sed 's/[0-9.]//g')

      to_mb() {
        local val=$1 unit=$2
        case "${unit^^}" in
          KIB|KB) echo "scale=2; $val / 1024"   | bc ;;
          MIB|MB) echo "$val" ;;
          GIB|GB) echo "scale=2; $val * 1024"   | bc ;;
          *)      echo "$val" ;;
        esac
      }

      mem_mb=$(to_mb "$mem_num"  "$mem_unit")
      lim_mb=$(to_mb "$mem_lim_num" "$mem_lim_unit")
      mem_pct_val="${mem_pct//%/}"

      # net_io: "1.2MB / 3.4MB"
      net_in_mb=$(echo "$net_io" | awk '{print $1}' | sed 's/[A-Za-z]//g')
      net_out_mb=$(echo "$net_io" | awk '{print $3}' | sed 's/[A-Za-z]//g')

      # block_io: "5.6MB / 7.8MB"
      blk_in_mb=$(echo "$block_io" | awk '{print $1}' | sed 's/[A-Za-z]//g')
      blk_out_mb=$(echo "$block_io" | awk '{print $3}' | sed 's/[A-Za-z]//g')

      echo "${TS},${name},${cpu_val},${mem_mb},${lim_mb},${mem_pct_val},${net_in_mb},${net_out_mb},${blk_in_mb},${blk_out_mb}" \
        >> "$OUTPUT"
    done

  sleep "$INTERVAL"
done
