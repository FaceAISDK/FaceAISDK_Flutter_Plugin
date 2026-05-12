import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:face_ai_sdk/face_ai_sdk.dart';

import '../theme/app_theme.dart';
import '../widgets/hero_section.dart';
import '../widgets/performance_card.dart';
import '../widgets/stats_grid.dart';
import '../widgets/feature_card.dart';
import '../widgets/use_case_section.dart';
import '../widgets/platforms_section.dart';
import '../widgets/sdk_modules_section.dart';
import '../widgets/recent_history_card.dart';
import '../widgets/sdk_update_card.dart';

class DashboardPage extends StatefulWidget {
  final void Function(int index)? onSwitchTab;

  const DashboardPage({super.key, this.onSwitchTab});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

enum _SdkStatus { loading, ready, error }

class _DashboardPageState extends State<DashboardPage> {
  final _sdk = FaceAiSdk();
  _SdkStatus _status = _SdkStatus.loading;
  String _sdkMessage = '';

  @override
  void initState() {
    super.initState();
    _initSdk();
  }

  Future<void> _initSdk() async {
    try {
      final result = await _sdk.initializeSDK({'apiKey': 'demo-key'});
      if (!mounted) return;
      setState(() {
        _sdkMessage = result;
        _status = _SdkStatus.ready;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _sdkMessage = e.toString();
        _status = _SdkStatus.error;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'FaceAI SDK',
          style: GoogleFonts.manrope(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: AppColors.onSurface,
          ),
        ),
        centerTitle: false,
        actions: [
          Container(
            margin: const EdgeInsets.only(right: 16),
            width: 32,
            height: 32,
            decoration: const BoxDecoration(
              color: AppColors.primaryContainer,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.bolt, color: Colors.white, size: 16),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 120),
        children: [
          // SDK init status banner
          _SdkStatusBanner(
            status: _status,
            message: _sdkMessage,
            onRetry: _initSdk,
          ),
          const SizedBox(height: 16),

          // Hero + Performance bento
          const HeroSection(),
          const SizedBox(height: 16),
          const PerformanceCard(),

          // Stats
          const SizedBox(height: 32),
          const StatsGrid(),

          // Core Capabilities
          const SizedBox(height: 40),
          Text(
            'SDK Core Capabilities',
            style: GoogleFonts.manrope(
              fontSize: 24,
              fontWeight: FontWeight.w700,
              color: AppColors.onSurface,
            ),
          ),
          const SizedBox(height: 16),
          FeatureCard(
            icon: Icons.face_unlock_rounded,
            title: 'Verify',
            description:
                '1:1 matching engine for identity confirmation. Compare live '
                'frames against stored biometric templates.',
            onTap: () => widget.onSwitchTab?.call(1),
          ),
          const SizedBox(height: 12),
          FeatureCard(
            icon: Icons.person_search_rounded,
            title: 'Search',
            description:
                '1:N recognition across large-scale datasets. Locate specific '
                'identities within millisecond response times.',
            onTap: () => widget.onSwitchTab?.call(2),
          ),
          const SizedBox(height: 12),
          FeatureCard(
            icon: Icons.verified_user_rounded,
            title: 'Liveness Detection',
            description:
                'Anti-spoofing technology to detect photos, masks, or digital '
                'replays. Ensures a human is physically present.',
            onTap: () async {
              await _sdk.initializeSDK({'apiKey': 'demo-key'});
              if (!mounted) return;
              await _sdk.startLiveness();
            },
          ),
          const SizedBox(height: 12),
          FeatureCard(
            icon: Icons.fingerprint_rounded,
            title: 'Data',
            description:
                'Enroll new identities into the biometric vault. Cleanse and '
                'optimize facial landmarks for better accuracy.',
            onTap: () => widget.onSwitchTab?.call(3),
          ),

          // Use Cases
          const SizedBox(height: 40),
          const UseCaseSection(),

          // Platforms
          const SizedBox(height: 40),
          const PlatformsSection(),

          // SDK Modules
          const SizedBox(height: 40),
          const SdkModulesSection(),

          // Bottom insights row
          const SizedBox(height: 16),
          const SdkUpdateCard(),
        ],
      ),
    );
  }
}

class _SdkStatusBanner extends StatelessWidget {
  final _SdkStatus status;
  final String message;
  final VoidCallback onRetry;

  const _SdkStatusBanner({
    required this.status,
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    final (Color bg, Color accent, IconData icon, String label) = switch (status) {
      _SdkStatus.loading => (
          AppColors.surfaceContainerLow,
          AppColors.primary,
          Icons.hourglass_top_rounded,
          'Initializing SDK...',
        ),
      _SdkStatus.ready => (
          AppColors.primary.withValues(alpha: 0.08),
          AppColors.primary,
          Icons.check_circle_rounded,
          'SDK Ready',
        ),
      _SdkStatus.error => (
          AppColors.error.withValues(alpha: 0.08),
          AppColors.error,
          Icons.error_outline_rounded,
          'SDK Init Failed',
        ),
    };

    return AnimatedContainer(
      duration: const Duration(milliseconds: 400),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          if (status == _SdkStatus.loading)
            SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                color: accent,
              ),
            )
          else
            Icon(icon, size: 20, color: accent),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: accent,
                  ),
                ),
                if (message.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    message,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.onSurfaceVariant,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ],
            ),
          ),
          if (status == _SdkStatus.error)
            GestureDetector(
              onTap: onRetry,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                decoration: BoxDecoration(
                  color: accent.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  'Retry',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: accent,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
