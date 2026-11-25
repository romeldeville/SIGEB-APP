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
  final IconData activeIcon;
  final String label;
  const _NavItem(this.route, this.icon, this.activeIcon, this.label);
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
    final isWideDesktop = width >= ScreenBreakpoints.desktop;

    final items = <_NavItem>[
      _NavItem('/dashboard', Icons.dashboard_outlined, Icons.dashboard_rounded, strings.navDashboard),
      if (session?.niveau == 'PAROISSE')
        _NavItem('/baptemes', Icons.menu_book_outlined, Icons.menu_book_rounded, strings.navActes),
      _NavItem('/verification/qr', Icons.qr_code_scanner, Icons.qr_code_2_rounded, strings.navScan),
      if (session?.niveau == 'PAROISSE')
        _NavItem('/mentions', Icons.sync_alt_outlined, Icons.sync_alt_rounded, strings.navMentions),
      if (session != null && session.role != 'SECRETAIRE')
        _NavItem('/referentiel', Icons.account_tree_outlined, Icons.account_tree_rounded, strings.navReferentiel),
      if (session != null && !(session.role == 'SECRETAIRE' && session.niveau == 'PAROISSE'))
        _NavItem('/audit', Icons.verified_user_outlined, Icons.verified_user_rounded, strings.navAudit),
    ];

    final selectedIndex = items.indexWhere((i) => currentRoute.startsWith(i.route));
    final safeIndex = selectedIndex < 0 ? 0 : selectedIndex;

    Widget buildSidebar({required bool expanded, bool inDrawer = false}) {
      return Container(
        width: expanded ? 264 : 84,
        decoration: const BoxDecoration(gradient: AppTheme.navyGradient),
        child: SafeArea(
          child: Column(
            children: [
              // Emblème SIGEB
              Padding(
                padding: EdgeInsets.symmetric(horizontal: expanded ? 20 : 12, vertical: 22),
                child: Row(
                  mainAxisAlignment: expanded ? MainAxisAlignment.start : MainAxisAlignment.center,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        gradient: AppTheme.goldGradient,
                        borderRadius: BorderRadius.circular(12),
                        boxShadow: [
                          BoxShadow(color: AppTheme.gold.withValues(alpha: 0.35), blurRadius: 12),
                        ],
                      ),
                      child: const Icon(Icons.church_rounded, color: AppTheme.navyDark, size: 22),
                    ),
                    if (expanded) ...[
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'SIGEB',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 19,
                                fontWeight: FontWeight.w800,
                                letterSpacing: 1.2,
                              ),
                            ),
                            Text(
                              'CENC · Cameroun',
                              style: TextStyle(color: AppTheme.goldLight, fontSize: 11, fontWeight: FontWeight.w500),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const Divider(color: Color(0x22FFFFFF), height: 1),
              const SizedBox(height: 12),
              Expanded(
                child: ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  itemCount: items.length,
                  itemBuilder: (context, idx) {
                    final item = items[idx];
                    final active = idx == safeIndex;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 4),
                      child: Material(
                        color: active ? AppTheme.gold.withValues(alpha: 0.18) : Colors.transparent,
                        borderRadius: BorderRadius.circular(12),
                        child: InkWell(
                          borderRadius: BorderRadius.circular(12),
                          onTap: () {
                            if (inDrawer) Navigator.pop(context);
                            context.go(item.route);
                          },
                          child: Container(
                            padding: EdgeInsets.symmetric(horizontal: expanded ? 14 : 0, vertical: 12),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(12),
                              border: active
                                  ? Border.all(color: AppTheme.gold.withValues(alpha: 0.45))
                                  : null,
                            ),
                            child: Row(
                              mainAxisAlignment: expanded ? MainAxisAlignment.start : MainAxisAlignment.center,
                              children: [
                                Icon(
                                  active ? item.activeIcon : item.icon,
                                  color: active ? AppTheme.goldLight : const Color(0xFFB8C4DA),
                                  size: 22,
                                ),
                                if (expanded) ...[
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Text(
                                      item.label,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(
                                        color: active ? Colors.white : const Color(0xFFCBD5E1),
                                        fontWeight: active ? FontWeight.w700 : FontWeight.w500,
                                        fontSize: 14,
                                      ),
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
              if (session != null && expanded)
                Padding(
                  padding: const EdgeInsets.all(14),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(12),
                    onTap: () {
                      if (inDrawer) Navigator.pop(context);
                      context.go('/profil');
                    },
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.07),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
                      ),
                      child: Row(
                        children: [
                          CircleAvatar(
                            radius: 18,
                            backgroundColor: AppTheme.gold,
                            child: Text(
                              session.nomComplet.isNotEmpty ? session.nomComplet[0].toUpperCase() : 'S',
                              style: const TextStyle(color: AppTheme.navyDark, fontWeight: FontWeight.bold),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  session.nomComplet,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(color: Colors.white, fontSize: 12.5, fontWeight: FontWeight.w600),
                                ),
                                Text(
                                  '${session.role} · ${session.niveau}',
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(color: AppTheme.goldLight, fontSize: 10.5),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      );
    }

    final topBar = AppBar(
      backgroundColor: isMobile ? AppTheme.navy : Colors.white,
      foregroundColor: isMobile ? Colors.white : AppTheme.navy,
      shape: isMobile ? null : const Border(bottom: BorderSide(color: AppTheme.border)),
      title: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: isMobile ? 16 : 19, fontWeight: FontWeight.w800),
                ),
                if (session != null && isMobile)
                  Text(
                    '${session.role} · ${session.niveau}',
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11, color: AppTheme.goldLight),
                  ),
              ],
            ),
          ),
        ],
      ),
      actions: [
        if (!isMobile)
          Container(
            margin: const EdgeInsets.symmetric(vertical: 12, horizontal: 6),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: AppTheme.success.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(999),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.circle, size: 8, color: AppTheme.success),
                SizedBox(width: 6),
                Text('Registre connecté', style: TextStyle(color: AppTheme.success, fontSize: 12, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: TextButton(
            style: TextButton.styleFrom(
              backgroundColor: isMobile ? Colors.white.withValues(alpha: 0.12) : AppTheme.goldSoft,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
              minimumSize: const Size(44, 34),
            ),
            onPressed: () => ref.read(localeProvider.notifier).state = lang == 'FR' ? 'EN' : 'FR',
            child: Text(
              lang,
              style: TextStyle(
                color: isMobile ? AppTheme.goldLight : AppTheme.navy,
                fontWeight: FontWeight.w800,
                fontSize: 12,
              ),
            ),
          ),
        ),
        IconButton(
          tooltip: 'Profil & Sécurité',
          icon: const Icon(Icons.manage_accounts_outlined),
          onPressed: () => context.go('/profil'),
        ),
        IconButton(
          tooltip: strings.logout,
          icon: const Icon(Icons.logout_rounded),
          onPressed: () async {
            await ref.read(authControllerProvider.notifier).logout();
            if (context.mounted) context.go('/login');
          },
        ),
        const SizedBox(width: 6),
      ],
    );

    if (!isMobile) {
      return Scaffold(
        body: Row(
          children: [
            buildSidebar(expanded: isWideDesktop),
            Expanded(
              child: Column(
                children: [
                  topBar,
                  Expanded(child: body),
                ],
              ),
            ),
          ],
        ),
        floatingActionButton: floatingActionButton,
      );
    }

    return Scaffold(
      appBar: topBar,
      drawer: Drawer(child: buildSidebar(expanded: true, inDrawer: true)),
      body: SafeArea(child: body),
      bottomNavigationBar: items.length >= 2
          ? NavigationBar(
              backgroundColor: Colors.white,
              indicatorColor: AppTheme.goldSoft,
              selectedIndex: safeIndex.clamp(0, items.length > 4 ? 3 : items.length - 1),
              labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
              onDestinationSelected: (idx) => context.go(items[idx].route),
              destinations: items
                  .take(4)
                  .map((i) => NavigationDestination(
                        icon: Icon(i.icon),
                        selectedIcon: Icon(i.activeIcon, color: AppTheme.navy),
                        label: i.label,
                      ))
                  .toList(),
            )
          : null,
      floatingActionButton: floatingActionButton,
    );
  }
}
