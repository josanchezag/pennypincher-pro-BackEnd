# Manifiestos de Kubernetes

Manifiestos para autoalojar PennyPincher Pro en un clúster compatible con CNCF, pensados para **k3s**
(por ejemplo, en Oracle Cloud Always Free). Una réplica por servicio, sin HPA. Adminer no se incluye:
es solo para desarrollo.

```
kubernetes/
├── 00-namespace.yaml              # namespace pennypincher-pro (prefijo 00- para aplicarse primero)
├── backend/                       # API: deployment, service, ingress, configmap y secret (plantilla)
├── frontend/                      # Flutter web servido por nginx: deployment, service, ingress
└── database/                      # PostgreSQL 16: deployment, service, PVC de 5Gi y secret (plantilla)
```

- Recursos por contenedor: `requests` 100m / 256Mi y `limits` 500m / 512Mi.
- Backend: probes en `/actuator/health/liveness` y `/actuator/health/readiness` (más un `startupProbe`
  para el arranque de la JVM), usuario no root (UID 10001) y sistema de archivos de solo lectura.
- Frontend: probes en `/healthz` (lo sirve el nginx de la imagen del frontend) y variable `API_ORIGIN`
  con el origen público de la API (`https://api.pennypincher.yourdomain.com`).
- Ingress: clase `nginx` con TLS emitido por cert-manager (`cert-manager.io/cluster-issuer: letsencrypt-prod`).
  Cambiar `pennypincher.yourdomain.com` y `api.pennypincher.yourdomain.com` por los dominios reales,
  y `CORS_ALLOWED_ORIGINS` y `GOOGLE_ALLOWED_REDIRECT_URIS` en `backend/configmap.yaml` por el dominio del frontend.

## Requisitos del clúster

- Un ingress controller nginx (`ingress-nginx`). En k3s se puede desactivar Traefik con `--disable traefik`.
- cert-manager con un `ClusterIssuer` llamado `letsencrypt-prod`.
- Un `StorageClass` por defecto (k3s trae `local-path`).
- Las imágenes `ghcr.io/josanchezag/pennypincher-pro-backend` y `-frontend` publicadas (si son privadas,
  agregar un `imagePullSecret`).

## Despliegue

Los archivos `secret.yaml` son **plantillas** con `<VALUE>`; nunca se suben valores reales. Los secretos
se crean directamente en el clúster:

```bash
kubectl apply -f infra/kubernetes/00-namespace.yaml

kubectl -n pennypincher-pro create secret generic pennypincher-db \
  --from-literal=POSTGRES_DB=pennypincher \
  --from-literal=POSTGRES_USER=pennypincher \
  --from-literal=POSTGRES_PASSWORD='<contraseña>'

kubectl -n pennypincher-pro create secret generic pennypincher-backend \
  --from-literal=GOOGLE_CLIENT_ID='<client-id>' \
  --from-literal=GOOGLE_CLIENT_SECRET='<client-secret>' \
  --from-literal=JWT_PRIVATE_KEY_PEM="$(base64 -w0 private.pem)" \
  --from-literal=JWT_PUBLIC_KEY_PEM="$(base64 -w0 public.pem)"

for dir in database backend frontend; do
  find infra/kubernetes/$dir -name '*.yaml' ! -name 'secret*.yaml' -exec kubectl apply -f {} \;
done

kubectl -n pennypincher-pro get pods
```

Si se prefieren archivos, copiar cada plantilla a `secret.local.yaml` (ignorado por git), completar los
valores y aplicarla en lugar de los comandos `create secret`.

## Validación

```bash
kubectl apply --dry-run=client -R -f infra/kubernetes/
kubeconform -strict -summary -ignore-filename-pattern README infra/kubernetes/
```

El workflow `.github/workflows/validate-k8s.yml` ejecuta ambas validaciones en cada push y pull request.
