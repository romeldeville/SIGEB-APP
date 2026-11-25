import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/audit/presentation/audit_screen.dart';
import '../../features/auth/presentation/auth_controller.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/profil_securite_screen.dart';
import '../../features/baptemes/presentation/bapteme_detail_screen.dart';
import '../../features/baptemes/presentation/bapteme_form_screen.dart';
import '../../features/baptemes/presentation/baptemes_list_screen.dart';
import '../../features/dashboard/presentation/dashboard_screen.dart';
import '../../features/mentions/presentation/mentions_screen.dart';
import '../../features/referentiel/presentation/referentiel_screen.dart';
import '../../features/verification/presentation/qr_scanner_screen.dart';
import '../../features/verification/presentation/recherche_nationale_screen.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final auth = ref.watch(authControllerProvider);

  return GoRouter(
    initialLocation: '/login',
    redirect: (context, state) {
      final loc = state.matchedLocation;
      final isPublic = loc == '/login' || loc == '/verification/qr';
      if (!auth.isAuthenticated && !isPublic) return '/login';
      if (auth.isAuthenticated && loc == '/login') return '/dashboard';
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (_, _) => const LoginScreen()),
      GoRoute(path: '/dashboard', builder: (_, _) => const DashboardScreen()),
      GoRoute(path: '/baptemes', builder: (_, _) => const BaptemesListScreen()),
      GoRoute(path: '/baptemes/nouveau', builder: (_, _) => const BaptemeFormScreen()),
      GoRoute(
        path: '/baptemes/:id',
        builder: (_, state) => BaptemeDetailScreen(acteId: state.pathParameters['id']!),
      ),
      GoRoute(path: '/verification/qr', builder: (_, _) => const QrScannerScreen()),
      GoRoute(path: '/verification/recherche', builder: (_, _) => const RechercheNationaleScreen()),
      GoRoute(path: '/mentions', builder: (_, _) => const MentionsScreen()),
      GoRoute(path: '/referentiel', builder: (_, _) => const ReferentielScreen()),
      GoRoute(path: '/audit', builder: (_, _) => const AuditScreen()),
      GoRoute(path: '/profil', builder: (_, _) => const ProfilSecuriteScreen()),
    ],
  );
});
