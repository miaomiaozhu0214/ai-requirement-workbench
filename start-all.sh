#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5174}"
SERVER_ADDRESS="${SERVER_ADDRESS:-127.0.0.1}"
AI_PROVIDER="${AI_PROVIDER:-openai}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-ai_requirement_workbench}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
DB_URL="${DB_URL:-jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}}"

BACKEND_LOG="${ROOT_DIR}/backend.log"
FRONTEND_LOG="${ROOT_DIR}/frontend.log"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令：$1"
    exit 1
  fi
}

kill_port() {
  local port="$1"
  local pids
  pids="$(lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -z "${pids}" ]]; then
    echo "端口 ${port} 未被占用"
    return
  fi

  echo "端口 ${port} 已被占用，停止进程：${pids//$'\n'/ }"
  kill ${pids} 2>/dev/null || true
  sleep 2

  local remaining
  remaining="$(lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -n "${remaining}" ]]; then
    echo "进程未正常退出，强制停止：${remaining//$'\n'/ }"
    kill -9 ${remaining} 2>/dev/null || true
  fi
}

wait_for_tcp() {
  local host="$1"
  local port="$2"
  local name="$3"

  if ! command -v nc >/dev/null 2>&1; then
    echo "未找到 nc，等待 ${name} 3 秒"
    sleep 3
    return
  fi

  echo "等待 ${name} 就绪：${host}:${port}"
  for _ in {1..40}; do
    if nc -z "${host}" "${port}" >/dev/null 2>&1; then
      echo "${name} 已就绪"
      return
    fi
    sleep 1
  done

  echo "${name} 未在预期时间内就绪，请查看日志或 Docker 状态"
  exit 1
}

wait_for_http() {
  local url="$1"
  local name="$2"

  echo "等待 ${name} 就绪：${url}"
  for _ in {1..60}; do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "${name} 已就绪"
      return
    fi
    sleep 1
  done

  echo "${name} 未在预期时间内就绪"
  exit 1
}

require_command docker
require_command mvn
require_command npm
require_command lsof
require_command curl

echo "项目目录：${ROOT_DIR}"
echo "AI_PROVIDER=${AI_PROVIDER}"

echo "启动 PostgreSQL"
cd "${ROOT_DIR}"
docker compose up -d postgres
wait_for_tcp "${DB_HOST}" "${DB_PORT}" "PostgreSQL"

kill_port "${BACKEND_PORT}"
kill_port "${FRONTEND_PORT}"

echo "启动后端：http://${SERVER_ADDRESS}:${BACKEND_PORT}"
(
  cd "${ROOT_DIR}/backend"
  DB_URL="${DB_URL}" \
  DB_USERNAME="${DB_USERNAME}" \
  DB_PASSWORD="${DB_PASSWORD}" \
  SERVER_ADDRESS="${SERVER_ADDRESS}" \
  AI_PROVIDER="${AI_PROVIDER}" \
  nohup mvn spring-boot:run > "${BACKEND_LOG}" 2>&1 &
  echo $! > "${ROOT_DIR}/backend.pid"
)
wait_for_http "http://${SERVER_ADDRESS}:${BACKEND_PORT}/api/health" "后端服务"

echo "启动前端：http://${SERVER_ADDRESS}:${FRONTEND_PORT}"
(
  cd "${ROOT_DIR}/frontend"
  nohup npm run dev > "${FRONTEND_LOG}" 2>&1 &
  echo $! > "${ROOT_DIR}/frontend.pid"
)
wait_for_http "http://${SERVER_ADDRESS}:${FRONTEND_PORT}/" "前端服务"

echo
echo "全部服务已启动"
echo "前端访问：http://${SERVER_ADDRESS}:${FRONTEND_PORT}/"
echo "后端健康：http://${SERVER_ADDRESS}:${BACKEND_PORT}/api/health"
echo "后端日志：${BACKEND_LOG}"
echo "前端日志：${FRONTEND_LOG}"
echo
echo "切换 Mock 可这样启动："
echo "AI_PROVIDER=mock ./start-all.sh"
