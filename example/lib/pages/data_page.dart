import 'dart:io';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:face_ai_sdk/face_ai_sdk.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/person_face.dart';
import '../theme/app_theme.dart';

class DataPage extends StatefulWidget {
  const DataPage({super.key});

  @override
  State<DataPage> createState() => _DataPageState();
}

class _DataPageState extends State<DataPage> {
  static const _spKey = 'registered_faces';

  final _nameController = TextEditingController();
  final _faceAiSdk = FaceAiSdk();
  _EnrollState _enrollState = _EnrollState.idle;
  List<PersonFace> _faces = [];

  bool get _canEnroll =>
      _nameController.text.trim().isNotEmpty &&
      _enrollState != _EnrollState.loading;

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
    });
  }

  Future<void> _saveFaces() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(
      _spKey,
      _faces.map((f) => f.encode()).toList(),
    );
  }

  Future<void> _enroll() async {
    if (!_canEnroll) return;

    final faceId = _nameController.text.trim();
    setState(() => _enrollState = _EnrollState.loading);

    try {
      await _faceAiSdk.initializeSDK(
        {'apiKey': 'demo-key', 'locale': 'en'},
      );
      if (!mounted) return;

      final result = await _faceAiSdk.startEnroll(faceId: faceId);
      if (!mounted) return;

      final code = result['code'] as int? ?? 0;
      if (code == 1) {
        final person = PersonFace(
          faceId: faceId,
          faceFeature: (result['faceFeature'] as String?) ?? '',
          facePath: (result['faceImage'] as String?) ?? '',
        );
        _faces.insert(0, person);
        await _saveFaces();
        _nameController.clear();
        setState(() => _enrollState = _EnrollState.success);
      } else {
        setState(() => _enrollState = _EnrollState.error);
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _enrollState = _EnrollState.error);
    }

    Future.delayed(const Duration(seconds: 3), () {
      if (mounted && _enrollState != _EnrollState.loading) {
        setState(() => _enrollState = _EnrollState.idle);
      }
    });
  }

  Future<void> _deleteFace(int index) async {
    setState(() => _faces.removeAt(index));
    await _saveFaces();
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Face AI SDK',
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
          // ── Editorial Header ──
          Text(
            'IDENTITY ENROLLMENT',
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w600,
              letterSpacing: 1.8,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(height: 8),
          Text.rich(
            TextSpan(
              children: [
                const TextSpan(text: 'Add Face '),
                TextSpan(
                  text: 'Feature Data',
                  style: TextStyle(color: AppColors.primary),
                ),
              ],
            ),
            style: GoogleFonts.manrope(
              fontSize: 34,
              fontWeight: FontWeight.w800,
              letterSpacing: -1,
              height: 1.1,
              color: AppColors.onSurface,
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'Securely capture facial landmarks to register a new identity '
            'into the biometric database.',
            style: TextStyle(
              fontSize: 16,
              color: AppColors.onSurfaceVariant,
              height: 1.5,
            ),
          ),

          const SizedBox(height: 32),

          // ── Form Card ──
          Container(
            padding: const EdgeInsets.all(28),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: BorderRadius.circular(28),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Subject Information',
                  style: GoogleFonts.manrope(
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: AppColors.onSurface,
                  ),
                ),
                const SizedBox(height: 24),

                // Name field
                const Text(
                  'FACE ID / NAME',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: AppColors.onSurfaceVariant,
                    letterSpacing: 1,
                  ),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _nameController,
                  onChanged: (_) => setState(() {}),
                  style: const TextStyle(
                    fontSize: 15,
                    color: AppColors.onSurface,
                  ),
                  decoration: InputDecoration(
                    hintText: 'e.g. Julian Montgomery',
                    hintStyle: const TextStyle(color: AppColors.outline),
                    filled: true,
                    fillColor: AppColors.surfaceContainerHighest,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: BorderSide.none,
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: BorderSide.none,
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 16,
                    ),
                  ),
                ),

                const SizedBox(height: 28),

                // Enroll button
                SizedBox(
                  width: double.infinity,
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: _canEnroll ? _enroll : null,
                      borderRadius: BorderRadius.circular(100),
                      child: Ink(
                        decoration: BoxDecoration(
                          gradient: _canEnroll
                              ? const LinearGradient(
                                  begin: Alignment.topLeft,
                                  end: Alignment.bottomRight,
                                  colors: [
                                    AppColors.primary,
                                    AppColors.primaryContainer,
                                  ],
                                )
                              : null,
                          color: _canEnroll
                              ? null
                              : AppColors.surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(100),
                          boxShadow: _canEnroll
                              ? [
                                  BoxShadow(
                                    color: AppColors.primary
                                        .withValues(alpha: 0.2),
                                    blurRadius: 16,
                                    offset: const Offset(0, 6),
                                  ),
                                ]
                              : null,
                        ),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 18),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              if (_enrollState == _EnrollState.loading)
                                const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.white,
                                  ),
                                )
                              else ...[
                                Text(
                                  'Enroll Subject',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w700,
                                    color: _canEnroll
                                        ? Colors.white
                                        : AppColors.outline,
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Icon(
                                  Icons.person_add_rounded,
                                  size: 20,
                                  color: _canEnroll
                                      ? Colors.white
                                      : AppColors.outline,
                                ),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // ── Feedback Notifications ──
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 400),
            child: _enrollState == _EnrollState.success
                ? _FeedbackCard(
                    key: const ValueKey('success'),
                    icon: Icons.check_circle_rounded,
                    iconColor: AppColors.primary,
                    bgColor: AppColors.primary.withValues(alpha: 0.1),
                    title: 'Face Enrolled Successfully',
                  )
                : _enrollState == _EnrollState.error
                    ? _FeedbackCard(
                        key: const ValueKey('error'),
                        icon: Icons.error_rounded,
                        iconColor: AppColors.error,
                        bgColor: AppColors.error.withValues(alpha: 0.1),
                        title: 'Enrollment Failed',
                      )
                    : const SizedBox.shrink(),
          ),

          const SizedBox(height: 32),

          // ── Registered Faces Grid ──
          if (_faces.isNotEmpty) ...[
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'REGISTERED FACES',
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w600,
                          letterSpacing: 1.8,
                          color: AppColors.primary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${_faces.length} subject${_faces.length == 1 ? '' : 's'}',
                        style: const TextStyle(
                          fontSize: 14,
                          color: AppColors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                mainAxisSpacing: 16,
                crossAxisSpacing: 16,
                childAspectRatio: 0.78,
              ),
              itemCount: _faces.length,
              itemBuilder: (context, i) => _RegisteredFaceTile(
                face: _faces[i],
                onDelete: () => _deleteFace(i),
              ),
            ),
          ],

          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

