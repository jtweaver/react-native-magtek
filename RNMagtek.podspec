Pod::Spec.new do |s|
  s.name         = "RNMagtek"
  s.version      = "1.0.0"
  s.summary      = "React Native Magtek Card Reader component for iOS + Android"
  s.description  = <<-DESC
                  MagTek library for React Native
                   DESC
  s.homepage     = ""
  s.license      = "UNLICENSED"
  s.author       = { "author" => "luis@monoku.com" }
  s.homepage     = "https://github.com/monoku/react-native-magtek#readme"
  s.platform     = :ios, "8.0"
  s.source       = { :git => "https://github.com/monoku/react-native-magtek.git", :tag => "master" }
  s.source_files = "ios/Magtek/**/*.{h,m}", "libMTSCRA/*.h"
  s.private_header_files = "libMTSCRA/*.h"
  s.requires_arc = true
  s.ios.vendored_library = "ios/Magtek/libMTSCRA.a"
  s.libraries = "MTSCRA"

  s.dependency "React"
  #s.dependency "others"

end
