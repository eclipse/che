{{- define "keycloakAuthUrl" }}
  {{- if eq .Values.global.serverStrategy "default-host" }}
    {{- if .Values.global.tls.enabled }}
      {{- printf "https://%s/auth" .Values.global.ingressDomain }}
    {{- else }}
      {{- printf "http://%s/auth" .Values.global.ingressDomain }}
    {{- end }}
  {{- else if eq .Values.global.serverStrategy "single-host" }}
    {{- if .Values.global.tls.enabled }}
      {{- printf "https://che-%s.%s/auth" .Release.Namespace .Values.global.ingressDomain }}
    {{- else }}
      {{- printf "http:/che-%s./%s/auth" .Release.Namespace .Values.global.ingressDomain }}
    {{- end }}
  {{- else }}
    {{- if .Values.global.tls.enabled }}
      {{- printf "https://keycloak-%s.%s/auth" .Release.Namespace .Values.global.ingressDomain }}
    {{- else }}
      {{- printf "http://keycloak-%s.%s/auth" .Release.Namespace .Values.global.ingressDomain }}
    {{- end }}
  {{- end }}
{{- end }}
