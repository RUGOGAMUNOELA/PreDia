// file:// and WebViewAssetLoader (appassets.androidplatform.net) cannot host a real SW — skip to avoid errors.
var __h = location.hostname || '';
if ('serviceWorker' in navigator && location.protocol !== 'file:' && __h !== 'appassets.androidplatform.net') {
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('./sw.js', { scope: './' }).catch(function () {})
  })
}