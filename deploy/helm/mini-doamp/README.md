# Mini DOAMP Helm Chart

> P0-f skeleton — packaging the monolith for K8s deployment. Real service split happens at P3-P4.

## Prerequisites

- Kubernetes 1.26+
- Helm 3.10+
- A pre-populated Secret named `mini-doamp-secrets` with keys `DB_PASSWORD`, `JWT_SECRET`, and optionally `RABBIT_PASSWORD`, `XXL_JOB_ACCESS_TOKEN`. P1 will switch this to External Secrets Operator backed by Vault.
- External MySQL 8 / Redis / RabbitMQ reachable from the cluster (use bitnami charts or a managed offering).

## Usage

```bash
# Create the required secret (dev example — use Vault in prod)
kubectl create secret generic mini-doamp-secrets \
  --from-literal=DB_PASSWORD=changeme \
  --from-literal=JWT_SECRET=$(openssl rand -hex 32)

# Install
helm install mini-doamp deploy/helm/mini-doamp \
  --set image.repository=ghcr.io/chlingyu/mini-doamp \
  --set image.tag=latest \
  --set database.host=mysql.data.svc.cluster.local \
  --set redis.host=redis-master.data.svc.cluster.local \
  --set rabbitmq.host=rabbitmq.msg.svc.cluster.local

# Lint locally
helm lint deploy/helm/mini-doamp

# Render templates
helm template mini-doamp deploy/helm/mini-doamp
```

## What's baked in

- Non-root container (`runAsUser=1000`, all caps dropped)
- Rolling update (`maxSurge=1, maxUnavailable=0`) for zero-downtime
- Liveness / readiness probes on Spring Boot Actuator
- HorizontalPodAutoscaler on CPU/memory (opt-out via `autoscaling.enabled=false`)
- Optional Prometheus Operator `ServiceMonitor` (`serviceMonitor.enabled=true`)
- Optional Ingress (`ingress.enabled=true`)
- ConfigMap checksum annotation triggers pod rollout when config changes
