// FuelLog Pro Firebase Web configuration — single source of truth.
// This browser API key is protected by Firebase Authentication and Security Rules.
export const firebaseConfig = {
  apiKey: "AIzaSyBkhbAr9HLXzUDvqFfiy9yPKP5DFKRawvI",
  authDomain: "fuellog-pro-f2a12.firebaseapp.com",
  projectId: "fuellog-pro-f2a12",
  storageBucket: "fuellog-pro-f2a12.firebasestorage.app",
  messagingSenderId: "7788280545",
  appId: "1:7788280545:web:24b930ab05f9564948d5a0"
};

if (typeof window !== 'undefined') window.FUELLOG_FIREBASE_CONFIG = firebaseConfig;
