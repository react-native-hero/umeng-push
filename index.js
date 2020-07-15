import { NativeEventEmitter, NativeModules } from 'react-native'

const { RNTUmengPush } = NativeModules

const eventEmitter = new NativeEventEmitter(RNTUmengPush)

export const ALIAS_TYPE = {
  SINA: ALIAS_TYPE_SINA,
  TENCENT: ALIAS_TYPE_TENCENT,
  QQ: ALIAS_TYPE_QQ,
  WEIXIN: ALIAS_TYPE_WEIXIN,
  BAIDU: ALIAS_TYPE_BAIDU,
  RENREN: ALIAS_TYPE_RENREN,
  KAIXIN: ALIAS_TYPE_KAIXIN,
  DOUBAN: ALIAS_TYPE_DOUBAN,
  FACEBOOK: ALIAS_TYPE_FACEBOOK,
  TWITTER: ALIAS_TYPE_TWITTER,
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
