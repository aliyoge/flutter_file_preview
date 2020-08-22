#import <Flutter/Flutter.h>

@interface FlutterFilePreviewPlugin : NSObject<FlutterPlugin>
@property (nonatomic, assign) UIViewController *hostViewController;
@property (nonatomic, copy) NSString *backImgPath;
@end
