import { NativeEventEmitter, NativeModules } from 'react-native'

const { RNTUmengPush } = NativeModules

const eventEmitter = new NativeEventEmitter(RNTUmengPush)

// 初始化时配置的渠道
export const CHANNEL = RNTUmengPush.CHANNEL

export const ALIAS_TYPE = {
  SINA: 'sina',
  TENCENT: 'tencent',
  QQ: 'qq',
  WEIXIN: 'weixin',
  BAIDU: 'baidu',
  RENREN: 'renren',
  KAIXIN: 'kaixin',
  DOUBAN: 'douban',
  FACEBOOK: 'facebook',
  TWITTER: 'twitter',
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
