//
//  FilePreviewVC.m
//  barcode_scan
//
//  Created by wenjunhuang on 2018/9/6.
//

#import "FilePreviewVC.h"

@interface FilePreviewVC ()
@property (nonatomic, strong) UIWebView *myWebView;
@end

@implementation FilePreviewVC

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setNeedsStatusBarAppearanceUpdate];
    UIBarButtonItem *closeItem = [[UIBarButtonItem alloc]initWithBarButtonSystemItem:UIBarButtonSystemItemStop target:self action:@selector(close)];
    self.navigationItem.rightBarButtonItem = closeItem;
    self.navigationController.navigationBar.backgroundColor = [UIColor whiteColor];
    self.navigationController.navigationBar.tintColor = [UIColor blackColor];
    self.myWebView = [[UIWebView alloc] initWithFrame: CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height)];
    self.myWebView.scalesPageToFit = YES;//使文档的显示范围适合UIWebView的bounds
    [self.view addSubview:self.myWebView];
}

- (void)viewWillAppear:(BOOL)animated {
    NSURL *filePath = [NSURL URLWithString:self.url];
    NSURLRequest *request = [NSURLRequest requestWithURL: filePath];
    [self.myWebView loadRequest:request];
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

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
