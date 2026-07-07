import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:face_aisdk_flutter_plugin/face_aisdk_flutter_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      // 1. 配置多语言支持
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('en', 'US'),
        Locale('zh', 'CN'),
      ],
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _resultDisplay = '';
  final String _testFaceId = "yourFaceID";

  // 简单的多语言映射
  String t(String key) {
    bool isZh = Localizations.localeOf(context).languageCode == 'zh';
    Map<String, Map<String, String>> localizedValues = {
      'title': {'en': 'FaceAISDK Demo', 'zh': 'FaceAISDK人脸识别演示'},
      'waiting': {'en': 'Waiting for operation...', 'zh': '等待操作...'},
      'result_empty': {'en': 'Result is empty', 'zh': '结果为空'},
      'btn_add_camera': {'en': 'Register Face via SDK Camera', 'zh': 'SDK相机录入人脸信息'},
      'btn_verify': {'en': 'Face Verify + Liveness', 'zh': '人脸识别+活体检测'},
      'btn_liveness': {'en': 'Liveness Detection Only', 'zh': '检测人脸是否活体'},
      'btn_query': {'en': 'Query Face Feature', 'zh': '查询人脸特征信息'},
      'btn_insert': {'en': 'Sync/Insert Face Feature', 'zh': '同步人脸特征信息'},
      'btn_add_image': {'en': 'Register Face via Image', 'zh': '人脸图录入人脸信息'},
      'btn_delete': {'en': 'Delete Face Feature', 'zh': '删除人脸特征信息'},
      'delete_done': {'en': 'Delete Completed', 'zh': '删除完成'},
    };
    return localizedValues[key]?[isZh ? 'zh' : 'en'] ?? key;
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_resultDisplay.isEmpty) {
      _resultDisplay = t('waiting');
    }
  }

  void _updateDisplay(FaceAiSdkResult result, {String? method}) {
    if (method == 'faceVerify' || method == 'livenessVerify') {
       _resultDisplay = "code: ${result.code}\n"
                        "msg: ${result.message}\n"
                        "${result.similarity != null ? 'similarity: ${result.similarity}\n' : ''}"
                        "liveness: ${result.livenessValue}\n"
                        "faceBase64: ${_truncate(result.faceBase64)}";
    } else {
       _resultDisplay = "code: ${result.code}\n"
                        "msg: ${result.message}\n"
                        "feature: ${_truncate(result.faceFeature)}\n"
                        "faceBase64: ${_truncate(result.faceBase64)}";
    }
    setState(() {});
  }

  String _truncate(dynamic value) {
    if (value is String && value.length > 50) {
      return "${value.substring(0, 20)}...${value.substring(value.length - 20)}";
    }
    return value?.toString() ?? "null";
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF1B4332),
        title: Text(t('title'), style: const TextStyle(color: Colors.white, fontSize: 18)),
        centerTitle: true,
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView(
              children: [
                _buildMenuButton(t('btn_add_camera'), () async {
                  final result = await FaceAiSdkFlutterPlugin.addFaceBySDKCamera(
                    faceId: _testFaceId,
                    addFacePerformanceMode: 1,
                    needShowConfirmDialog: true,
                  );
                  _updateDisplay(result);
                }),
                _buildMenuButton(t('btn_verify'), () async {
                  final result = await FaceAiSdkFlutterPlugin.faceVerify(
                    faceId: _testFaceId,
                    threshold: 0.84,
                    livenessType: 1,
                    motionLivenessTypes: "1,2,3,4,5",
                    motionLivenessTimeOut: 7,
                    motionLivenessSteps: 2,
                  );
                  _updateDisplay(result, method: 'faceVerify');
                }),
                _buildMenuButton(t('btn_liveness'), () async {
                  final result = await FaceAiSdkFlutterPlugin.livenessVerify(
                    livenessType: 2,
                    motionLivenessTypes: "1,2,3,4,5",
                    motionLivenessTimeOut: 7,
                    motionLivenessSteps: 2,
                    showResultTips: true,
                  );
                  _updateDisplay(result, method: 'livenessVerify');
                }),
                _buildMenuButton(t('btn_query'), () async {
                  final res = await FaceAiSdkFlutterPlugin.getFaceFeature(_testFaceId);
                  _updateDisplay(res);
                }),
                _buildMenuButton(t('btn_insert'), () async {
                  final res = await FaceAiSdkFlutterPlugin.insertFaceFeature(faceId: _testFaceId, feature: "...");
                  _updateDisplay(res);
                }),
                _buildMenuButton(t('btn_add_image'), () async {
                  final res = await FaceAiSdkFlutterPlugin.addFaceBySDKImage(faceId: _testFaceId, imageBase64: "...");
                  _updateDisplay(res);
                }),
                _buildMenuButton(t('btn_delete'), () async {
                  await FaceAiSdkFlutterPlugin.deleteFaceFeature(_testFaceId);
                  setState(() => _resultDisplay = t('delete_done'));
                }),
              ],
            ),
          ),
          
          // 结果展示区域
          Container(
            height: 200,
            width: double.infinity,
            margin: const EdgeInsets.all(12),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: const Color(0xFFF8F0FF),
              border: Border.all(color: Colors.purple, width: 1.5),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    child: Text(
                      _resultDisplay,
                      style: const TextStyle(color: Colors.purple, fontSize: 14, height: 1.5, fontFamily: 'monospace'),
                    ),
                  ),
                ),
                const Divider(color: Colors.purple),
                const Text('Email: FaceAISDK.Service@gmail.com', style: TextStyle(color: Colors.black54, fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMenuButton(String title, VoidCallback onPressed) {
    return Container(
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: Color(0xFFEEEEEE)))),
      child: ListTile(
        title: Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w400)),
        onTap: onPressed,
        tileColor: Colors.white,
      ),
    );
  }
}
