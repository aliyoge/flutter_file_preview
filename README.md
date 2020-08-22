# flutter_file_preview

PDF, Word, Excel, and PowerPoint Viewer For Flutter App

## Getting Started

这是一个`Flutter`的Office文件预览插件。

支持同时支持本地和在线PDF、Word、Excel、PowerPoint等文件预览。

`Android`使用的是[TBS(腾讯浏览服务)](https://x5.tencent.com/tbs/guide/sdkInit.html)

`iOS`使用的是原生`WKWebView`

## 使用方法

pubspec.yaml中添加

```
file_preview:
    git:
        url: git://github.com/aliyoge/flutter_file_preview.git
```

在文件中使用

```
import 'package:flutter_file_preview/flutter_file_preview.dart';

# 预览在线文件
FlutterFilePreview.openFile("http://www.xxx.com/1245.pdf", title: 'Online PDF');

# 预览本地文件
FilePreview.openFile("/storage/emulated/0/Download/20180715.pdf", title: 'Local PDF');
```

## 常见问题

For help getting started with Flutter, view our online
[documentation](https://flutter.io/).

For help on editing plugin code, view the [documentation](https://flutter.io/platform-plugins/#edit-code).