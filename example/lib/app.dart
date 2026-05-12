import 'package:flutter/material.dart';

import 'theme/app_theme.dart';
import 'navigation/main_shell.dart';

class FaceAiSdkApp extends StatelessWidget {
  const FaceAiSdkApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FaceAI SDK',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: const MainShell(),
    );
  }
}
