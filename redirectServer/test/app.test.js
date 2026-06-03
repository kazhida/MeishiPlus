const assert = require('node:assert/strict');
const { afterEach, describe, it } = require('node:test');

const {
  buildDeepLink,
  redirectToApp,
  serviceFromLocation,
} = require('../js/app.js');

const originalWindow = global.window;
const originalDocument = global.document;

afterEach(() => {
  global.window = originalWindow;
  global.document = originalDocument;
});

describe('serviceFromLocation', () => {
  it('uses the first path segment as the service', () => {
    const service = serviceFromLocation({
      pathname: '/instagram/callback',
      search: '?service=github',
    });

    assert.equal(service, 'instagram');
  });

  it('falls back to the service query parameter', () => {
    const service = serviceFromLocation({
      pathname: '/',
      search: '?service=qiiita',
    });

    assert.equal(service, 'qiita');
  });

  it('returns null when the service is unknown', () => {
    const service = serviceFromLocation({
      pathname: '/unknown',
      search: '?service=also-unknown',
    });

    assert.equal(service, null);
  });
});

describe('buildDeepLink', () => {
  it('keeps only OAuth result parameters', () => {
    const deepLink = buildDeepLink(
      'github',
      '?code=abc&state=return-here&ignored=value&error=',
    );

    assert.equal(deepLink, 'mspls://github?code=abc&state=return-here');
  });

  it('encodes forwarded parameter values', () => {
    const deepLink = buildDeepLink(
      'instagram',
      '?error=invalid_request&error_description=bad code',
    );

    assert.equal(
      deepLink,
      'mspls://instagram?error=invalid_request&error_description=bad+code',
    );
  });
});

describe('redirectToApp', () => {
  it('shows an error message when the service cannot be determined', () => {
    const { elements } = installBrowserMocks({
      pathname: '/unknown',
      search: '?code=abc',
    });

    redirectToApp();

    assert.equal(elements.statusMessage.textContent, '認証サービスを判定できませんでした。');
    assert.equal(elements.openAppLink.hidden, true);
  });

  it('shows an error message when OAuth result parameters are missing', () => {
    const { elements } = installBrowserMocks({
      pathname: '/github',
      search: '?state=only-state',
    });

    redirectToApp();

    assert.equal(elements.statusMessage.textContent, '認証結果が見つかりませんでした。');
    assert.equal(elements.openAppLink.hidden, true);
  });

  it('sets the app link and redirects to the deep link', () => {
    const { assignedUrls, elements } = installBrowserMocks({
      pathname: '/x',
      search: '?code=abc&state=xyz',
    });

    redirectToApp();

    assert.equal(elements.openAppLink.href, 'mspls://x?code=abc&state=xyz');
    assert.equal(elements.openAppLink.hidden, false);
    assert.deepEqual(assignedUrls, ['mspls://x?code=abc&state=xyz']);
  });
});

function installBrowserMocks({ pathname, search }) {
  const elements = {
    statusMessage: { textContent: '' },
    detailMessage: { textContent: '' },
    openAppLink: {
      hidden: true,
      href: '#',
    },
  };
  const assignedUrls = [];

  global.document = {
    getElementById(id) {
      const element = {
        'status-message': elements.statusMessage,
        'detail-message': elements.detailMessage,
        'open-app-link': elements.openAppLink,
      }[id];

      if (!element) {
        throw new Error(`Unexpected element id: ${id}`);
      }

      return element;
    },
  };

  global.window = {
    location: {
      pathname,
      search,
      assign(url) {
        assignedUrls.push(url);
      },
    },
    setTimeout(callback) {
      callback();
    },
  };

  return { assignedUrls, elements };
}
