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
@property (nonatomic, strong) UIWebView *webView;
@end

@implementation FilePreviewVC

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setNeedsStatusBarAppearanceUpdate];
    UIBarButtonItem *closeItem = [[UIBarButtonItem alloc]initWithBarButtonSystemItem:UIBarButtonSystemItemStop target:self action:@selector(close)];
    self.navigationItem.rightBarButtonItem = closeItem;
    self.navigationController.navigationBar.backgroundColor = [UIColor whiteColor];
    self.navigationController.navigationBar.tintColor = [UIColor blackColor];
    ///用于适配文档文件
    _previewVC = [[QLPreviewController alloc]init];
    _previewVC.dataSource = self;
    _previewVC.view.frame = self.view.frame;
    
    ///用于适配音频文件
    _webView = [[UIWebView alloc] initWithFrame: CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height)];
    _webView.scalesPageToFit = YES;//使文档的显示范围适合UIWebView的bounds
    
    if ([self isAudioBy:self.url]) {
        [self.view addSubview:_webView];
    }else{
        [self addChildViewController:_previewVC];
        [self.view addSubview:_previewVC.view];
    }
}

- (void)viewWillAppear:(BOOL)animated {
    NSURL *filePath = [NSURL URLWithString:self.url];
    NSURLRequest *request = [NSURLRequest requestWithURL: filePath];
    if ([self isAudioBy:self.url]) {
        [_webView loadRequest:request];
    }
}

- (BOOL)isAudioBy:(NSString*) url {
    return [url containsString:@"mp3"]
    || [url containsString:@"MP3"]
    || [url containsString:@"mp4"]
    || [url containsString:@"MP4"]
    || [url containsString:@"wav"]
    || [url containsString:@"WAV"];
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
