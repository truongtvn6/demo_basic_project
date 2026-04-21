import { initializeApp, type FirebaseApp } from 'firebase/app';
import { getMessaging, getToken, onMessage, type Messaging, type MessagePayload } from 'firebase/messaging';
import { api } from './api';

export interface EnablePushResult {
  token: string;
}

export type ForegroundMessageHandler = (payload: {
  title?: string;
  body?: string;
  data?: Record<string, string>;
}) => void;

const env = import.meta.env;

export function isFcmEnabled(): boolean {
  return (env.VITE_FCM_ENABLED ?? 'false') === 'true';
}

let firebaseApp: FirebaseApp | null = null;
let messaging: Messaging | null = null;

function firebaseConfig() {
  return {
    apiKey: env.VITE_FIREBASE_API_KEY,
    authDomain: env.VITE_FIREBASE_AUTH_DOMAIN,
    projectId: env.VITE_FIREBASE_PROJECT_ID,
    messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID,
    appId: env.VITE_FIREBASE_APP_ID,
  };
}

function ensureConfigured() {
  if (!isFcmEnabled()) {
    throw new Error('FCM is disabled: set VITE_FCM_ENABLED=true in frontend/.env');
  }
  const cfg = firebaseConfig();
  const missing = Object.entries(cfg)
    .filter(([, v]) => !v)
    .map(([k]) => k);
  if (missing.length > 0 || !env.VITE_FIREBASE_VAPID_KEY) {
    throw new Error(
      `Missing Firebase config: ${[...missing, !env.VITE_FIREBASE_VAPID_KEY ? 'vapidKey' : null]
        .filter(Boolean)
        .join(', ')}`,
    );
  }
}

function initFirebase(): Messaging {
  ensureConfigured();
  if (!firebaseApp) {
    firebaseApp = initializeApp(firebaseConfig());
  }
  if (!messaging) {
    messaging = getMessaging(firebaseApp);
  }
  return messaging;
}

export async function enablePushOnThisBrowser(): Promise<EnablePushResult> {
  if (!('serviceWorker' in navigator)) {
    throw new Error('Service workers not supported in this browser.');
  }
  if (!('Notification' in window)) {
    throw new Error('Notification API not supported in this browser.');
  }

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    throw new Error(`Notification permission not granted (${permission}).`);
  }

  const cfg = firebaseConfig();
  const swUrl = new URL('/firebase-messaging-sw.js', window.location.origin);
  swUrl.searchParams.set('apiKey', cfg.apiKey ?? '');
  swUrl.searchParams.set('authDomain', cfg.authDomain ?? '');
  swUrl.searchParams.set('projectId', cfg.projectId ?? '');
  swUrl.searchParams.set('messagingSenderId', cfg.messagingSenderId ?? '');
  swUrl.searchParams.set('appId', cfg.appId ?? '');

  const registration = await navigator.serviceWorker.register(swUrl.toString());
  await navigator.serviceWorker.ready;

  const m = initFirebase();
  const token = await getToken(m, {
    vapidKey: env.VITE_FIREBASE_VAPID_KEY,
    serviceWorkerRegistration: registration,
  });
  if (!token) {
    throw new Error('getToken returned empty token.');
  }

  await api.registry.register(token, navigator.userAgent);
  return { token };
}

export function onForegroundMessage(handler: ForegroundMessageHandler): () => void {
  if (!isFcmEnabled()) return () => {};
  try {
    const m = initFirebase();
    const unsub = onMessage(m, (payload: MessagePayload) => {
      handler({
        title: payload.notification?.title,
        body: payload.notification?.body,
        data: payload.data as Record<string, string> | undefined,
      });
    });
    return unsub;
  } catch {
    return () => {};
  }
}
