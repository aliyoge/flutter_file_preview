//
//  FilePreviewVC.m
//  barcode_scan
//
//  Created by wenjunhuang on 2018/9/6.
//

#import "FilePreviewVC.h"
#import <QuickLook/QuickLook.h>

@interface FilePreviewVC ()<QLPreviewControllerDataSource>
@property (nonatomic, strong) QLPreviewController *previewVC;
@end

@implementation FilePreviewVC

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setNeedsStatusBarAppearanceUpdate];
    UIBarButtonItem *closeItem = [[UIBarButtonItem alloc]initWithBarButtonSystemItem:UIBarButtonSystemItemStop target:self action:@selector(close)];
    self.navigationItem.rightBarButtonItem = closeItem;
    self.navigationController.navigationBar.backgroundColor = [UIColor whiteColor];
    self.navigationController.navigationBar.tintColor = [UIColor blackColor];

    _previewVC = [[QLPreviewController alloc]init];
    _previewVC.dataSource = self;
    _previewVC.view.frame = self.view.frame;
    [self addChildViewController:_previewVC];
    [self.view addSubview:_previewVC.view];
}

- (void)close {
    [self dismissViewControllerAnimated:true completion:nil];
}

- (UIStatusBarStyle)preferredStatusBarStyle{
    if (@available(iOS 13.0, *)) {
    return UIStatusBarStyleDarkContent;
    } else {
    return UIStatusBarStyleDefault;
    }
}

- (void)setStatusBarBackgroundColor:(UIColor *)color {
    UIView *statusBar = [[[UIApplication sharedApplication] valueForKey:@"statusBarWindow"] valueForKey:@"statusBar"];

    if ([statusBar respondsToSelector:@selector(setBackgroundColor:)]) {
    statusBar.backgroundColor = color;
    }
}

- (NSInteger)numberOfPreviewItemsInPreviewController:(nonnull QLPreviewController *)controller {
    return 1;
}

- (nonnull id<QLPreviewItem>)previewController:(nonnull QLPreviewController *)controller previewItemAtIndex:(NSInteger)index {
    return [NSURL URLWithString:_url];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
