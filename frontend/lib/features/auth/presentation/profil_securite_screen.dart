import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../data/auth_repository.dart';
import 'auth_controller.dart';

class ProfilSecuriteScreen extends ConsumerStatefulWidget {
  const ProfilSecuriteScreen({super.key});

  @override
  ConsumerState<ProfilSecuriteScreen> createState() => _ProfilSecuriteScreenState();
}

class _ProfilSecuriteScreenState extends ConsumerState<ProfilSecuriteScreen> {
  final _ancienMdp = TextEditingController();
  final _nouveauMdp = TextEditingController();
  final _mdpPourPin = TextEditingController();
  final _nouveauPin = TextEditingController();

  Future<void> _changerMdp() async {
    try {
      await ref.read(authRepositoryProvider).changePassword(
            ancien: _ancienMdp.text,
            nouveau: _nouveauMdp.text,
          );
      _ancienMdp.clear();
      _nouveauMdp.clear();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Mot de passe mis à jour. Toutes les autres sessions ont été révoquées.')),
        );
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _changerPin() async {
    try {
      await ref.read(authRepositoryProvider).changePin(
            motDePasse: _mdpPourPin.text,
            pin: _nouveauPin.text.trim(),
          );
      _mdpPourPin.clear();
      _nouveauPin.clear();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Code PIN de signature enregistré (Argon2id).')),
        );
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).session;
    return AdaptiveScaffold(
      currentRoute: '/profil',
      title: 'Profil & Sécurité',
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Card(
            child: ListTile(
              leading: const CircleAvatar(backgroundColor: AppTheme.navy, child: Icon(Icons.person, color: Colors.white)),
              title: Text(session?.nomComplet ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
              subtitle: Text('Rôle : ${session?.role} · Niveau : ${session?.niveau} · 2FA : ${session?.totpActive == true ? "Actif" : "Non activé"}'),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Changer le mot de passe', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.navy)),
                  const SizedBox(height: 12),
                  TextField(controller: _ancienMdp, obscureText: true, decoration: const InputDecoration(labelText: 'Mot de passe actuel')),
                  const SizedBox(height: 10),
                  TextField(controller: _nouveauMdp, obscureText: true, decoration: const InputDecoration(labelText: 'Nouveau mot de passe (10+ caractères)')),
                  const SizedBox(height: 12),
                  ElevatedButton(onPressed: _changerMdp, child: const Text('Mettre à jour le mot de passe')),
                ],
              ),
            ),
          ),
          if (session?.isCure == true || session?.isChancelier == true) ...[
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Code PIN de signature canonique (Scellement / Avenants)',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.navy)),
                    const SizedBox(height: 12),
                    TextField(controller: _mdpPourPin, obscureText: true, decoration: const InputDecoration(labelText: 'Mot de passe de confirmation')),
                    const SizedBox(height: 10),
                    TextField(controller: _nouveauPin, obscureText: true, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Nouveau PIN (4 à 6 chiffres)')),
                    const SizedBox(height: 12),
                    ElevatedButton(onPressed: _changerPin, child: const Text('Enregistrer le PIN')),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
