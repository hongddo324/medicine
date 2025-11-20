/**
 * Firebase Configuration
 *
 * Firebase Cloud Messaging(FCM)을 위한 설정 파일
 * VAPID 키는 Firebase 콘솔 > 프로젝트 설정 > Cloud Messaging > 웹 푸시 인증서에서 생성
 */

// Firebase SDK 설정
const firebaseConfig = {
    apiKey: "AIzaSyC4oP4zGjT1W5gVsWu0kyOoHVpbieOtVpM",
    authDomain: "mdedicine.firebaseapp.com",
    projectId: "mdedicine",
    storageBucket: "mdedicine.firebasestorage.app",
    messagingSenderId: "659585584078",
    appId: "1:659585584078:web:bf9f90d5a2fdcd42f5b99f",
    measurementId: "G-GETB4S7XM4"
};

// VAPID 키 (Web Push 인증서)
// TODO: Firebase 콘솔에서 VAPID 키를 생성하고 여기에 입력하세요
// 경로: Firebase Console > 프로젝트 설정 > Cloud Messaging > 웹 푸시 인증서 > 키 쌍 생성
const vapidKey = "BNqXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";  // TODO: 실제 VAPID 키로 교체

// Firebase Config 내보내기
window.FIREBASE_CONFIG = firebaseConfig;
window.VAPID_KEY = vapidKey;
