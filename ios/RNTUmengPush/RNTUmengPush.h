#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import <UserNotifications/UserNotifications.h>

@interface RNTUmengPush : RCTEventEmitter <RCTBridgeModule, UNUserNotificationCenterDelegate>

+ (void)init:(NSString *)appKey channel:(NSString *)channel debug:(BOOL)debug;

+ (void)push:(NSDictionary *)launchOptions;

+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;
         
+ (void)didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler;

@end
