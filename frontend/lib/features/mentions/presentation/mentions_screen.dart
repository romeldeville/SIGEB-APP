import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../auth/presentation/auth_controller.dart';
import '../data/mention_repository.dart';
import '../domain/mention_item.dart';

class MentionsScreen extends ConsumerStatefulWidget {
  const MentionsScreen({super.key});

  @override
  ConsumerState<MentionsScreen> createState() => _MentionsScreenState();
}

class _MentionsScreenState extends ConsumerState<MentionsScreen> {
  List<MentionItem> _boite = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await ref.read(mentionRepositoryProvider).boiteReception();
      setState(() {
        _boite = list;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _loading = false;
      });
    }
  }

  Future<void> _dialogDeclarer() async {
    String type = 'MARIAGE';
    final iun = TextEditingController();
    final date = TextEditingController(text: '2026-04-15');
    final lieu = TextEditingController();
    final ministre = TextEditingController();
    final conjoint = TextEditingController();

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) => AlertDialog(
          title: const Text('Déclarer une mention marginale (Paroisse B → A)'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(controller: iun, decoration: const InputDecoration(labelText: 'IUN du baptisé *')),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: type,
                  decoration: const InputDecoration(labelText: 'Sacrement'),
                  items: const [
                    DropdownMenuItem(value: 'CONFIRMATION', child: Text('Confirmation')),
                    DropdownMenuItem(value: 'MARIAGE', child: Text('Mariage')),
                    DropdownMenuItem(value: 'NULLITE_MARIAGE', child: Text('Nullité de mariage')),
                    DropdownMenuItem(value: 'ORDINATION_DIACONAT', child: Text('Ordination diaconale')),
                    DropdownMenuItem(value: 'ORDINATION_PRESBYTERAT', child: Text('Ordination presbytérale')),
                    DropdownMenuItem(value: 'VOEUX_PERPETUELS', child: Text('Vœux perpétuels')),
                  ],
                  onChanged: (v) => setS(() => type = v ?? type),
                ),
                const SizedBox(height: 10),
                TextField(controller: date, decoration: const InputDecoration(labelText: 'Date (AAAA-MM-JJ) *')),
                const SizedBox(height: 10),
                TextField(controller: lieu, decoration: const InputDecoration(labelText: 'Lieu de célébration *')),
                const SizedBox(height: 10),
                TextField(controller: ministre, decoration: const InputDecoration(labelText: 'Ministre célébrant *')),
                if (type == 'MARIAGE' || type == 'NULLITE_MARIAGE') ...[
                  const SizedBox(height: 10),
                  TextField(controller: conjoint, decoration: const InputDecoration(labelText: 'Nom complet du conjoint *')),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Notifier Paroisse A')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(mentionRepositoryProvider).declarer({
        'iunActeBapteme': iun.text.trim(),
        'typeSacrement': type,
        'dateEvenement': date.text.trim(),
        'lieuEvenement': lieu.text.trim(),
        'ministreCelebrant': ministre.text.trim(),
        if (conjoint.text.trim().isNotEmpty) 'conjointNomComplet': conjoint.text.trim(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Mention déclarée et transmise.')));
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _validerAvecPin(MentionItem m) async {
    final pinCtrl = TextEditingController();
    final pin = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Inscrire en marge (PIN Curé)'),
        content: TextField(controller: pinCtrl, obscureText: true, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Code PIN')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          ElevatedButton(onPressed: () => Navigator.pop(ctx, pinCtrl.text.trim()), child: const Text('Valider')),
        ],
      ),
    );
    if (pin == null || pin.isEmpty) return;
    try {
      await ref.read(mentionRepositoryProvider).valider(m.id, pin);
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _rejeterMention(MentionItem m) async {
    final motifCtrl = TextEditingController();
    final motif = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Rejeter la mention marginale'),
        content: TextField(
          controller: motifCtrl,
          maxLines: 3,
          decoration: const InputDecoration(labelText: 'Motif du rejet *'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.danger),
            onPressed: () => Navigator.pop(ctx, motifCtrl.text.trim()),
            child: const Text('Rejeter'),
          ),
        ],
      ),
    );
    if (motif == null || motif.isEmpty) return;
    try {
      await ref.read(mentionRepositoryProvider).rejeter(m.id, motif);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Mention marginale rejetée.')));
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    final isCure = ref.watch(authControllerProvider).session?.isCure == true;

    return AdaptiveScaffold(
      currentRoute: '/mentions',
      title: strings.navMentions,
      floatingActionButton: isCure
          ? FloatingActionButton.extended(
              backgroundColor: AppTheme.navy,
              foregroundColor: Colors.white,
              onPressed: _dialogDeclarer,
              icon: const Icon(Icons.send),
              label: const Text('Déclarer mention'),
            )
          : null,
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : _boite.isEmpty
                  ? const Center(child: Text('Aucune mention marginale en attente de validation.'))
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _boite.length,
                      itemBuilder: (_, i) {
                        final m = _boite[i];
                        return Card(
                          child: ListTile(
                            title: Text('${m.typeSacrement} · ${m.iun}', style: const TextStyle(fontWeight: FontWeight.bold)),
                            subtitle: Text('Paroisse déclarante : ${m.paroisseDeclaranteNomFr} · Date : ${m.dateEvenement}'
                                '${m.conjointNomComplet != null ? " · Conjoint : ${m.conjointNomComplet}" : ""}'),
                            trailing: isCure
                                ? Wrap(
                                    spacing: 8,
                                    children: [
                                      ElevatedButton(onPressed: () => _validerAvecPin(m), child: const Text('Inscrire')),
                                      OutlinedButton(
                                        style: OutlinedButton.styleFrom(foregroundColor: AppTheme.danger),
                                        onPressed: () => _rejeterMention(m),
                                        child: const Text('Rejeter'),
                                      ),
                                    ],
                                  )
                                : Chip(label: Text(m.statut)),
                          ),
                        );
                      },
                    ),
    );
  }
}
