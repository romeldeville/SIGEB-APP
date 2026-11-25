import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../data/bapteme_repository.dart';

class BaptemeFormScreen extends ConsumerStatefulWidget {
  const BaptemeFormScreen({super.key});

  @override
  ConsumerState<BaptemeFormScreen> createState() => _BaptemeFormScreenState();
}

class _BaptemeFormScreenState extends ConsumerState<BaptemeFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nom = TextEditingController();
  final _prenoms = TextEditingController();
  final _dateNaiss = TextEditingController(text: '2025-01-15');
  final _lieuNaiss = TextEditingController();
  final _nomPere = TextEditingController();
  final _nomMere = TextEditingController();
  final _nomParrain = TextEditingController();
  final _nomMarraine = TextEditingController();
  final _dateBap = TextEditingController(text: '2026-02-01');
  final _lieuBap = TextEditingController();
  final _ministre = TextEditingController();
  final _tome = TextEditingController();
  final _page = TextEditingController();
  final _numero = TextEditingController();

  String _sexe = 'M';
  String _origine = 'NOUVEAU';
  bool _soumettre = true;
  bool _saving = false;

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      final created = await ref.read(baptemeRepositoryProvider).creer({
        'origine': _origine,
        'nom': _nom.text.trim(),
        'prenoms': _prenoms.text.trim(),
        'sexe': _sexe,
        'dateNaissance': _dateNaiss.text.trim(),
        'lieuNaissance': _lieuNaiss.text.trim(),
        'nomPere': _nomPere.text.trim(),
        'nomMere': _nomMere.text.trim(),
        'filiationConfidentielle': false,
        'nomParrain': _nomParrain.text.trim().isEmpty ? null : _nomParrain.text.trim(),
        'nomMarraine': _nomMarraine.text.trim().isEmpty ? null : _nomMarraine.text.trim(),
        'dateBapteme': _dateBap.text.trim(),
        'lieuBapteme': _lieuBap.text.trim(),
        'ministreCelebrant': _ministre.text.trim(),
        'registreTome': _tome.text.trim(),
        'registrePage': _page.text.trim(),
        'registreNumero': _numero.text.trim(),
        'soumettrePourValidation': _soumettre,
      });
      if (mounted) context.go('/baptemes/${created.id}');
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    return AdaptiveScaffold(
      currentRoute: '/baptemes',
      title: strings.newAct,
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'NOUVEAU', label: Text('Nouveau baptême')),
                ButtonSegment(value: 'REPRISE_ARCHIVE', label: Text('Reprise archive papier')),
              ],
              selected: {_origine},
              onSelectionChanged: (s) => setState(() => _origine = s.first),
            ),
            const SizedBox(height: 16),
            TextFormField(controller: _nom, decoration: const InputDecoration(labelText: 'Nom *'), validator: _req),
            const SizedBox(height: 12),
            TextFormField(controller: _prenoms, decoration: const InputDecoration(labelText: 'Prénoms *'), validator: _req),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _sexe,
              decoration: const InputDecoration(labelText: 'Sexe'),
              items: const [DropdownMenuItem(value: 'M', child: Text('Masculin')), DropdownMenuItem(value: 'F', child: Text('Féminin'))],
              onChanged: (v) => setState(() => _sexe = v ?? 'M'),
            ),
            const SizedBox(height: 12),
            TextFormField(controller: _dateNaiss, decoration: const InputDecoration(labelText: 'Date de naissance (AAAA-MM-JJ) *'), validator: _req),
            const SizedBox(height: 12),
            TextFormField(controller: _lieuNaiss, decoration: const InputDecoration(labelText: 'Lieu de naissance *'), validator: _req),
            const SizedBox(height: 12),
            TextFormField(controller: _nomPere, decoration: const InputDecoration(labelText: 'Nom du père')),
            const SizedBox(height: 12),
            TextFormField(controller: _nomMere, decoration: const InputDecoration(labelText: 'Nom de la mère')),
            const SizedBox(height: 12),
            TextFormField(controller: _nomParrain, decoration: const InputDecoration(labelText: 'Parrain (au moins 1 requis)')),
            const SizedBox(height: 12),
            TextFormField(controller: _nomMarraine, decoration: const InputDecoration(labelText: 'Marraine')),
            const SizedBox(height: 12),
            TextFormField(controller: _dateBap, decoration: const InputDecoration(labelText: 'Date de baptême (AAAA-MM-JJ) *'), validator: _req),
            const SizedBox(height: 12),
            TextFormField(controller: _lieuBap, decoration: const InputDecoration(labelText: 'Lieu de célébration *'), validator: _req),
            const SizedBox(height: 12),
            TextFormField(controller: _ministre, decoration: const InputDecoration(labelText: 'Ministre célébrant *'), validator: _req),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: TextFormField(controller: _tome, decoration: const InputDecoration(labelText: 'Tome *'), validator: _req)),
                const SizedBox(width: 8),
                Expanded(child: TextFormField(controller: _page, decoration: const InputDecoration(labelText: 'Page *'), validator: _req)),
                const SizedBox(width: 8),
                Expanded(child: TextFormField(controller: _numero, decoration: const InputDecoration(labelText: 'N° *'), validator: _req)),
              ],
            ),
            SwitchListTile(
              title: const Text('Soumettre directement au Curé pour scellement'),
              value: _soumettre,
              onChanged: (v) => setState(() => _soumettre = v),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _saving ? null : _save,
              child: _saving ? const CircularProgressIndicator(color: Colors.white) : const Text('Enregistrer l\'acte'),
            ),
          ],
        ),
      ),
    );
  }

  String? _req(String? v) => (v == null || v.trim().isEmpty) ? 'Obligatoire' : null;
}
