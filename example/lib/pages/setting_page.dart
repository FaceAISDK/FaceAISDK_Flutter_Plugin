import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../theme/app_theme.dart';

class SettingPage extends StatefulWidget {
  const SettingPage({super.key});

  @override
  State<SettingPage> createState() => _SettingPageState();
}

class _SettingPageState extends State<SettingPage> {
  // System Camera
  bool _isFrontCamera = true;
  int _sysCameraDegree = 270;

  // UVC RGB Camera
  int _rgbUvcDegree = 0;
  bool _rgbUvcMirrorH = false;
  final String _rgbCameraName = 'Not selected';

  // UVC IR Camera
  int _irUvcDegree = 0;
  bool _irUvcMirrorH = false;
  final String _irCameraName = 'Not selected';

  void _toggleCamera() {
    setState(() => _isFrontCamera = !_isFrontCamera);
  }

  void _cycleSysDegree() {
    setState(() => _sysCameraDegree = (_sysCameraDegree + 90) % 360);
  }

  void _cycleRgbDegree() {
    setState(() => _rgbUvcDegree = (_rgbUvcDegree + 90) % 360);
  }

  void _cycleIrDegree() {
    setState(() => _irUvcDegree = (_irUvcDegree + 90) % 360);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Settings',
          style: GoogleFonts.manrope(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: AppColors.onSurface,
          ),
        ),
        centerTitle: false,
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 120),
        children: [
          // ── System Camera ──
          const _SectionHeader(title: 'System Camera'),
          const SizedBox(height: 12),
          _SettingTile(
            icon: Icons.flip_camera_android_rounded,
            title: 'Front / Back Camera',
            subtitle: _isFrontCamera ? 'Front camera' : 'Back / USB camera',
            trailing: _ChipValue(
              label: _isFrontCamera ? 'FRONT' : 'BACK',
              color: AppColors.primary,
            ),
            onTap: _toggleCamera,
          ),
          _SettingTile(
            icon: Icons.rotate_90_degrees_cw_rounded,
            title: 'Camera Rotation',
            subtitle: 'Rotate system camera preview',
            trailing: _ChipValue(
              label: '$_sysCameraDegree°',
              color: AppColors.primary,
            ),
            onTap: _cycleSysDegree,
          ),

          const SizedBox(height: 32),

          // ── UVC Camera (RGB) ──
          const _SectionHeader(title: 'UVC Camera · RGB'),
          const SizedBox(height: 12),
          _SettingTile(
            icon: Icons.rotate_90_degrees_cw_rounded,
            title: 'RGB Rotation',
            subtitle: 'Rotate RGB UVC camera preview',
            trailing: _ChipValue(
              label: '$_rgbUvcDegree°',
              color: AppColors.secondary,
            ),
            onTap: _cycleRgbDegree,
          ),
          _SettingTile(
            icon: Icons.flip_rounded,
            title: 'RGB Horizontal Mirror',
            subtitle: 'Flip RGB camera horizontally',
            trailing: Switch.adaptive(
              value: _rgbUvcMirrorH,
              onChanged: (v) => setState(() => _rgbUvcMirrorH = v),
              activeTrackColor: AppColors.primary,
            ),
            onTap: () => setState(() => _rgbUvcMirrorH = !_rgbUvcMirrorH),
          ),
          _SettingTile(
            icon: Icons.videocam_rounded,
            title: 'RGB Camera Select',
            subtitle: _rgbCameraName,
            onTap: () {
              // TODO: open device picker via plugin
            },
          ),

          const SizedBox(height: 32),

          // ── UVC Camera (IR) ──
          const _SectionHeader(title: 'UVC Camera · IR'),
          const SizedBox(height: 12),
          _SettingTile(
            icon: Icons.rotate_90_degrees_cw_rounded,
            title: 'IR Rotation',
            subtitle: 'Rotate IR UVC camera preview',
            trailing: _ChipValue(
              label: '$_irUvcDegree°',
              color: AppColors.tertiary,
            ),
            onTap: _cycleIrDegree,
          ),
          _SettingTile(
            icon: Icons.flip_rounded,
            title: 'IR Horizontal Mirror',
            subtitle: 'Flip IR camera horizontally',
            trailing: Switch.adaptive(
              value: _irUvcMirrorH,
              onChanged: (v) => setState(() => _irUvcMirrorH = v),
              activeTrackColor: AppColors.primary,
            ),
            onTap: () => setState(() => _irUvcMirrorH = !_irUvcMirrorH),
          ),
          _SettingTile(
            icon: Icons.videocam_rounded,
            title: 'IR Camera Select',
            subtitle: _irCameraName,
            onTap: () {
              // TODO: open device picker via plugin
            },
          ),
        ],
      ),
    );
  }
}

// ── Private widgets ──

class _SectionHeader extends StatelessWidget {
  final String title;

  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Text(
      title.toUpperCase(),
      style: GoogleFonts.inter(
        fontSize: 12,
        fontWeight: FontWeight.w600,
        letterSpacing: 1.2,
        color: AppColors.outline,
      ),
    );
  }
}

class _ChipValue extends StatelessWidget {
  final String label;
  final Color color;

  const _ChipValue({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        label,
        style: GoogleFonts.inter(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}

class _SettingTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final Widget? trailing;
  final VoidCallback onTap;

  const _SettingTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.trailing,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Material(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withOpacity(0.08),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(icon, size: 20, color: AppColors.primary),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: GoogleFonts.inter(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: AppColors.onSurface,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        subtitle,
                        style: GoogleFonts.inter(
                          fontSize: 13,
                          color: AppColors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                if (trailing != null) trailing!,
                if (trailing == null)
                  const Icon(
                    Icons.chevron_right_rounded,
                    size: 20,
                    color: AppColors.outline,
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
