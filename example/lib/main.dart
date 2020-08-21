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
            new FlatButton(
                child: new Text("Open Online Pdf"),
                onPressed: () {
                  FlutterFilePreview.openFile(
                      "http://www.wh.gov.cn/zwgk/szfgz/202003/P020191023546167721245.pdf");
                }),
            new Container(
              height: 20,
            ),
            new FlatButton(
                child: new Text("Open Online Docx"),
                onPressed: () {
                  FlutterFilePreview.openFile(
                      "http://www.wh.gov.cn/zwgk/szfxx/202003/P020191021421042777162.doc");
                }),
            new Container(
              height: 20,
            ),
            new FlatButton(
                child: new Text("Open Online Xls"),
                onPressed: () {
                  FlutterFilePreview.openFile(
                      "http://www.bjrbj.gov.cn/csibiz/home/static/articles/catalog_118000/2017-12-14/article_ff808081583de24e0160543be5a10261/ff808081583de24e0160543be5a10264.xls");
                }),
          ],
        ),
      ),
    );
  }
}
