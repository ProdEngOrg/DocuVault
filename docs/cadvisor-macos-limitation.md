# cAdvisor Container Names — macOS/Docker Desktop Limitation

## Problem

The Grafana cAdvisor dashboard (`/d/pMEd7m0Mz/cadvisor-exporter`) showed no data in the **Container** dropdown and no graphs, despite cAdvisor running healthy and Prometheus successfully scraping it.

### Root Cause

The dashboard's template variable queried for the `name` label:
```promql
label_values({__name__=~"container.*", instance=~"$host"}, name)
```

On **macOS with Docker Desktop**, cAdvisor runs inside a lightweight Linux VM managed by Docker Desktop. From inside that VM, cAdvisor can only see aggregate cgroup paths (`/`, `/docker`, `/system.slice`, etc.) — it has **no visibility into individual Docker container cgroups** and therefore never emits a `name` label or any `container_label_*` labels.

This is a **known architectural limitation** of running cAdvisor on macOS. On a native Linux host, cAdvisor has direct cgroup access and populates `name` and Docker compose labels automatically.

---

## What Was Tried

| Attempt | Result |
|---------|--------|
| Add `--store_container_labels=true` flag to cAdvisor | No effect — Docker socket inside the VM has no container metadata |
| Add `--docker_only=true` flag | No effect — no per-container cgroup paths visible |
| Prometheus `metric_relabel_configs` to extract hash from `/docker/<hash>` id | No match — individual container paths (`/docker/<hash>`) are not exposed, only the aggregate `/docker` group |
| Switch dashboard variable to use `id` label | Works — dropdown populated with cgroup paths |
| Apply Grafana regex `/(.+)$` to strip path prefix | Grafana reported regex as invalid |

---

## Final State

The dashboard was updated to use the `id` label throughout (variable query, panel filters, and legend formats). The Container dropdown now shows cgroup path names like:

- `/` — host root
- `/docker` — all Docker containers aggregate
- `/system.slice` — Linux system services
- `/init.scope`, `/user.slice` — other cgroups

These are **not human-readable Docker container names** but are the only identifiers available on macOS. Selecting `/docker` in the dropdown will show aggregate metrics across all running Docker containers.

---

## Proper Fix (Linux Only)

On a Linux host (e.g. CI server, VM, or cloud instance), cAdvisor automatically exposes:
- `name` label = Docker container name (e.g. `docuvault-prod-eng-1`)
- `container_label_com_docker_compose_service` = compose service name (e.g. `prod-eng`)

No dashboard changes would be needed on Linux — the original `name`-based queries work correctly there.

---

## Files Changed

- `infrastructure/grafana/dashboards/containers/cadvisor.json` — switched all queries and the variable from `name` to `id`
- `docker-compose.yml` — added `--docker_only=true` and `--store_container_labels=true` flags to cAdvisor (harmless, no effect on macOS)
- `infrastructure/prometheus/prometheus.yml` — added `metric_relabel_configs` for cAdvisor (regex never matches on macOS, harmless)
