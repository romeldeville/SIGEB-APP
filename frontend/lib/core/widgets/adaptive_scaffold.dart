import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/l10n/app_strings.dart';
import '../../app/theme/app_theme.dart';
import '../../features/auth/presentation/auth_controller.dart';
import 'responsive_layout.dart';

class _NavItem {
  final String route;
  final IconData icon;
  final String label;
  const _NavItem(this.route, this.icon, this.label);
}

class AdaptiveScaffold extends ConsumerWidget {
  final String currentRoute;
  final String title;
  final Widget body;
  final Widget? floatingActionButton;

  const AdaptiveScaffold({
    super.key,
    required this.currentRoute,
    required this.title,
    required this.body,
    this.floatingActionButton,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = ref.watch(stringsProvider);
    final session = ref.watch(authControllerProvider).session;
    final lang = ref.watch(localeProvider);
    final width = MediaQuery.sizeOf(context).width;

    final isMobile = width < ScreenBreakpoints.mobile;
    final isDesktop = width >= ScreenBreakpoints.tablet;
    final isWideDesktop = width >= ScreenBreakpoints.desktop;

    final items = <_NavItem>[
      _NavItem('/dashboard', Icons.dashboard_outlined, strings.navDashboard),
      if (session?.niveau == 'PAROISSE')
        _NavItem('/baptemes', Icons.menu_book_outlined, strings.navActes),
      _NavItem('/verification/qr', Icons.qr_code_scanner, strings.navScan),
      if (session?.niveau == 'PAROISSE')
        _NavItem('/mentions', Icons.sync_alt, strings.navMentions),
      if (session != null && session.role != 'SECRETAIRE')
        _NavItem('/referentiel', Icons.account_tree_outlined, strings.navReferentiel),
      if (session != null && !(session.role == 'SECRETAIRE' && session.niveau == 'PAROISSE'))
        _NavItem('/audit', Icons.verified_user_outlined, strings.navAudit),
    ];

    final selectedIndex = items.indexWhere((i) => currentRoute.startsWith(i.route));
    final safeIndex = selectedIndex < 0 ? 0 : selectedIndex;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: isMobile ? 16 : 18, fontWeight: FontWeight.w700),
            ),
            if (session != null)
              Text(
                '${session.nomComplet} · ${session.role} (${session.niveau})',
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11, color: Color(0xFFE2C15A)),
              ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => ref.read(localeProvider.notifier).state = lang == 'FR' ? 'EN' : 'FR',
            child: Text(lang, style: const TextStyle(color: AppTheme.gold, fontWeight: FontWeight.bold)),
          ),
          IconButton(
            tooltip: 'Profil & Sécurité',
            icon: const Icon(Icons.manage_accounts_outlined),
            onPressed: () => context.go('/profil'),
          ),
          IconButton(
            tooltip: strings.logout,
            icon: const Icon(Icons.logout),
            onPressed: () async {
              await ref.read(authControllerProvider.notifier).logout();
              if (context.mounted) context.go('/login');
            },
          ),
        ],
      ),
      drawer: isMobile
          ? Drawer(
              child: SafeArea(
                child: Column(
                  children: [
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(20),
                      color: AppTheme.navy,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(Icons.church, color: AppTheme.gold, size: 34),
                          const SizedBox(height: 8),
                          const Text('SIGEB Cameroun',
                              style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                          if (session != null)
                            Text('${session.role} · ${session.niveau}',
                                style: const TextStyle(color: Color(0xFFE2C15A), fontSize: 12)),
                        ],
                      ),
                    ),
                    Expanded(
                      child: ListView(
                        children: [
                          for (final item in items)
                            ListTile(
                              leading: Icon(item.icon,
                                  color: currentRoute.startsWith(item.route) ? AppTheme.navy : null),
                              title: Text(item.label),
                              selected: currentRoute.startsWith(item.route),
                              onTap: () {
                                Navigator.pop(context);
                                context.go(item.route);
                              },
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            )
          : null,
      body: SafeArea(
        child: !isMobile
            ? Row(
                children: [
                  NavigationRail(
                    selectedIndex: safeIndex,
                    extended: isWideDesktop,
                    labelType: isWideDesktop ? NavigationRailLabelType.none : NavigationRailLabelType.all,
                    backgroundColor: Colors.white,
                    indicatorColor: AppTheme.gold.withValues(alpha: 0.22),
                    leading: isDesktop
                        ? Padding(
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            child: Icon(Icons.church, color: AppTheme.gold, size: isWideDesktop ? 36 : 28),
                          )
                        : null,
                    onDestinationSelected: (idx) => context.go(items[idx].route),
                    destinations: items
                        .map((i) => NavigationRailDestination(icon: Icon(i.icon), label: Text(i.label)))
                        .toList(),
                  ),
                  const VerticalDivider(width: 1),
                  Expanded(child: body),
                ],
              )
            : body,
      ),
      bottomNavigationBar: isMobile && items.length >= 2
          ? NavigationBar(
              selectedIndex: safeIndex.clamp(0, items.length > 4 ? 3 : items.length - 1),
              labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
              onDestinationSelected: (idx) => context.go(items[idx].route),
              destinations: items
                  .take(4)
                  .map((i) => NavigationDestination(icon: Icon(i.icon), label: i.label))
                  .toList(),
            )
          : null,
      floatingActionButton: floatingActionButton,
    );
  }
}
