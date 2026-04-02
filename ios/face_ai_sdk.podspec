Pod::Spec.new do |s|
  s.name             = 'face_ai_sdk'
  s.version          = '0.0.1'
  s.summary          = 'FaceAI SDK Flutter Plugin for iOS'
  s.description      = <<-DESC
FaceAI SDK Flutter Plugin - Face verification, liveness detection, and face enrollment.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'FaceAISDK_Core'
  s.platform = :ios, '15.5'

  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
