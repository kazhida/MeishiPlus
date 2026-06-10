const SERVICES = new Map([
  ['facebook', 'facebook'],
  ['github', 'github'],
  ['instagram', 'instagram'],
  ['qiita', 'qiita'],
  ['qiiita', 'qiita'],
  ['x', 'x'],
]);

const PASSTHROUGH_PARAMS = [
  'code',
  'state',
  'error',
  'error_description',
  'error_reason',
];

function serviceFromLocation(location) {
  const pathService = location.pathname
    .split('/')
    .filter(Boolean)[0]
    ?.toLowerCase();
  const queryService = new URLSearchParams(location.search)
    .get('service')
    ?.toLowerCase();

  return SERVICES.get(pathService) || SERVICES.get(queryService) || null;
}

function buildDeepLink(service, search) {
  const sourceParams = new URLSearchParams(search);
  const targetParams = new URLSearchParams();

  PASSTHROUGH_PARAMS.forEach((name) => {
    const value = sourceParams.get(name);
    if (value !== null && value !== '') {
      targetParams.set(name, value);
    }
  });

  const queryString = targetParams.toString();
  return `mspls://${service}${queryString ? `?${queryString}` : ''}`;
}

function setMessage(status, detail = '') {
  document.getElementById('status-message').textContent = status;
  document.getElementById('detail-message').textContent = detail;
}

function redirectToApp() {
  const service = serviceFromLocation(window.location);
  const openAppLink = document.getElementById('open-app-link');

  if (!service) {
    setMessage(
      '認証サービスを判定できませんでした。',
      'URL は /github、/facebook、/instagram、/qiita、/x のいずれかで開いてください。',
    );
    return;
  }

  const hasResult = new URLSearchParams(window.location.search).has('code') ||
    new URLSearchParams(window.location.search).has('error');
  if (!hasResult) {
    setMessage(
      '認証結果が見つかりませんでした。',
      'OAuth プロバイダから code または error が返っているか確認してください。',
    );
    return;
  }

  const deepLink = buildDeepLink(service, window.location.search);
  openAppLink.href = deepLink;
  openAppLink.hidden = false;
  setMessage('名刺+ アプリを開いてください。', 'Facebook 内ブラウザでは自動起動が失敗することがあります。');
}

if (typeof window !== 'undefined') {
  redirectToApp();
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    buildDeepLink,
    redirectToApp,
    serviceFromLocation,
  };
}
