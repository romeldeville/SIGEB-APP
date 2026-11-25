import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../auth/presentation/auth_controller.dart';
import '../data/statistique_repository.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = ref.watch(stringsProvider);
    final session = ref.watch(authControllerProvider).session;
    final statsAsync = ref.watch(dashboardStatsProvider);

    return AdaptiveScaffold(
      currentRoute: '/dashboard',
      title: strings.navDashboard,
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(dashboardStatsProvider),
        child: statsAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (err, _) => Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: AppTheme.danger),
                  const SizedBox(height: 12),
                  Text(extractApiError(err), textAlign: TextAlign.center),
                  const SizedBox(height: 12),
                  ElevatedButton(
                    onPressed: () => ref.invalidate(dashboardStatsProvider),
                    child: const Text('Réessayer'),
                  ),
                ],
              ),
            ),
          ),
          data: (stats) => ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Wrap(
                spacing: 14,
                runSpacing: 14,
                children: [
                  _KpiCard(title: strings.sealedActs, value: '${stats.totalActesScelles}', color: AppTheme.navy, icon: Icons.verified),
                  _KpiCard(title: strings.pendingActs, value: '${stats.totalActesEnAttente}', color: AppTheme.warning, icon: Icons.hourglass_top),
                  _KpiCard(title: strings.drafts, value: '${stats.totalBrouillonsOuACorriger}', color: const Color(0xFF475569), icon: Icons.edit_note),
                  _KpiCard(title: strings.originalCards, value: '${stats.totalCartesOriginales}', color: AppTheme.success, icon: Icons.badge_outlined),
                  _KpiCard(title: strings.duplicates, value: '${stats.totalDuplicatas}', color: AppTheme.gold, icon: Icons.copy_all),
                ],
              ),
              const SizedBox(height: 24),
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  if (session?.isParoisse == true)
                    ElevatedButton.icon(
                      onPressed: () => context.go('/baptemes/nouveau'),
                      icon: const Icon(Icons.add),
                      label: Text(strings.newAct),
                    ),
                  OutlinedButton.icon(
                    onPressed: () => context.go('/verification/qr'),
                    icon: const Icon(Icons.qr_code_scanner),
                    label: Text(strings.navScan),
                  ),
                  if (session != null && !(session.role == 'SECRETAIRE' && session.isParoisse))
                    OutlinedButton.icon(
                      onPressed: () => context.go('/verification/recherche'),
                      icon: const Icon(Icons.search),
                      label: Text(strings.nationalSearch),
                    ),
                ],
              ),
              if (stats.mentionsParType.isNotEmpty) ...[
                const SizedBox(height: 24),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(strings.navMentions, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.navy)),
                        const SizedBox(height: 10),
                        ...stats.mentionsParType.entries.map((e) => ListTile(
                              dense: true,
                              title: Text(e.key),
                              trailing: Chip(label: Text('${e.value}')),
                            )),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _KpiCard extends StatelessWidget {
  final String title;
  final String value;
  final Color color;
  final IconData icon;

  const _KpiCard({required this.title, required this.value, required this.color, required this.icon});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 210,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: color, size: 28),
              const SizedBox(height: 10),
              Text(value, style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: color)),
              const SizedBox(height: 4),
              Text(title, style: const TextStyle(fontSize: 13, color: Color(0xFF475569))),
            ],
          ),
        ),
      ),
    );
  }
}
