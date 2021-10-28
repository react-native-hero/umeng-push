#import "RNTUmengPush.h"

#import <UMCommon/UMConfigure.h>
#import <UMPush/UMessage.h>
#import <React/RCTConvert.h>

@implementation RNTUmengPush

static NSString *ALIAS_TYPE_SINA = @"sina";
static NSString *ALIAS_TYPE_TENCENT = @"tencent";
static NSString *ALIAS_TYPE_QQ = @"qq";
static NSString *ALIAS_TYPE_WEIXIN = @"weixin";
static NSString *ALIAS_TYPE_BAIDU = @"baidu";
static NSString *ALIAS_TYPE_RENREN = @"renren";
static NSString *ALIAS_TYPE_KAIXIN = @"kaixin";
static NSString *ALIAS_TYPE_DOUBAN = @"douban";
static NSString *ALIAS_TYPE_FACEBOOK = @"facebook";
static NSString *ALIAS_TYPE_TWITTER = @"twitter";

static RNTUmengPush *PUSH_INSTANCE = nil;
static NSDictionary *LAUNCH_OPTIONS = nil;

RCT_EXPORT_MODULE(RNTUmengPush);

+ (void)init:(NSString *)appKey channel:(NSString *)channel debug:(BOOL)debug launchOptions:(NSDictionary *)launchOptions {

    [UMConfigure initWithAppkey:appKey channel:channel];
    [UMConfigure setLogEnabled:debug];
    
    LAUNCH_OPTIONS = launchOptions;
    
}

