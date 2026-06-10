import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../theme/app_theme.dart';

class UseCaseSection extends StatelessWidget {
  const UseCaseSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Use Cases',
          style: GoogleFonts.manrope(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppColors.onSurface,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Choose the right mode for your deployment scenario',
          style: TextStyle(
            fontSize: 14,
            color: AppColors.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 20),
        const _UseCaseCard(
          badge: '1:1',
          title: 'Verification',
          subtitle: 'One-to-one face matching for identity verification.',
          items: [
            'Mobile attendance & check-in',
            'App passwordless login',
            'Face-based authorization',
            'Face unlock',
            'Patrol checkpoint verification',
          ],
          color: AppColors.primary,
        ),
        const SizedBox(height: 16),
        const _UseCaseCard(
          badge: '1:N',
          title: 'Identification',
          subtitle:
              'Search one face against N registered faces in database.',
          items: [
            'Residential access control',
            'Corporate gate access',
            'Smart door locks',
            'Smart campus',
            'Robotics & smart home',
            'Hotel & community',
          ],
          color: AppColors.secondary,
        ),
        const SizedBox(height: 16),
        const _UseCaseCard(
          badge: 'M:N',
          title: 'Surveillance',
          subtitle:
              'Match M faces against N targets simultaneously in real-time.',
          items: [
            'Public security watchlist',
            'Crowd tracking',
            'Multi-camera monitoring',
          ],
          color: AppColors.tertiary,
        ),
      ],
    );
  }
}

class _UseCaseCard extends StatelessWidget {
  final String badge;
  final String title;
  final String subtitle;
  final List<String> items;
  final Color color;

  const _UseCaseCard({
    required this.badge,
    required this.title,
    required this.subtitle,
    required this.items,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: color.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  badge,
                  style: GoogleFonts.manrope(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: color,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Text(
                title,
                style: GoogleFonts.manrope(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: AppColors.onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            subtitle,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.onSurfaceVariant,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: items
                .map((item) => Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerHighest
                            .withOpacity(0.5),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        item,
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.onSurface,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ))
                .toList(),
          ),
        ],
      ),
    );
  }
}
