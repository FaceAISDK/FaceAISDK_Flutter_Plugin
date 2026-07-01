import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:face_aisdk_flutter_plugin/face_aisdk_flutter_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _resultDisplay = '等待操作...';
  final String _testFaceId = "yourFaceID";

  void _updateDisplay(Map? result, {String? method}) {
    if (result == null) {
      setState(() => _resultDisplay = "结果为空");
      return;
    }

    // 模拟 UTS 版本的显示逻辑
    if (method == 'faceVerify') {
       _resultDisplay = "code: ${result['code']}\n"
                        "msg: ${result['message'] ?? result['msg']}\n"
                        "similarity: ${result['similarity']}\n"
                        "liveness: ${result['livenessValue']}\n"
                        "faceBase64: ${_truncate(result['faceBase64'])}";
    } else if (method == 'livenessVerify') {
       _resultDisplay = "code: ${result['code']}\n"
                        "msg: ${result['message'] ?? result['msg']}\n"
                        "liveness: ${result['livenessValue']}\n"
                        "faceBase64: ${_truncate(result['faceBase64'])}";
    } else {
      // 通用显示
      Map<String, dynamic> displayMap = Map<String, dynamic>.from(result);
      displayMap.forEach((key, value) {
        if ((key == 'faceBase64' || key == 'faceFeature') && value is String && value.length > 50) {
          displayMap[key] = "${value.substring(0, 20)}...${value.substring(value.length - 20)}";
        }
      });
      _resultDisplay = const JsonEncoder.withIndent('  ').convert(displayMap);
    }
    
    setState(() {});
  }

  String _truncate(dynamic value) {
    if (value is String && value.length > 50) {
      return "${value.substring(0, 20)}...${value.substring(value.length - 20)}";
    }
    return value?.toString() ?? "null";
  }

  // 1. SDK 相机录入
  Future<void> _addFaceByCamera() async {
    final result = await FaceAiSdkFlutterPlugin.addFaceBySDKCamera(
      faceId: _testFaceId,
      addFacePerformanceMode: 1, // 1: 快速模式
      needShowConfirmDialog: true,
    );
    _updateDisplay(result);
  }

  // 2. 人脸识别 + 活体检测
  Future<void> _faceVerify() async {
    final result = await FaceAiSdkFlutterPlugin.faceVerify(
      faceId: _testFaceId,
      threshold: 0.84,
      livenessType: 1, // 1: 动作活体
      motionLivenessTypes: "1,2,3,4,5",
      motionLivenessTimeOut: 7,
      motionLivenessSteps: 2,
    );
    _updateDisplay(result, method: 'faceVerify');
  }

  // 3. 检测人脸是否活体
  Future<void> _livenessVerify() async {
    final result = await FaceAiSdkFlutterPlugin.livenessVerify(
      livenessType: 2, // 2: 动作+炫彩活体
      motionLivenessTypes: "1,2,3,4,5",
      motionLivenessTimeOut: 7,
      motionLivenessSteps: 2,
      showResultTips: true,
    );
    _updateDisplay(result, method: 'livenessVerify');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          backgroundColor: const Color(0xFF1B4332),
          title: const Text('FaceAISDK人脸识别演示', style: TextStyle(color: Colors.white, fontSize: 18)),
          centerTitle: true,
        ),
        body: Column(
          children: [
            Expanded(
              child: ListView(
                children: [
                  _buildMenuButton('SDK相机录入人脸信息', _addFaceByCamera),
                  _buildMenuButton('人脸识别+活体检测', _faceVerify),
                  _buildMenuButton('检测人脸是否活体', _livenessVerify),
                  _buildMenuButton('查询人脸特征信息', () async {
                    final res = await FaceAiSdkFlutterPlugin.getFaceFeature(_testFaceId);
                    _updateDisplay(res);
                  }),
                  _buildMenuButton('同步人脸特征信息', () async {
                    final res = await FaceAiSdkFlutterPlugin.insertFaceFeature(faceId: _testFaceId, feature: "...");
                    _updateDisplay(res);
                  }),
                  _buildMenuButton('人脸图录入人脸信息', () async {
                    final res = await FaceAiSdkFlutterPlugin.addFaceBySDKImage(faceId: _testFaceId, imageBase64: "...");
                    _updateDisplay(res);
                  }),
                  _buildMenuButton('删除人脸特征信息', () async {
                    await FaceAiSdkFlutterPlugin.deleteFaceFeature(_testFaceId);
                    setState(() => _resultDisplay = "删除完成");
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
