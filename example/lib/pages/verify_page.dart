import 'dart:io';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:face_ai_sdk/face_ai_sdk.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/person_face.dart';
import '../theme/app_theme.dart';

class VerifyPage extends StatefulWidget {
  const VerifyPage({super.key});

  @override
  State<VerifyPage> createState() => _VerifyPageState();
}

class _VerifyPageState extends State<VerifyPage> {
  static const _spKey = 'registered_faces';

  final _faceAiSdk = FaceAiSdk();
  List<PersonFace> _faces = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadFaces();
  }

  Future<void> _loadFaces() async {
    final prefs = await SharedPreferences.getInstance();
    final list = prefs.getStringList(_spKey) ?? [];
    setState(() {
      _faces = list.map(PersonFace.decode).toList();
      _isLoading = false;
    });
  }

  Future<void> _verify(PersonFace face) async {
    try {
      await _faceAiSdk.initializeSDK({'apiKey': 'demo-key'});
      if (!mounted) return;

      final result = await _faceAiSdk.startVerification(
        faceFeature: face.faceFeature.isNotEmpty ? face.faceFeature : null,
        faceId: null,
        livenessType: 2,
        motionStepSize: 2,
        allowRetry: false
      );
      if (!mounted) return;

      final code = result['code'] as int? ?? 0;
      final similarity = result['similarity'] as double? ?? 0.0;
      final msg = result['msg'] as String? ?? '';

      if (code == 1) {
        _showResult(
          icon: Icons.check_circle_rounded,
          color: AppColors.primary,
          title: 'Verified',
          subtitle: 'Similarity: ${(similarity * 100).toStringAsFixed(1)}%',
        );
      } else {
        _showResult(
          icon: Icons.cancel_rounded,
          color: AppColors.error,
          title: 'Not Verified',
          subtitle: msg.isNotEmpty ? msg : 'Verification failed',
        );
      }
    } catch (e) {
      if (!mounted) return;
      _showResult(
        icon: Icons.error_rounded,
        color: AppColors.error,
        title: 'Error',
        subtitle: e.toString(),
      );
    }
  }

  void _showResult({
    required IconData icon,
    required Color color,
    required String title,
    required String subtitle,
  }) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        backgroundColor: color,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        margin: const EdgeInsets.fromLTRB(24, 0, 24, 24),
        content: Row(
          children: [
            Icon(icon, color: Colors.white, size: 22),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.white.withValues(alpha: 0.85),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Verify',
          style: GoogleFonts.manrope(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: AppColors.onSurface,
          ),
        ),
        centerTitle: false,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadFaces,
              child: ListView(
              padding: const EdgeInsets.fromLTRB(24, 16, 24, 120),
              children: [
                // Header info
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.06),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: AppColors.primary.withValues(alpha: 0.12),
                    ),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.primary.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Icon(
                          Icons.face_unlock_rounded,
                          size: 28,
                          color: AppColors.primary,
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '1:1 Face Verification',
                              style: GoogleFonts.manrope(
                                fontSize: 16,
                                fontWeight: FontWeight.w700,
                                color: AppColors.onSurface,
                              ),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              'Select a face to verify against live camera.',
                              style: TextStyle(
                                fontSize: 13,
                                color: AppColors.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 24),

                // Stats row
                Row(
                  children: [
                    _StatChip(
                      label: 'Registered',
                      value: '${_faces.length}',
                      icon: Icons.people_alt_rounded,
                    ),
                  ],
                ),

                const SizedBox(height: 24),

                // Section title
                Text(
                  'Registered Faces',
                  style: GoogleFonts.manrope(
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: AppColors.onSurface,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Tap on a face to start 1:1 verification',
                  style: TextStyle(
                    fontSize: 13,
                    color: AppColors.onSurfaceVariant,
                  ),
                ),

                const SizedBox(height: 16),

                if (_faces.isEmpty)
                  Container(
                    padding: const EdgeInsets.symmetric(vertical: 48),
                    child: Column(
                      children: [
                        Icon(
                          Icons.person_off_rounded,
                          size: 48,
                          color: AppColors.outline.withValues(alpha: 0.5),
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'No registered faces yet',
                          style: TextStyle(
                            fontSize: 15,
                            color: AppColors.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'Enroll a face first from the Data page',
                          style: TextStyle(
                            fontSize: 13,
                            color: AppColors.outline,
                          ),
                        ),
                      ],
                    ),
                  )
                else
                  ...List.generate(_faces.length, (i) {
                    final face = _faces[i];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 14),
                      child: _FaceCard(
                        face: face,
                        index: i,
                        onTap: () => _verify(face),
                      ),
                    );
                  }),
              ],
            ),
            ),
    );
  }
}

// ── Widgets ──

class _StatChip extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;

  const _StatChip({
    required this.label,
    required this.value,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: AppColors.surfaceContainerLow,
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          children: [
            Icon(icon, size: 18, color: AppColors.primary),
            const SizedBox(width: 10),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  value,
                  style: GoogleFonts.manrope(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: AppColors.onSurface,
                  ),
                ),
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _FaceCard extends StatelessWidget {
  final PersonFace face;
  final int index;
  final VoidCallback onTap;

  const _FaceCard({
    required this.face,
    required this.index,
    required this.onTap,
  });

  static const _avatarColors = [
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
    Color(0xFFE91E63),
    Color(0xFF00BCD4),
    Color(0xFF795548),
    Color(0xFF607D8B),
  ];

  @override
  Widget build(BuildContext context) {
    final color = _avatarColors[index % _avatarColors.length];
    final hasImage =
        face.facePath.isNotEmpty && File(face.facePath).existsSync();

    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF191C1B).withValues(alpha: 0.05),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          splashColor: AppColors.primary.withValues(alpha: 0.08),
          highlightColor: AppColors.primary.withValues(alpha: 0.04),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                // Face avatar
                Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    color: hasImage ? null : color.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(16),
                    border: hasImage
                        ? null
                        : Border.all(
                            color: color.withValues(alpha: 0.3),
                            width: 2,
                          ),
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: hasImage
                      ? Image.file(File(face.facePath), fit: BoxFit.cover)
                      : Center(
                          child: Text(
                            face.faceId.isNotEmpty
                                ? face.faceId
                                    .split(' ')
                                    .map((e) => e[0])
                                    .take(2)
                                    .join()
                                    .toUpperCase()
                                : '?',
                            style: GoogleFonts.manrope(
                              fontSize: 17,
                              fontWeight: FontWeight.w800,
                              color: color,
                            ),
                          ),
                        ),
                ),

                const SizedBox(width: 16),

                // Name + subtitle
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        face.faceId,
                        style: GoogleFonts.manrope(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: AppColors.onSurface,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 3),
                      Row(
                        children: [
                          Container(
                            width: 7,
                            height: 7,
                            decoration: BoxDecoration(
                              color: face.faceFeature.isNotEmpty
                                  ? AppColors.primary
                                  : AppColors.outline,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 6),
                          Text(
                            face.faceFeature.isNotEmpty
                                ? 'Feature ready'
                                : 'ID only',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w500,
                              color: face.faceFeature.isNotEmpty
                                  ? AppColors.primary
                                  : AppColors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),

                const SizedBox(width: 12),

                // Verify button
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [
                        AppColors.primary,
                        AppColors.primaryContainer,
                      ],
                    ),
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.primary.withValues(alpha: 0.25),
                        blurRadius: 8,
                        offset: const Offset(0, 3),
                      ),
                    ],
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.verified_user_rounded,
                        size: 16,
                        color: Colors.white,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        'Verify',
                        style: GoogleFonts.manrope(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
