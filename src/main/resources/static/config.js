// ── App configuration ─────────────────────────────────────────────────────────
// Change apiBase to point at a remote server when deploying the SPA separately.
// Examples:
//   '/api'                          (same origin — default)
//   'http://localhost:8080/api'     (local backend on a different port)
//   'https://api.yourdomain.com/api' (production)

window.APP_CONFIG = {
  apiBase: '/api',
  stripePublishableKey: 'pk_test_...', // replace with your Stripe publishable key
}
