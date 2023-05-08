import { NativeEventEmitter, NativeModules } from 'react-native'

const { RNTUmengPush } = NativeModules

const eventEmitter = new NativeEventEmitter(RNTUmengPush)

export const ALIAS_TYPE = {
  SINA: RNTUmengPush.ALIAS_TYPE_SINA,
  TENCENT: RNTUmengPush.ALIAS_TYPE_TENCENT,
  QQ: RNTUmengPush.ALIAS_TYPE_QQ,
  WEIXIN: RNTUmengPush.ALIAS_TYPE_WEIXIN,
  BAIDU: RNTUmengPush.ALIAS_TYPE_BAIDU,
  RENREN: RNTUmengPush.ALIAS_TYPE_RENREN,
  KAIXIN: RNTUmengPush.ALIAS_TYPE_KAIXIN,
  DOUBAN: RNTUmengPush.ALIAS_TYPE_DOUBAN,
  FACEBOOK: RNTUmengPush.ALIAS_TYPE_FACEBOOK,
  TWITTER: RNTUmengPush.ALIAS_TYPE_TWITTER,
}

export const NOTIFICATION_PLAY = {
  SERVER: RNTUmengPush.NOTIFICATION_PLAY_SERVER,
  SDK_ENABLE: RNTUmengPush.NOTIFICATION_PLAY_SDK_ENABLE,
  SDK_DISABLE: RNTUmengPush.NOTIFICATION_PLAY_SDK_DISABLE,
}

export function init(options) {
  RNTUmengPush.init(options || {})
}

export function start() {
  RNTUmengPush.start()
}

export function getTags() {
  return RNTUmengPush.getTags()
}

export function addTags(tags) {
  return RNTUmengPush.addTags(tags)
}

export function removeTags(tags) {
  return RNTUmengPush.removeTags(tags)
}

export function setAlias(alias, type) {
  return RNTUmengPush.setAlias(alias, type)
}

export function addAlias(alias, type) {
  return RNTUmengPush.addAlias(alias, type)
}

export function removeAlias(alias, type) {
  return RNTUmengPush.removeAlias(alias, type)
}

export function setAdvanced(options) {
  RNTUmengPush.setAdvanced(options)
}

export function addListener(type, listener) {
  return eventEmitter.addListener(type, listener)
}

// ios 不支持关闭推送
export const supportDisable = !!RNTUmengPush.disable

export function enable() {
  return RNTUmengPush.enable()
}

export function disable() {
  return RNTUmengPush.disable()
}

// ios 不支持推送通道
export const supportNotificationChannel = !!RNTUmengPush.createNotificationChannel

export function createNotificationChannel(options) {
  RNTUmengPush.createNotificationChannel(options)
}

export function deleteNotificationChannel(options) {
  RNTUmengPush.deleteNotificationChannel(options)
}
