import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../../core/widgets/responsive_layout.dart';
import '../data/auth_repository.dart';
import '../domain/user_profile.dart';
import 'auth_controller.dart';

class ProfilSecuriteScreen extends ConsumerStatefulWidget {
  const ProfilSecuriteScreen({super.key});

  @override
  ConsumerState<ProfilSecuriteScreen> createState() => _ProfilSecuriteScreenState();
}

class _ProfilSecuriteScreenState extends ConsumerState<ProfilSecuriteScreen> {
  final _profileFormKey = GlobalKey<FormState>();
  final _nomComplet = TextEditingController();
  final _telephone = TextEditingController();
  final _ancienMdp = TextEditingController();
  final _nouveauMdp = TextEditingController();
  final _mdpPourPin = TextEditingController();
  final _nouveauPin = TextEditingController();
  final _totpCode = TextEditingController();

  UserProfile? _profile;
  TotpSetupData? _totpSetup;
  String _languePreferee = 'FR';

  bool _loadingProfile = true;
  bool _savingProfile = false;
  bool _savingMdp = false;
  bool _savingPin = false;
  bool _loadingTotp = false;
  bool _obscureAncien = true;
  bool _obscureNouveau = true;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _chargerProfil();
  }

  @override
  void dispose() {
    _nomComplet.dispose();
    _telephone.dispose();
    _ancienMdp.dispose();
    _nouveauMdp.dispose();
    _mdpPourPin.dispose();
    _nouveauPin.dispose();
    _totpCode.dispose();
    super.dispose();
  }

  Future<void> _chargerProfil() async {
    setState(() {
      _loadingProfile = true;
      _loadError = null;
    });
    try {
      final profile = await ref.read(authRepositoryProvider).getMyProfile();
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _nomComplet.text = profile.nomComplet;
        _telephone.text = profile.telephone ?? '';
        _languePreferee = profile.languePreferee;
        _loadingProfile = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loadError = extractApiError(e);
        _loadingProfile = false;
      });
    }
  }

  Future<void> _sauvegarderProfil() async {
    if (!(_profileFormKey.currentState?.validate() ?? false)) return;
    setState(() => _savingProfile = true);
    try {
      final updated = await ref.read(authRepositoryProvider).updateMyProfile(
            nomComplet: _nomComplet.text,
            telephone: _telephone.text,
            languePreferee: _languePreferee,
          );
      ref.read(authControllerProvider.notifier).updateSessionInfo(nomComplet: updated.nomComplet);
      ref.read(localeProvider.notifier).state = updated.languePreferee;
      if (!mounted) return;
      setState(() {
        _profile = updated;
        _savingProfile = false;
      });
      _showSuccess('Profil mis à jour avec succès et enregistré au journal d\'audit.');
    } catch (e) {
      if (!mounted) return;
      setState(() => _savingProfile = false);
      _showError(extractApiError(e));
    }
  }

  Future<void> _changerMdp() async {
    if (_ancienMdp.text.isEmpty || _nouveauMdp.text.length < 10) {
      _showError('Le nouveau mot de passe doit contenir au moins 10 caractères.');
      return;
    }
    setState(() => _savingMdp = true);
    try {
      await ref.read(authRepositoryProvider).changePassword(
            ancien: _ancienMdp.text,
            nouveau: _nouveauMdp.text,
          );
      _ancienMdp.clear();
      _nouveauMdp.clear();
      ref.read(authControllerProvider.notifier).updateSessionInfo(mdpAChanger: false);
      if (!mounted) return;
      setState(() => _savingMdp = false);
      _showSuccess('Mot de passe mis à jour. Toutes les autres sessions actives ont été révoquées.');
    } catch (e) {
      if (!mounted) return;
      setState(() => _savingMdp = false);
      _showError(extractApiError(e));
    }
  }

  Future<void> _changerPin() async {
    if (_mdpPourPin.text.isEmpty || _nouveauPin.text.trim().length < 4) {
      _showError('Veuillez saisir votre mot de passe et un PIN de 4 à 6 chiffres.');
      return;
    }
    setState(() => _savingPin = true);
    try {
      await ref.read(authRepositoryProvider).changePin(
            motDePasse: _mdpPourPin.text,
            pin: _nouveauPin.text.trim(),
          );
      _mdpPourPin.clear();
      _nouveauPin.clear();
      await _chargerProfil();
      if (!mounted) return;
      setState(() => _savingPin = false);
      _showSuccess('Code PIN de signature enregistré avec chiffrement Argon2id.');
    } catch (e) {
      if (!mounted) return;
      setState(() => _savingPin = false);
      _showError(extractApiError(e));
    }
  }

  Future<void> _initierTotp() async {
    setState(() => _loadingTotp = true);
    try {
      final setup = await ref.read(authRepositoryProvider).setupTotp();
      if (!mounted) return;
      setState(() {
        _totpSetup = setup;
        _loadingTotp = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loadingTotp = false);
      _showError(extractApiError(e));
    }
  }

  Future<void> _confirmerTotp() async {
    if (_totpCode.text.trim().length != 6) {
      _showError('Veuillez saisir le code à 6 chiffres généré par votre application Authenticator.');
      return;
    }
    setState(() => _loadingTotp = true);
    try {
      await ref.read(authRepositoryProvider).confirmTotp(_totpCode.text.trim());
      _totpCode.clear();
      ref.read(authControllerProvider.notifier).updateSessionInfo(totpActive: true);
      await _chargerProfil();
      if (!mounted) return;
      setState(() {
        _totpSetup = null;
        _loadingTotp = false;
      });
      _showSuccess('Double authentification (2FA) activée avec succès.');
    } catch (e) {
      if (!mounted) return;
      setState(() => _loadingTotp = false);
      _showError(extractApiError(e));
    }
  }

  void _showSuccess(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: AppTheme.success),
    );
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: AppTheme.danger),
    );
  }

  String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
    if (parts.isEmpty) return 'U';
    if (parts.length == 1) return parts.first.substring(0, 1).toUpperCase();
    return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).session;
    final profile = _profile;
    final displayName = profile?.nomComplet ?? session?.nomComplet ?? '';
    final role = profile?.role ?? session?.role ?? '';
    final niveau = profile?.niveau ?? session?.niveau ?? '';
    final totpActive = profile?.totpActive ?? session?.totpActive ?? false;
    final canSign = session?.canSignActs == true;

    return AdaptiveScaffold(
      currentRoute: '/profil',
      title: 'Mon Profil & Sécurité',
      body: RefreshIndicator(
        onRefresh: _chargerProfil,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: ResponsiveContainer(
            maxWidth: 1120,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // ── Bannière Prestige Utilisateur ──
                _ProfileHeroBanner(
                  initials: _initials(displayName),
                  nomComplet: displayName,
                  identifiant: profile?.identifiant ?? '',
                  affectation: profile?.affectationLabel ?? '',
                  role: role,
                  niveau: niveau,
                  totpActive: totpActive,
                  pinConfigure: profile?.pinConfigure ?? false,
                ),
                const SizedBox(height: 20),

                if (_loadingProfile)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 48),
                    child: Center(child: CircularProgressIndicator()),
                  )
                else if (_loadError != null)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        children: [
                          Text(_loadError!, style: const TextStyle(color: AppTheme.danger)),
                          const SizedBox(height: 12),
                          ElevatedButton.icon(
                            onPressed: _chargerProfil,
                            icon: const Icon(Icons.refresh),
                            label: const Text('Réessayer'),
                          ),
                        ],
                      ),
                    ),
                  )
                else
                  LayoutBuilder(
                    builder: (context, constraints) {
                      final isWide = constraints.maxWidth >= 900;
                      final leftColumn = Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          _buildPersonalInfoCard(),
                          const SizedBox(height: 18),
                          _buildGovernanceCard(profile!),
                        ],
                      );
                      final rightColumn = Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          _buildPasswordCard(),
                          const SizedBox(height: 18),
                          _buildTotpCard(totpActive),
                          if (canSign) ...[
                            const SizedBox(height: 18),
                            _buildPinCard(profile.pinConfigure),
                          ],
                        ],
                      );

                      if (isWide) {
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(flex: 5, child: leftColumn),
                            const SizedBox(width: 20),
                            Expanded(flex: 5, child: rightColumn),
                          ],
                        );
                      }
                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          leftColumn,
                          const SizedBox(height: 18),
                          rightColumn,
                        ],
                      );
                    },
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPersonalInfoCard() {
    return _SectionCard(
      icon: Icons.badge_outlined,
      accentColor: AppTheme.gold,
      title: 'Informations personnelles',
      subtitle: 'Modifiez votre nom d\'affichage, votre téléphone et votre langue préférée.',
      child: Form(
        key: _profileFormKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextFormField(
              controller: _nomComplet,
              decoration: const InputDecoration(
                labelText: 'Nom complet *',
                hintText: 'Ex. Abbé Jean-Marie Mbarga',
                prefixIcon: Icon(Icons.person_outline_rounded),
              ),
              validator: (v) {
                if (v == null || v.trim().length < 2) {
                  return 'Veuillez saisir un nom complet valide (2 caractères min.)';
                }
                return null;
              },
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _telephone,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: 'Téléphone',
                hintText: '+237 6XX XX XX XX',
                prefixIcon: Icon(Icons.phone_outlined),
              ),
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              initialValue: _languePreferee,
              decoration: const InputDecoration(
                labelText: 'Langue préférée',
                prefixIcon: Icon(Icons.translate_rounded),
              ),
              items: const [
                DropdownMenuItem(value: 'FR', child: Text('Français (FR)')),
                DropdownMenuItem(value: 'EN', child: Text('English (EN)')),
              ],
              onChanged: (v) {
                if (v != null) setState(() => _languePreferee = v);
              },
            ),
            const SizedBox(height: 18),
            ElevatedButton.icon(
              onPressed: _savingProfile ? null : _sauvegarderProfil,
              icon: _savingProfile
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.save_rounded, size: 18),
              label: Text(_savingProfile ? 'Enregistrement...' : 'Enregistrer mes informations'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGovernanceCard(UserProfile profile) {
    return _SectionCard(
      icon: Icons.verified_user_outlined,
      accentColor: AppTheme.navy,
      title: 'Affectation & Gouvernance',
      subtitle: 'Informations officielles gérées uniquement par votre supérieur hiérarchique.',
      child: Column(
        children: [
          _ReadOnlyRow(icon: Icons.alternate_email_rounded, label: 'Identifiant', value: profile.identifiant),
          const Divider(height: 20),
          _ReadOnlyRow(icon: Icons.shield_outlined, label: 'Rôle canonique', value: profile.role),
          const Divider(height: 20),
          _ReadOnlyRow(icon: Icons.account_tree_outlined, label: 'Niveau ecclésial', value: profile.niveau),
          const Divider(height: 20),
          _ReadOnlyRow(icon: Icons.church_outlined, label: 'Rattachement', value: profile.affectationLabel),
          const Divider(height: 20),
          _ReadOnlyRow(icon: Icons.check_circle_outline, label: 'Statut du compte', value: profile.statut),
        ],
      ),
    );
  }

  Widget _buildPasswordCard() {
    return _SectionCard(
      icon: Icons.lock_reset_rounded,
      accentColor: AppTheme.navy,
      title: 'Mot de passe',
      subtitle: 'La mise à jour révoque automatiquement toutes vos autres sessions actives.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _ancienMdp,
            obscureText: _obscureAncien,
            decoration: InputDecoration(
              labelText: 'Mot de passe actuel',
              prefixIcon: const Icon(Icons.lock_outline_rounded),
              suffixIcon: IconButton(
                icon: Icon(_obscureAncien ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                onPressed: () => setState(() => _obscureAncien = !_obscureAncien),
              ),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _nouveauMdp,
            obscureText: _obscureNouveau,
            decoration: InputDecoration(
              labelText: 'Nouveau mot de passe (10+ caractères)',
              prefixIcon: const Icon(Icons.vpn_key_outlined),
              suffixIcon: IconButton(
                icon: Icon(_obscureNouveau ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                onPressed: () => setState(() => _obscureNouveau = !_obscureNouveau),
              ),
            ),
          ),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: _savingMdp ? null : _changerMdp,
            icon: _savingMdp
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.security_rounded, size: 18),
            label: Text(_savingMdp ? 'Mise à jour...' : 'Mettre à jour le mot de passe'),
          ),
        ],
      ),
    );
  }

  Widget _buildTotpCard(bool totpActive) {
    return _SectionCard(
      icon: Icons.phonelink_lock_rounded,
      accentColor: totpActive ? AppTheme.success : AppTheme.gold,
      title: 'Double authentification (2FA / TOTP)',
      subtitle: totpActive
          ? 'Votre compte est protégé par une application Authenticator.'
          : 'Renforcez la sécurité de votre compte avec Google Authenticator ou Authy.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (totpActive && _totpSetup == null)
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.success.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.success.withValues(alpha: 0.3)),
              ),
              child: const Row(
                children: [
                  Icon(Icons.verified_rounded, color: AppTheme.success),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'La double authentification TOTP est active sur ce compte.',
                      style: TextStyle(fontWeight: FontWeight.w600, color: AppTheme.textPrimary),
                    ),
                  ),
                ],
              ),
            )
          else if (_totpSetup == null)
            OutlinedButton.icon(
              onPressed: _loadingTotp ? null : _initierTotp,
              icon: const Icon(Icons.qr_code_2_rounded),
              label: Text(_loadingTotp ? 'Génération...' : 'Configurer la double authentification (2FA)'),
            )
          else ...[
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.goldSoft,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.gold.withValues(alpha: 0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '1. Ajoutez cette clé secrète dans votre application Authenticator :',
                    style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: AppTheme.navy),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: SelectableText(
                          _totpSetup!.secret,
                          style: const TextStyle(
                            fontFamily: 'monospace',
                            fontWeight: FontWeight.w800,
                            fontSize: 14,
                            color: AppTheme.navyDark,
                          ),
                        ),
                      ),
                      IconButton(
                        tooltip: 'Copier la clé',
                        icon: const Icon(Icons.copy_rounded, size: 18, color: AppTheme.navy),
                        onPressed: () {
                          Clipboard.setData(ClipboardData(text: _totpSetup!.secret));
                          _showSuccess('Clé secrète 2FA copiée dans le presse-papiers.');
                        },
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _totpCode,
              keyboardType: TextInputType.number,
              maxLength: 6,
              decoration: const InputDecoration(
                labelText: '2. Saisissez le code à 6 chiffres généré',
                prefixIcon: Icon(Icons.pin_outlined),
                counterText: '',
              ),
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _loadingTotp ? null : _confirmerTotp,
              icon: const Icon(Icons.check_circle_outline_rounded, size: 18),
              label: Text(_loadingTotp ? 'Vérification...' : 'Activer la 2FA'),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildPinCard(bool pinConfigure) {
    return _SectionCard(
      icon: Icons.draw_rounded,
      accentColor: AppTheme.gold,
      title: 'Code PIN de signature canonique',
      subtitle: pinConfigure
          ? 'Un PIN est déjà configuré. Vous pouvez le renouveler ci-dessous.'
          : 'Requis pour sceller un acte de baptême ou approuver un avenant.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _mdpPourPin,
            obscureText: true,
            decoration: const InputDecoration(
              labelText: 'Mot de passe de confirmation',
              prefixIcon: Icon(Icons.lock_outline_rounded),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _nouveauPin,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            decoration: const InputDecoration(
              labelText: 'Nouveau PIN (4 à 6 chiffres)',
              prefixIcon: Icon(Icons.dialpad_rounded),
              counterText: '',
            ),
          ),
          const SizedBox(height: 14),
          ElevatedButton.icon(
            onPressed: _savingPin ? null : _changerPin,
            icon: _savingPin
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Icon(Icons.verified_rounded, size: 18),
            label: Text(_savingPin ? 'Enregistrement...' : 'Enregistrer le PIN'),
          ),
        ],
      ),
    );
  }
}