+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {

    if (![deviceToken isKindOfClass:[NSData class]]) {
        return;
    }

    const unsigned *tokenBytes = (const unsigned *)[deviceToken bytes];
    NSString *hexToken = [NSString stringWithFormat:@"%08x%08x%08x%08x%08x%08x%08x%08x",
                          ntohl(tokenBytes[0]), ntohl(tokenBytes[1]), ntohl(tokenBytes[2]),
                          ntohl(tokenBytes[3]), ntohl(tokenBytes[4]), ntohl(tokenBytes[5]),
                          ntohl(tokenBytes[6]), ntohl(tokenBytes[7])];

    NSMutableDictionary *body;

    if ([LAUNCH_OPTIONS objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey]) {
        NSDictionary *userInfo = [LAUNCH_OPTIONS objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
        body = [RNTUmengPush getNotification:userInfo];
    }
    else {
        body = [[NSMutableDictionary alloc] init];
    }

    body[@"deviceToken"] = hexToken;

    if (PUSH_INSTANCE != nil) {
        [PUSH_INSTANCE sendEventWithName:@"register" body:body];
    }

}

+ (void)didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {

    NSMutableDictionary *body = [RNTUmengPush getNotification:userInfo];
    NSString *message = body[@"message"];

    // 静默推送会走进这里，不论是什么系统版本
    // 普通推送，要求系统版本低于 10 才执行

    if ([[[UIDevice currentDevice] systemVersion]intValue] < 10) {
        [UMessage didReceiveRemoteNotification:userInfo];

        if (PUSH_INSTANCE != nil) {
            if (message) {
                [PUSH_INSTANCE sendEventWithName:@"message" body:body];
            }
            else {
                [PUSH_INSTANCE sendEventWithName:@"remoteNotification" body:body];
            }
        }
    }
    // 系统版本 >= 10，只能是静默推送
    // 静默推送时，message 必然是字符串，但是必须为空，否则会变成通知
    else if (message != nil && message.length == 0) {
        if (PUSH_INSTANCE != nil) {
            [PUSH_INSTANCE sendEventWithName:@"message" body:body];
        }
    }

    completionHandler(UIBackgroundFetchResultNewData);

}

// 获取自定义参数
+ (NSDictionary *)getCustom:(NSDictionary *)userInfo {
    
    NSMutableDictionary *custom = [[NSMutableDictionary alloc] init];

    for (NSString *key in userInfo) {
        // d p aps 这三个是所有通知都带的字段
        if (![key isEqual: @"d"] && ![key isEqual: @"p"] && ![key isEqual: @"aps"]) {
            custom[key] = userInfo[key];
        }
    }

    return custom;
    
}

// 获取推送消息
+ (NSMutableDictionary *)getNotification:(NSDictionary *)userInfo {
    
    NSDictionary *custom = [RNTUmengPush getCustom:userInfo];

    NSMutableDictionary *resultDict = [[NSMutableDictionary alloc] init];
    resultDict[@"custom"] = custom;

    NSDictionary *apsDict = userInfo[@"aps"];

    int contentAvailable = 0;
    if ([apsDict objectForKey:@"content-available"]) {
        contentAvailable = [[NSString stringWithFormat:@"%@", apsDict[@"content-available"]] intValue];
    }

    // 静默推送
    if (contentAvailable == 1) {
        // alert 不是个对象，而是字符串，对标安卓的 custom 字段也是个字符串
        resultDict[@"message"] = apsDict[@"alert"] ?: @"";
    }
    // 普通推送
    else {
        NSDictionary *alertDict = apsDict[@"alert"];
        if (alertDict) {
            resultDict[@"notification"] = @{
                                  @"title": alertDict[@"title"] ?: @"",
                                  @"subTitle": alertDict[@"subtitle"] ?: @"",
                                  @"content": alertDict[@"body"] ?: @""
                              };
        }
        else {
            NSString *alertStr = userInfo[@"aps"][@"alert"];
            resultDict[@"notification"] = @{
                                  @"title": alertStr ?: @"",
                                  @"subTitle": @"",
                                  @"content": @""
                              };
        }
    }

    return resultDict;
    
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

- (dispatch_queue_t)methodQueue {
    return dispatch_queue_create("com.github.reactnativehero.umengpush", DISPATCH_QUEUE_SERIAL);
}

- (instancetype)init {
    if (self = [super init]) {
        if (PUSH_INSTANCE) {
            PUSH_INSTANCE = nil;
        }
        PUSH_INSTANCE = self;
    }
    return self;
}

- (void)dealloc {
    PUSH_INSTANCE = nil;
}

- (NSDictionary *)constantsToExport {
    return @{
        @"ALIAS_TYPE_SINA": ALIAS_TYPE_SINA,
        @"ALIAS_TYPE_TENCENT": ALIAS_TYPE_TENCENT,
        @"ALIAS_TYPE_QQ": ALIAS_TYPE_QQ,
        @"ALIAS_TYPE_WEIXIN": ALIAS_TYPE_WEIXIN,
        @"ALIAS_TYPE_BAIDU": ALIAS_TYPE_BAIDU,
        @"ALIAS_TYPE_RENREN": ALIAS_TYPE_RENREN,
        @"ALIAS_TYPE_KAIXIN": ALIAS_TYPE_KAIXIN,
        @"ALIAS_TYPE_DOUBAN": ALIAS_TYPE_DOUBAN,
        @"ALIAS_TYPE_FACEBOOK": ALIAS_TYPE_FACEBOOK,
        @"ALIAS_TYPE_TWITTER": ALIAS_TYPE_TWITTER,
    };
}

- (NSArray<NSString *> *)supportedEvents {
  return @[
      @"register",
      @"message",
      @"localNotification",
      @"remoteNotification",
  ];
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler API_AVAILABLE(ios(10.0)) {

    NSDictionary *userInfo = notification.request.content.userInfo;

    if ([notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        // 应用处于前台时的远程推送
        [UMessage didReceiveRemoteNotification:userInfo];
        NSMutableDictionary *body = [RNTUmengPush getNotification:userInfo];
        body[@"presented"] = @YES;
        // 如果静默推送带了 alert 字段，则会作为通知展现
        [self sendNotificationToJs:body];
    }
    else {
        // 应用处于前台时的本地推送接受
    }

    completionHandler(UNNotificationPresentationOptionSound|UNNotificationPresentationOptionBadge|UNNotificationPresentationOptionAlert);

}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void(^)(void))completionHandler API_AVAILABLE(ios(10.0)) {

    NSDictionary *userInfo = response.notification.request.content.userInfo;

    if ([response.notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        // 应用处于后台时的远程推送
        [UMessage didReceiveRemoteNotification:userInfo];
        NSMutableDictionary *body = [RNTUmengPush getNotification:userInfo];
        body[@"clicked"] = @YES;
        // 如果静默推送带了 alert 字段，则会作为通知展现
        [self sendNotificationToJs:body];
    }
    else {
        // 应用处于后台时的本地推送接受
    }

}

- (void)sendNotificationToJs:(NSDictionary *)body {

    NSString *eventName = @"remoteNotification";

    if ([body objectForKey:@"message"]) {
        eventName = @"message";
    }

    [self sendEventWithName:eventName body:body];

}

// 初始化
RCT_EXPORT_METHOD(init:(NSDictionary*)options) {
    [self setPushSetting:options];
}

// 获取 device token
RCT_EXPORT_METHOD(start) {

    // Push 组件基本功能配置
    UMessageRegisterEntity *entity = [[UMessageRegisterEntity alloc] init];

    // type 是对推送的几个参数的选择，可以选择一个或者多个
    // 默认是三个全部打开，即：声音，弹窗，角标
    entity.types = UMessageAuthorizationOptionBadge|UMessageAuthorizationOptionSound|UMessageAuthorizationOptionAlert;

    if (@available(iOS 10.0, *)) {
        [UNUserNotificationCenter currentNotificationCenter].delegate = self;
    }

    dispatch_async(dispatch_get_main_queue(), ^{

        [UMessage registerForRemoteNotificationsWithLaunchOptions:LAUNCH_OPTIONS Entity:entity completionHandler:^(BOOL granted, NSError * _Nullable error) {

            if (!granted) {
                [self sendEventWithName:@"register" body:@{
                    @"error": @"permissions is not granted."
                }];
            }
            else if (error != nil) {
                [self sendEventWithName:@"register" body:@{
                    @"error": error.localizedDescription
                }];
            }

        }];

    });

}

// 获取所有标签
RCT_EXPORT_METHOD(getTags:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    [UMessage getTags:^(NSSet * _Nonnull responseTags, NSInteger remain, NSError * _Nonnull error) {
        if (error) {
            NSString *msg = [self getErrorMessage:error];
            reject([NSString stringWithFormat:@"%ld", (long)error.code], msg, nil);
        }
        else {
            if ([responseTags isKindOfClass:[NSSet class]]) {
                NSArray *tags = responseTags.allObjects;
                resolve(@{
                    @"tags": tags,
                });
                return;
            }
            reject(@"-1", @"error", nil);
        }
    }];

}

// 添加标签
RCT_EXPORT_METHOD(addTags:(NSArray *)tags
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    [UMessage addTags:tags response:^(id  _Nonnull responseObject, NSInteger remain, NSError * _Nonnull error) {

        if (error) {
            NSString *msg = [self getErrorMessage:error];
            reject([NSString stringWithFormat:@"%ld", (long)error.code], msg, nil);
        }
        else {
            if ([responseObject isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = responseObject;
                if ([dict[@"success"] isEqualToString:@"ok"]) {
                    resolve(@{
                        @"remain": @(remain),
                    });
                    return;
                }
            }
            reject(@"-1", @"error", nil);
        }

    }];

}

// 删除标签
RCT_EXPORT_METHOD(removeTags:(NSArray *)tags
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    [UMessage deleteTags:tags response:^(id  _Nonnull responseObject, NSInteger remain, NSError * _Nonnull error) {

        if (error) {
            NSString *msg = [self getErrorMessage:error];
            reject([NSString stringWithFormat:@"%ld", (long)error.code], msg, nil);
        }
        else {
            if ([responseObject isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = responseObject;
                if ([dict[@"success"] isEqualToString:@"ok"]) {
                    resolve(@{
                        @"remain": @(remain),
                    });
                    return;
                }
            }
            reject(@"-1", @"error", nil);
        }

    }];

}

// 重置别名
RCT_EXPORT_METHOD(setAlias:(NSString *)alias
                  type:(NSString *)type
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    NSString *innerType = [self getAliasType:type];

    [UMessage setAlias:alias type:innerType response:^(id  _Nonnull responseObject, NSError * _Nonnull error) {

        if (error) {
            NSString *msg = [self getErrorMessage:error];
            reject([NSString stringWithFormat:@"%ld", (long)error.code], msg, nil);
        }
        else {
            if ([responseObject isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = responseObject;
                if ([dict[@"success"] isEqualToString:@"ok"]) {
                    resolve(@{});
                    return;
                }
            }
            reject(@"-1", @"error", nil);
        }

    }];

}

// 绑定别名
RCT_EXPORT_METHOD(addAlias:(NSString *)alias
                  type:(NSString *)type
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    NSString *innerType = [self getAliasType:type];

    [UMessage addAlias:alias type:innerType response:^(id  _Nonnull responseObject, NSError * _Nonnull error) {

        if (error) {
            NSString *msg = [self getErrorMessage:error];
            reject([NSString stringWithFormat:@"%ld", (long)error.code], msg, nil);
        }
        else {
            if ([responseObject isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = responseObject;
                if ([dict[@"success"] isEqualToString:@"ok"]) {
                    resolve(@{});
                    return;
                }
            }
            reject(@"-1", @"error", nil);
        }

    }];

}

// 移除别名
RCT_EXPORT_METHOD(removeAlias:(NSString *)alias
                  type:(NSString *)type
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    NSString *innerType = [self getAliasType:type];

    [UMessage removeAlias:alias type:innerType response:^(id  _Nonnull responseObject, NSError * _Nonnull error) {

        if (error) {
            NSString *msg = [self getErrorMessage:error];
            reject([NSString stringWithFormat:@"%ld", (long)error.code], msg, nil);
        }
        else {
            if ([responseObject isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = responseObject;
                if ([dict[@"success"] isEqualToString:@"ok"]) {
                    resolve(@{});
                    return;
                }
            }
            reject(@"-1", @"error", nil);
        }

    }];

}

// 高级设置
RCT_EXPORT_METHOD(setAdvanced:(NSDictionary*)options) {
    
    [self setPushSetting:options];

}

- (void)setPushSetting:(NSDictionary *)options {
    
    // 当应用在前台运行收到 Push 时是否弹出 Alert 框
    if ([options objectForKey:@"autoAlert"]) {
        [UMessage setAutoAlert:[RCTConvert BOOL:options[@"autoAlert"]]];
    }
    
    // 设置是否允许 SDK 自动清空角标，默认自动角标清零
    if ([options objectForKey:@"badgeClear"]) {
        [UMessage setBadgeClear:[RCTConvert BOOL:options[@"badgeClear"]]];
    }
    
}

- (NSString *)getAliasType:(NSString *)type {

    // 默认用外面传入的
    NSString *aliasType = type;

    // 新浪微博
    if ([type isEqualToString:ALIAS_TYPE_SINA]) {
        aliasType = kUMessageAliasTypeSina;
    }
    // 腾讯微博
    else if ([type isEqualToString:ALIAS_TYPE_TENCENT]) {
        aliasType = kUMessageAliasTypeTencent;
    }
    // QQ
    else if ([type isEqualToString:ALIAS_TYPE_QQ]) {
        aliasType = kUMessageAliasTypeQQ;
    }
    // 微信
    else if ([type isEqualToString:ALIAS_TYPE_WEIXIN]) {
        aliasType = kUMessageAliasTypeWeiXin;
    }
    // 百度
    else if ([type isEqualToString:ALIAS_TYPE_BAIDU]) {
        aliasType = kUMessageAliasTypeBaidu;
    }
    // 人人网
    else if ([type isEqualToString:ALIAS_TYPE_RENREN]) {
        aliasType = kUMessageAliasTypeRenRen;
    }
    // 开心网
    else if ([type isEqualToString:ALIAS_TYPE_KAIXIN]) {
        aliasType = kUMessageAliasTypeKaixin;
    }
    // 豆瓣
    else if ([type isEqualToString:ALIAS_TYPE_DOUBAN]) {
        aliasType = kUMessageAliasTypeDouban;
    }
    // facebook
    else if ([type isEqualToString:ALIAS_TYPE_FACEBOOK]) {
        aliasType = kUMessageAliasTypeFacebook;
    }
    // twitter
    else if ([type isEqualToString:ALIAS_TYPE_TWITTER]) {
        aliasType = kUMessageAliasTypeTwitter;
    }

    return aliasType;

}

- (NSString *)getErrorMessage:(NSError *)error {
    switch (error.code) {
        case kUMessageErrorUnknown:
            return @"未知错误";
            break;
        case kUMessageErrorResponseErr:
            return @"响应出错";
            break;
        case kUMessageErrorOperateErr:
            return @"操作失败";
            break;
        case kUMessageErrorParamErr:
            return @"参数非法";
            break;
        case kUMessageErrorDependsErr:
            return @"条件不足(如：还未获取device_token，添加tag是不成功的)";
            break;
        case kUMessageErrorServerSetErr:
            return @"服务器限定操作";
            break;
        default:
            break;
    }
    return error.localizedDescription;
}

@end
