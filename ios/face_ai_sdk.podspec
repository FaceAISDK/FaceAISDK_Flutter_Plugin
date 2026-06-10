Pod::Spec.new do |s|
  s.name             = 'face_ai_sdk'
  s.version          = '0.0.2'
  s.summary          = 'FaceAI SDK Flutter Plugin for iOS'
  s.description      = <<-DESC
FaceAI SDK Flutter Plugin - Face verification, liveness detection, and face enrollment.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.resource_bundles = { 'face_ai_sdk' => ['Resources/**'] }
  s.dependency 'Flutter'
  s.dependency 'FaceAISDK_Core'
  s.platform = :ios, '15.5'
  s.static_framework = true

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386 arm64',
    'BUILD_LIBRARY_FOR_DISTRIBUTION' => 'NO',
    'SWIFT_EMIT_LOC_STRINGS' => 'NO'
  }
  s.user_target_xcconfig = {
    'BUILD_LIBRARY_FOR_DISTRIBUTION' => 'NO'
  }
  s.swift_version = '5.9'
end
