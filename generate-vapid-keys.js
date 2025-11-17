/**
 * VAPID 키 생성 스크립트
 *
 * 필수: npm install web-push
 * 실행: node generate-vapid-keys.js
 */

try {
    const webpush = require('web-push');

    console.log('\n==========================================');
    console.log('   VAPID 키 생성 중...');
    console.log('==========================================\n');

    const vapidKeys = webpush.generateVAPIDKeys();

    console.log('✅ VAPID 키가 생성되었습니다!\n');
    console.log('📋 application.yml에 다음 내용을 복사하세요:\n');
    console.log('webpush:');
    console.log(`  public-key: ${vapidKeys.publicKey}`);
    console.log(`  private-key: ${vapidKeys.privateKey}`);
    console.log('  subject: mailto:admin@medicine-app.com');
    console.log('\n==========================================');
    console.log('⚠️  중요: 이 키는 안전하게 보관하세요!');
    console.log('==========================================\n');

} catch (error) {
    console.error('❌ 오류 발생:', error.message);
    console.log('\n💡 해결 방법:');
    console.log('1. npm install web-push');
    console.log('2. node generate-vapid-keys.js\n');
    console.log('또는 온라인에서 생성:');
    console.log('https://vapidkeys.com/\n');
}
