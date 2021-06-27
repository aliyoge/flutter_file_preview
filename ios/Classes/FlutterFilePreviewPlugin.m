#import "FlutterFilePreviewPlugin.h"
#import "FilePreviewVC.h"

@implementation FlutterFilePreviewPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
                                   methodChannelWithName:@"flutter_file_preview"
                                   binaryMessenger:[registrar messenger]];
  FlutterFilePreviewPlugin* instance = [[FlutterFilePreviewPlugin alloc] init];
  instance.hostViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
  NSString *backImgKey = [registrar lookupKeyForAsset:@"assets/images/ic_back.png" fromPackage:@"flutter_file_preview"];
  NSString *backImgPath = [[NSBundle mainBundle] pathForResource:backImgKey ofType:nil];
  instance.backImgPath = backImgPath;
  [registrar addMethodCallDelegate:instance channel:channel];
 
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"openFile" isEqualToString:call.method]) {
    NSString *path = call.arguments[@"path"];
    NSString *title = call.arguments[@"title"];
    FilePreviewVC *preview = [[FilePreviewVC alloc] init];
    preview.backImgPath = self.backImgPath;
    preview.url = path;
    preview.title = title != NULL ? title : @"文件预览";
    UINavigationController *navCtrl = [[UINavigationController alloc] initWithRootViewController:preview];
    if (self.hostViewController == NULL) {
      self.hostViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
    }
    [navCtrl setModalPresentationStyle:UIModalPresentationFullScreen];
    [self.hostViewController presentViewController:navCtrl animated:YES completion:nil];
  } else if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  } else {
    result(FlutterMethodNotImplemented);
  }
}

@end
