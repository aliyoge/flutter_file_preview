import 'package:flutter/material.dart';
import 'package:flutter_file_preview/flutter_file_preview.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: new Column(
          children: <Widget>[
            new Container(
              height: 20,
            ),
            new ElevatedButton(
                child: new Text("Open Debug"),
                onPressed: () {
                  FlutterFilePreview.openDebug();
                }),
            new Container(
              height: 20,
            ),
            new ElevatedButton(
                child: new Text("Open Online Pdf"),
                onPressed: () {
                  FlutterFilePreview.openFile(
                      "https://gitee.com/kongkongss/flutter_file_preview/raw/master/test/docs/test_file_for.pdf",
                      title: 'Online PDF');
                }),
            new Container(
              height: 20,
            ),
            new ElevatedButton(
                child: new Text("Open Online Docx"),
                onPressed: () {
                  FlutterFilePreview.openFile(
                      "https://gitee.com/kongkongss/flutter_file_preview/raw/master/test/docs/test_file_for.docx",
                      title: 'Online Docx');
                }),
            new Container(
              height: 20,
            ),
            new ElevatedButton(
                child: new Text("Open Online Xls"),
                onPressed: () {
                  FlutterFilePreview.openFile(
                      "https://gitee.com/kongkongss/flutter_file_preview/raw/master/test/docs/test_file_for.xlsx",
                      title: 'Online Xls');
                }),
          ],
        ),
      ),
    );
  }
}
