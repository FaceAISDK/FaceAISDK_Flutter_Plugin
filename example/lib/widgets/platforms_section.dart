import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher.dart';

import '../theme/app_theme.dart';

class PlatformsSection extends StatelessWidget {
  const PlatformsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Platforms',
          style: GoogleFonts.manrope(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppColors.onSurface,
          ),
        ),
        const SizedBox(height: 16),
        _PlatformTile(
          icon: Icons.android_rounded,
          label: 'Android',
          url: 'https://github.com/FaceAISDK/FaceAISDK_Android',
          iconColor: const Color(0xFF3DDC84),
        ),
        const SizedBox(height: 12),
        _PlatformTile(
          icon: Icons.apple_rounded,
          label: 'iOS',
          url: 'https://github.com/FaceAISDK/FaceAISDK_iOS',
          iconColor: AppColors.onSurface,
        ),
      ],
    );
  }
}

class _PlatformTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final String url;
  final Color iconColor;

  const _PlatformTile({
    required this.icon,
    required this.label,
    required this.url,
    required this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surfaceContainerLow,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: () => launchUrl(
          Uri.parse(url),
          mode: LaunchMode.externalApplication,
        ),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: iconColor.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, size: 24, color: iconColor),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: GoogleFonts.manrope(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: AppColors.onSurface,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  url,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          const Icon(
            Icons.open_in_new_rounded,
            size: 18,
            color: AppColors.outline,
          ),
        ],
          ),
        ),
      ),
    );
  }
}
