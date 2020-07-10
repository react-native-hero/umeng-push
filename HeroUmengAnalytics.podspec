require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "HeroUmengPush"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platform     = :ios, "9.0"
  s.frameworks   = "UserNotifications"

  s.source       = { :git => "https://github.com/react-native-hero/umeng-push.git", :tag => "v#{s.version}" }
  s.source_files = "ios/**/*.{h,m}"

  s.dependency 'React'
  s.dependency 'UMCCommon'
  s.dependency 'UMCPush'
  s.dependency 'UMCSecurityPlugins'
end