// ── Feedback Card ──

class _FeedbackCard extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final Color bgColor;
  final String title;

  const _FeedbackCard({
    super.key,
    required this.icon,
    required this.iconColor,
    required this.bgColor,
    required this.title,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: iconColor.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: iconColor, size: 22),
          ),
          const SizedBox(width: 14),
          Text(
            title,
            style: GoogleFonts.manrope(
              fontSize: 15,
              fontWeight: FontWeight.w700,
              color: iconColor,
            ),
          ),
        ],
      ),
    );
  }
}

// ── Registered Face Tile ──

class _RegisteredFaceTile extends StatelessWidget {
  final PersonFace face;
  final VoidCallback onDelete;

  const _RegisteredFaceTile({
    required this.face,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final hasImage =
        face.facePath.isNotEmpty && File(face.facePath).existsSync();

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF191C1B).withValues(alpha: 0.06),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Face image
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerHighest,
                    borderRadius: const BorderRadius.vertical(
                      top: Radius.circular(20),
                    ),
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: hasImage
                      ? Image.file(
                          File(face.facePath),
                          fit: BoxFit.cover,
                        )
                      : const Center(
                          child: Icon(
                            Icons.person_rounded,
                            color: AppColors.outline,
                            size: 36,
                          ),
                        ),
                ),
              ),

              // Name
              Padding(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
                child: Text(
                  face.faceId,
                  style: GoogleFonts.manrope(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: AppColors.onSurface,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                ),
              ),
            ],
          ),

          // Delete button
          Positioned(
            top: 6,
            right: 6,
            child: GestureDetector(
              onTap: onDelete,
              child: Container(
                width: 28,
                height: 28,
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.45),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.close_rounded,
                  color: Colors.white,
                  size: 16,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

enum _EnrollState { idle, loading, success, error }
