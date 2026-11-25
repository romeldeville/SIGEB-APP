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

    final formCard = ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 440),
      child: Card(
        elevation: isDesktop ? 4 : 8,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: Padding(
          padding: EdgeInsets.all(isDesktop ? 32 : 22),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Icon(Icons.church, size: 38, color: AppTheme.gold),
                    TextButton(
                      onPressed: () =>
                          ref.read(localeProvider.notifier).state = lang == 'FR' ? 'EN' : 'FR',
                      child: Text(
                        lang == 'FR' ? 'EN' : 'FR',
                        style: const TextStyle(color: AppTheme.navy, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  strings.appTitle,
                  style: const TextStyle(fontSize: 25, fontWeight: FontWeight.w800, color: AppTheme.navy),
                ),
                const SizedBox(height: 4),
                Text(strings.appSubtitle, style: const TextStyle(fontSize: 13, color: Color(0xFF64748B))),
                const SizedBox(height: 22),
                TextFormField(
                  controller: _identifiantCtrl,
                  decoration: InputDecoration(
                    labelText: strings.identifier,
                    prefixIcon: const Icon(Icons.person_outline),
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
                    prefixIcon: const Icon(Icons.lock_outline),
                    suffixIcon: IconButton(
                      icon: Icon(_obscure ? Icons.visibility : Icons.visibility_off),
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
                    prefixIcon: const Icon(Icons.security),
                  ),
                  keyboardType: TextInputType.number,
                ),
                if (authState.error != null) ...[
                  const SizedBox(height: 14),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppTheme.danger.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(authState.error!, style: const TextStyle(color: AppTheme.danger, fontSize: 13)),
                  ),
                ],
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: authState.loading ? null : _submit,
                  child: authState.loading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : Text(strings.login, style: const TextStyle(fontSize: 16)),
                ),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: () => context.go('/verification/qr'),
                  icon: const Icon(Icons.qr_code_scanner, color: AppTheme.navy),
                  label: Text(strings.scanPublicQr, style: const TextStyle(color: AppTheme.navy)),
                ),
              ],
            ),
          ),
        ),
      ),
    );

    if (isDesktop) {
      return Scaffold(
        body: Row(
          children: [
            Expanded(
              flex: 5,
              child: Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [AppTheme.navy, AppTheme.navyDark],
                  ),
                ),
                padding: const EdgeInsets.all(48),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.church, size: 64, color: AppTheme.gold),
                    const SizedBox(height: 20),
                    const Text(
                      'SIGEB Cameroun',
                      style: TextStyle(color: Colors.white, fontSize: 40, fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      strings.appSubtitle,
                      style: const TextStyle(color: Color(0xFFE2C15A), fontSize: 18, height: 1.4),
                    ),
                    const SizedBox(height: 32),
                    const _HeroPoint(icon: Icons.verified_user, text: 'Actes scellés par le Curé · IUN national unique'),
                    const _HeroPoint(icon: Icons.qr_code_2, text: 'Cartes & duplicatas infalsifiables avec QR Code signé'),
                    const _HeroPoint(icon: Icons.sync_alt, text: 'Mentions marginales inter-paroisses en temps réel'),
                  ],
                ),
              ),
            ),
            Expanded(
              flex: 5,
              child: Center(
                child: SingleChildScrollView(padding: const EdgeInsets.all(32), child: formCard),
              ),
            ),
          ],
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppTheme.navyDark,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(padding: const EdgeInsets.all(16), child: formCard),
        ),
      ),
    );
  }
}

class _HeroPoint extends StatelessWidget {
  final IconData icon;
  final String text;
  const _HeroPoint({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Row(
        children: [
          Icon(icon, color: AppTheme.gold, size: 22),
          const SizedBox(width: 12),
          Expanded(child: Text(text, style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 15))),
        ],
      ),
    );
  }
}
