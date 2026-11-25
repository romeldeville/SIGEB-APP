import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../../core/widgets/responsive_layout.dart';
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
          data: (stats) => SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            child: ResponsiveContainer(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Bannière institutionnelle de bienvenue
                  Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      gradient: AppTheme.navyGradient,
                      borderRadius: BorderRadius.circular(20),
                      boxShadow: AppTheme.softShadow,
                    ),
                    child: Wrap(
                      spacing: 16,
                      runSpacing: 16,
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                              decoration: BoxDecoration(
                                color: AppTheme.gold.withValues(alpha: 0.22),
                                borderRadius: BorderRadius.circular(999),
                              ),
                              child: Text(
                                'PÉRIMÈTRE ${stats.niveau}',
                                style: const TextStyle(
                                  color: AppTheme.goldLight,
                                  fontSize: 11,
                                  fontWeight: FontWeight.w800,
                                  letterSpacing: 0.9,
                                ),
                              ),
                            ),
                            const SizedBox(height: 10),
                            Text(
                              'Bienvenue, ${session?.nomComplet ?? "SIGEB"}',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 22,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '${session?.role ?? ""} · Registre National des Baptêmes du Cameroun',
                              style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 13),
                            ),
                          ],
                        ),
                        Wrap(
                          spacing: 10,
                          runSpacing: 10,
                          children: [
                            if (session?.isParoisse == true)
                              ElevatedButton.icon(
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppTheme.gold,
                                  foregroundColor: AppTheme.navyDark,
                                ),
                                onPressed: () => context.go('/baptemes/nouveau'),
                                icon: const Icon(Icons.add_rounded),
                                label: Text(strings.newAct),
                              ),
                            ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.white.withValues(alpha: 0.14),
                                foregroundColor: Colors.white,
                              ),
                              onPressed: () => context.go('/verification/qr'),
                              icon: const Icon(Icons.qr_code_scanner_rounded),
                              label: Text(strings.navScan),
                            ),
                            if (session != null && !(session.role == 'SECRETAIRE' && session.isParoisse))
                              ElevatedButton.icon(
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: Colors.white.withValues(alpha: 0.14),
                                  foregroundColor: Colors.white,
                                ),
                                onPressed: () => context.go('/verification/recherche'),
                                icon: const Icon(Icons.search_rounded),
                                label: Text(strings.nationalSearch),
                              ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 22),

                  // Grille KPI fluide 1 -> 5 colonnes
                  ResponsiveGrid(
                    minItemWidth: 185,
                    maxColumns: 5,
                    children: [
                      _PrestigeKpiCard(
                        title: strings.sealedActs,
                        subtitle: 'Actes officiels IUN',
                        value: '${stats.totalActesScelles}',
                        color: AppTheme.navy,
                        icon: Icons.verified_rounded,
                      ),
                      _PrestigeKpiCard(
                        title: strings.pendingActs,
                        subtitle: 'En attente de PIN Curé',
                        value: '${stats.totalActesEnAttente}',
                        color: AppTheme.warning,
                        icon: Icons.hourglass_top_rounded,
                      ),
                      _PrestigeKpiCard(
                        title: strings.drafts,
                        subtitle: 'Saisie en cours',
                        value: '${stats.totalBrouillonsOuACorriger}',
                        color: const Color(0xFF475569),
                        icon: Icons.edit_note_rounded,
                      ),
                      _PrestigeKpiCard(
                        title: strings.originalCards,
                        subtitle: 'Cartes & extraits émis',
                        value: '${stats.totalCartesOriginales}',
                        color: AppTheme.success,
                        icon: Icons.badge_rounded,
                      ),
                      _PrestigeKpiCard(
                        title: strings.duplicates,
                        subtitle: 'Remplacements tracés',
                        value: '${stats.totalDuplicatas}',
                        color: AppTheme.gold,
                        icon: Icons.copy_all_rounded,
                      ),
                    ],
                  ),
                  const SizedBox(height: 22),

                  // Panneaux analytiques côte à côte sur grand écran
                  ResponsiveGrid(
                    minItemWidth: 320,
                    maxColumns: 2,
                    children: [
                      _BreakdownCard(
                        title: 'Baptêmes scellés par année',
                        icon: Icons.bar_chart_rounded,
                        emptyText: 'Aucun baptême scellé enregistré pour le moment.',
                        entries: stats.baptemesParAnnee,
                        barColor: AppTheme.navy,
                      ),
                      _BreakdownCard(
                        title: 'Mentions marginales inscrites',
                        icon: Icons.sync_alt_rounded,
                        emptyText: 'Aucune mention marginale inscrite pour le moment.',
                        entries: stats.mentionsParType,
                        barColor: AppTheme.gold,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _PrestigeKpiCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final String value;
  final Color color;
  final IconData icon;

  const _PrestigeKpiCard({
    required this.title,
    required this.subtitle,
    required this.value,
    required this.color,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppTheme.border),
        boxShadow: AppTheme.softShadow,
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(height: 4, color: color),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(9),
                      decoration: BoxDecoration(
                        color: color.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Icon(icon, color: color, size: 22),
                    ),
                    Text(
                      value,
                      style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900, color: color),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: AppTheme.textPrimary)),
                const SizedBox(height: 2),
                Text(subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11.5, color: AppTheme.textMuted)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _BreakdownCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final String emptyText;
  final Map<String, int> entries;
  final Color barColor;

  const _BreakdownCard({
    required this.title,
    required this.icon,
    required this.emptyText,
    required this.entries,
    required this.barColor,
  });

  @override
  Widget build(BuildContext context) {
    final maxVal = entries.values.fold<int>(1, (a, b) => a > b ? a : b);

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppTheme.border),
        boxShadow: AppTheme.softShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: barColor, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(title,
                    style: const TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800, color: AppTheme.navy)),
              ),
            ],
          ),
          const Divider(height: 22),
          if (entries.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 18),
              child: Text(emptyText, style: const TextStyle(color: AppTheme.textMuted, fontSize: 13)),
            )
          else
            ...entries.entries.map((e) => Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(e.key, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                          Text('${e.value}', style: TextStyle(fontWeight: FontWeight.w800, color: barColor)),
                        ],
                      ),
                      const SizedBox(height: 6),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(999),
                        child: LinearProgressIndicator(
                          value: e.value / maxVal,
                          minHeight: 7,
                          backgroundColor: const Color(0xFFE2E8F0),
                          color: barColor,
                        ),
                      ),
                    ],
                  ),
                )),
        ],
      ),
    );
  }
}