class _ProfileHeroBanner extends StatelessWidget {
  final String initials;
  final String nomComplet;
  final String identifiant;
  final String affectation;
  final String role;
  final String niveau;
  final bool totpActive;
  final bool pinConfigure;

  const _ProfileHeroBanner({
    required this.initials,
    required this.nomComplet,
    required this.identifiant,
    required this.affectation,
    required this.role,
    required this.niveau,
    required this.totpActive,
    required this.pinConfigure,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: AppTheme.navyGradient,
        borderRadius: BorderRadius.circular(20),
        boxShadow: AppTheme.softShadow,
      ),
      padding: const EdgeInsets.all(24),
      child: Wrap(
        spacing: 20,
        runSpacing: 16,
        alignment: WrapAlignment.spaceBetween,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: [
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  gradient: AppTheme.goldGradient,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: AppTheme.gold.withValues(alpha: 0.35),
                      blurRadius: 14,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                alignment: Alignment.center,
                child: Text(
                  initials,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.navyDark,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Flexible(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      nomComplet.isEmpty ? 'Utilisateur SIGEB' : nomComplet,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: Colors.white,
                      ),
                    ),
                    if (identifiant.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        identifiant,
                        style: TextStyle(
                          fontSize: 13.5,
                          color: Colors.white.withValues(alpha: 0.8),
                        ),
                      ),
                    ],
                    if (affectation.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        affectation,
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: AppTheme.goldLight,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _HeroPill(icon: Icons.shield_outlined, label: role),
              _HeroPill(icon: Icons.layers_outlined, label: niveau),
              _HeroPill(
                icon: totpActive ? Icons.verified_user_rounded : Icons.gpp_maybe_outlined,
                label: totpActive ? '2FA Actif' : '2FA Inactif',
                highlight: totpActive,
              ),
              if (pinConfigure)
                const _HeroPill(
                  icon: Icons.draw_rounded,
                  label: 'PIN configuré',
                  highlight: true,
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _HeroPill extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool highlight;

  const _HeroPill({required this.icon, required this.label, this.highlight = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 6),
      decoration: BoxDecoration(
        color: highlight
            ? AppTheme.gold.withValues(alpha: 0.22)
            : Colors.white.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: highlight
              ? AppTheme.goldLight.withValues(alpha: 0.65)
              : Colors.white.withValues(alpha: 0.2),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: highlight ? AppTheme.goldLight : Colors.white),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: highlight ? AppTheme.goldLight : Colors.white,
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  final IconData icon;
  final Color accentColor;
  final String title;
  final String subtitle;
  final Widget child;

  const _SectionCard({
    required this.icon,
    required this.accentColor,
    required this.title,
    required this.subtitle,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppTheme.border),
        boxShadow: AppTheme.softShadow,
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(height: 4, color: accentColor),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(9),
                      decoration: BoxDecoration(
                        color: accentColor.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(11),
                      ),
                      child: Icon(icon, size: 20, color: accentColor),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w800,
                              color: AppTheme.navy,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            subtitle,
                            style: const TextStyle(fontSize: 12.5, color: AppTheme.textMuted),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 18),
                child,
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ReadOnlyRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _ReadOnlyRow({required this.icon, required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 18, color: AppTheme.textMuted),
        const SizedBox(width: 10),
        Expanded(
          flex: 4,
          child: Text(
            label,
            style: const TextStyle(fontSize: 13, color: AppTheme.textMuted, fontWeight: FontWeight.w600),
          ),
        ),
        Expanded(
          flex: 6,
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: const TextStyle(fontSize: 13.5, color: AppTheme.textPrimary, fontWeight: FontWeight.w700),
          ),
        ),
      ],
    );
  }
}
