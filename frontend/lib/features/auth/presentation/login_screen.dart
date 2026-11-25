import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/widgets/responsive_layout.dart';
import 'auth_controller.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifiantCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _totpCtrl = TextEditingController();
  bool _obscure = true;

  @override
  void dispose() {
    _identifiantCtrl.dispose();
    _passwordCtrl.dispose();
    _totpCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final ok = await ref.read(authControllerProvider.notifier).login(
          _identifiantCtrl.text,
          _passwordCtrl.text,
          totpCode: _totpCtrl.text,
        );
    if (ok && mounted) {
      context.go('/dashboard');
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    final authState = ref.watch(authControllerProvider);
    final lang = ref.watch(localeProvider);
    final isDesktop = ScreenBreakpoints.isDesktop(context);

    final loginCard = ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 440),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: AppTheme.border),
              boxShadow: AppTheme.softShadow,
            ),
            clipBehavior: Clip.antiAlias,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Filet Or institutionnel
                Container(height: 5, decoration: const BoxDecoration(gradient: AppTheme.goldGradient)),
                Padding(
                  padding: EdgeInsets.all(isDesktop ? 32 : 22),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(10),
                                    decoration: BoxDecoration(
                                      color: AppTheme.navy,
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: const Icon(Icons.church_rounded, size: 22, color: AppTheme.goldLight),
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          strings.appTitle,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(
                                            fontSize: isDesktop ? 20 : 17,
                                            fontWeight: FontWeight.w800,
                                            color: AppTheme.navy,
                                          ),
                                        ),
                                        const Text(
                                          'Portail Officiel CENC',
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(fontSize: 11.5, color: AppTheme.gold, fontWeight: FontWeight.w700),
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 8),
                            TextButton(
                              style: TextButton.styleFrom(
                                backgroundColor: AppTheme.goldSoft,
                                minimumSize: const Size(44, 34),
                                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                              ),
                              onPressed: () =>
                                  ref.read(localeProvider.notifier).state = lang == 'FR' ? 'EN' : 'FR',
                              child: Text(
                                lang == 'FR' ? 'EN' : 'FR',
                                style: const TextStyle(color: AppTheme.navy, fontWeight: FontWeight.w800),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 22),
                        Text(
                          lang == 'FR' ? 'Accès Clergé & Secrétariat' : 'Clergy & Secretariat Access',
                          style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w800, color: AppTheme.textPrimary),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          lang == 'FR'
                              ? 'Accès réservé aux personnels ecclésiastiques habilités.'
                              : 'Restricted access for authorized church personnel.',
                          style: const TextStyle(fontSize: 13, color: AppTheme.textMuted),
                        ),
                        const SizedBox(height: 22),
                        TextFormField(
                          controller: _identifiantCtrl,
                          decoration: InputDecoration(
                            labelText: strings.identifier,
                            prefixIcon: const Icon(Icons.person_outline_rounded),
                          ),
                          keyboardType: TextInputType.emailAddress,
                          validator: (v) => (v == null || v.trim().isEmpty) ? 'Requis' : null,
                        ),
                        const SizedBox(height: 14),
                        TextFormField(
                          controller: _passwordCtrl,
                          obscureText: _obscure,
                          decoration: InputDecoration(
                            labelText: strings.password,
                            prefixIcon: const Icon(Icons.lock_outline_rounded),
                            suffixIcon: IconButton(
                              icon: Icon(_obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                              onPressed: () => setState(() => _obscure = !_obscure),
                            ),
                          ),
                          validator: (v) => (v == null || v.isEmpty) ? 'Requis' : null,
                        ),
                        const SizedBox(height: 14),
                        TextFormField(
                          controller: _totpCtrl,
                          decoration: InputDecoration(
                            labelText: strings.totpCode,
                            prefixIcon: const Icon(Icons.shield_outlined),
                          ),
                          keyboardType: TextInputType.number,
                        ),
                        if (authState.error != null) ...[
                          const SizedBox(height: 14),
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppTheme.danger.withValues(alpha: 0.08),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(color: AppTheme.danger.withValues(alpha: 0.3)),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.error_outline, color: AppTheme.danger, size: 18),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(authState.error!,
                                      style: const TextStyle(color: AppTheme.danger, fontSize: 13)),
                                ),
                              ],
                            ),
                          ),
                        ],
                        const SizedBox(height: 22),
                        SizedBox(
                          height: 50,
                          child: ElevatedButton(
                            onPressed: authState.loading ? null : _submit,
                            child: authState.loading
                                ? const SizedBox(
                                    height: 20,
                                    width: 20,
                                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                  )
                                : Text(strings.login, style: const TextStyle(fontSize: 15.5)),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Carte CTA Vérification Publique QR
          InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: () => context.go('/verification/qr'),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
              decoration: BoxDecoration(
                color: isDesktop ? Colors.white : Colors.white.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppTheme.gold.withValues(alpha: 0.55)),
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppTheme.gold.withValues(alpha: 0.18),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(Icons.qr_code_scanner_rounded,
                        color: isDesktop ? AppTheme.navy : AppTheme.goldLight, size: 22),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          strings.scanPublicQr,
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            fontSize: 13.5,
                            color: isDesktop ? AppTheme.navy : Colors.white,
                          ),
                        ),
                        Text(
                          'Contrôle d\'authenticité instantané sans connexion',
                          style: TextStyle(
                            fontSize: 11.5,
                            color: isDesktop ? AppTheme.textMuted : const Color(0xFFCBD5E1),
                          ),
                        ),
                      ],
                    ),
                  ),
                  Icon(Icons.arrow_forward_ios_rounded,
                      size: 16, color: isDesktop ? AppTheme.gold : AppTheme.goldLight),
                ],
              ),
            ),
          ),
        ],
      ),
    );

    if (isDesktop) {
      return Scaffold(
        backgroundColor: AppTheme.background,
        body: Row(
          children: [
            Expanded(
              flex: 5,
              child: Container(
                decoration: const BoxDecoration(gradient: AppTheme.navyGradient),
                child: Stack(
                  children: [
                    Positioned(
                      right: -90,
                      bottom: -90,
                      child: Icon(Icons.church_rounded, size: 360, color: Colors.white.withValues(alpha: 0.035)),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(52),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
                            decoration: BoxDecoration(
                              color: AppTheme.gold.withValues(alpha: 0.18),
                              borderRadius: BorderRadius.circular(999),
                              border: Border.all(color: AppTheme.gold.withValues(alpha: 0.45)),
                            ),
                            child: const Text(
                              'CONFÉRENCE ÉPISCOPALE NATIONALE DU CAMEROUN',
                              style: TextStyle(
                                color: AppTheme.goldLight,
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 1.1,
                              ),
                            ),
                          ),
                          const SizedBox(height: 22),
                          const Text(
                            'SIGEB Cameroun',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 44,
                              fontWeight: FontWeight.w900,
                              letterSpacing: -0.5,
                            ),
                          ),
                          const SizedBox(height: 10),
                          Text(
                            strings.appSubtitle,
                            style: const TextStyle(color: Color(0xFFD6DFEE), fontSize: 17, height: 1.45),
                          ),
                          const SizedBox(height: 36),
                          const _GlassFeatureTile(
                            icon: Icons.verified_rounded,
                            title: 'Scellement canonique & IUN national',
                            subtitle: 'Double contrôle Secrétariat → Curé (PIN Argon2id) et immuabilité garantie.',
                          ),
                          const SizedBox(height: 14),
                          const _GlassFeatureTile(
                            icon: Icons.qr_code_2_rounded,
                            title: 'Cartes & duplicatas infalsifiables',
                            subtitle: 'Signature HMAC-SHA256 versionnée, révocation immédiate et PDF à la volée.',
                          ),
                          const SizedBox(height: 14),
                          const _GlassFeatureTile(
                            icon: Icons.sync_alt_rounded,
                            title: 'Mentions marginales inter-paroisses',
                            subtitle: 'Confirmation, Mariage, Ordination et état libre vérifiés en 2 secondes.',
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Expanded(
              flex: 5,
              child: Center(
                child: SingleChildScrollView(padding: const EdgeInsets.all(36), child: loginCard),
              ),
            ),
          ],
        ),
      );
    }

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(gradient: AppTheme.navyGradient),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(padding: const EdgeInsets.all(16), child: loginCard),
          ),
        ),
      ),
    );
  }
}

class _GlassFeatureTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  const _GlassFeatureTile({required this.icon, required this.title, required this.subtitle});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(9),
            decoration: BoxDecoration(
              color: AppTheme.gold.withValues(alpha: 0.2),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: AppTheme.goldLight, size: 20),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 14.5)),
                const SizedBox(height: 3),
                Text(subtitle, style: const TextStyle(color: Color(0xFFB8C4DA), fontSize: 12.5, height: 1.35)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
