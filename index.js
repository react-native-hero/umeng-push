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
