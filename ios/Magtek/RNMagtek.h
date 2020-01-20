#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import <React/RCTEventEmitter.h>
#import "MTSCRA.h"

@interface RNMagtek : RCTEventEmitter <RCTBridgeModule, MTSCRAEventDelegate>

@property(nonatomic, strong) MTSCRA *lib;

- (void) connect;
- (void) disconnect;

- (void) devConnStatusChange;
- (void) onDeviceConnectionDidChange:(MTSCRADeviceType)deviceType connected:(BOOL)connected instance:(id)instance;

@end
