import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../../core/widgets/responsive_layout.dart';
import '../data/bapteme_repository.dart';

class BaptemeFormScreen extends ConsumerStatefulWidget {
  final String? acteId;
  const BaptemeFormScreen({super.key, this.acteId});

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
  bool _loadingActe = false;
  List<Map<String, dynamic>> _doublons = [];
  bool _doublonsChecked = false;

  bool get _isEdit => widget.acteId != null;

  @override
  void initState() {
    super.initState();
    if (_isEdit) _loadActe();
  }

  Future<void> _loadActe() async {
    setState(() => _loadingActe = true);
    try {
      final a = await ref.read(baptemeRepositoryProvider).consulter(widget.acteId!);
      _nom.text = a.nom;
      _prenoms.text = a.prenoms;
      _sexe = a.sexe;
      _origine = a.origine;
      _dateNaiss.text = a.dateNaissance;
      _lieuNaiss.text = a.lieuNaissance;
      _nomPere.text = a.nomPere ?? '';
      _nomMere.text = a.nomMere ?? '';
      _nomParrain.text = a.nomParrain ?? '';
      _nomMarraine.text = a.nomMarraine ?? '';
      _dateBap.text = a.dateBapteme;
      _lieuBap.text = a.lieuBapteme;
      _ministre.text = a.ministreCelebrant;
      _tome.text = a.registreTome;
      _page.text = a.registrePage;
      _numero.text = a.registreNumero;
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    } finally {
      if (mounted) setState(() => _loadingActe = false);
    }
  }

  Future<void> _verifierDoublons() async {
    if (_nom.text.trim().isEmpty || _prenoms.text.trim().isEmpty || _dateNaiss.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Renseignez Nom, Prénoms et Date de naissance pour vérifier les doublons.')),
      );
      return;
    }
    try {
      final list = await ref.read(baptemeRepositoryProvider).verifierDoublons(
            nom: _nom.text,
            prenoms: _prenoms.text,
            dateNaissance: _dateNaiss.text,
          );
      setState(() {
        _doublons = list;
        _doublonsChecked = true;
      });
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    final payload = {
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
    };
    try {
      final repo = ref.read(baptemeRepositoryProvider);
      final saved = _isEdit ? await repo.modifier(widget.acteId!, payload) : await repo.creer(payload);
      if (mounted) context.go('/baptemes/${saved.id}');
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
      title: _isEdit ? 'Modifier l\'acte de baptême' : strings.newAct,
      body: _loadingActe
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
        child: ResponsiveContainer(
          maxWidth: 920,
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (_doublonsChecked)
                  Container(
                    margin: const EdgeInsets.only(bottom: 14),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: (_doublons.isEmpty ? AppTheme.success : AppTheme.warning).withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: _doublons.isEmpty ? AppTheme.success : AppTheme.warning),
                    ),
                    child: _doublons.isEmpty
                        ? const Text('Aucun doublon potentiel détecté au registre national.', style: TextStyle(color: AppTheme.success, fontWeight: FontWeight.w600))
                        : Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('${_doublons.length} doublon(s) potentiel(s) trouvé(s) :',
                                  style: const TextStyle(color: AppTheme.warning, fontWeight: FontWeight.bold)),
                              ..._doublons.map((d) => Text('• ${d['nom']} ${d['prenoms']} (${d['iun'] ?? d['statut']}) — ${d['paroisseNomFr']}')),
                            ],
                          ),
                  ),
                SegmentedButton<String>(
                  segments: const [
                    ButtonSegment(value: 'NOUVEAU', label: Text('Nouveau')),
                    ButtonSegment(value: 'REPRISE_ARCHIVE', label: Text('Reprise archive')),
                  ],
                  selected: {_origine},
                  onSelectionChanged: (s) => setState(() => _origine = s.first),
                ),
                const SizedBox(height: 16),
                ResponsiveGrid(
                  minItemWidth: 280,
                  maxColumns: 2,
                  children: [
                    TextFormField(controller: _nom, decoration: const InputDecoration(labelText: 'Nom *'), validator: _req),
                    TextFormField(controller: _prenoms, decoration: const InputDecoration(labelText: 'Prénoms *'), validator: _req),
                    DropdownButtonFormField<String>(
                      initialValue: _sexe,
                      decoration: const InputDecoration(labelText: 'Sexe'),
                      items: const [
                        DropdownMenuItem(value: 'M', child: Text('Masculin')),
                        DropdownMenuItem(value: 'F', child: Text('Féminin')),
                      ],
                      onChanged: (v) => setState(() => _sexe = v ?? 'M'),
                    ),
                    TextFormField(controller: _dateNaiss, decoration: const InputDecoration(labelText: 'Date de naissance (AAAA-MM-JJ) *'), validator: _req),
                    TextFormField(controller: _lieuNaiss, decoration: const InputDecoration(labelText: 'Lieu de naissance *'), validator: _req),
                    TextFormField(controller: _nomPere, decoration: const InputDecoration(labelText: 'Nom du père')),
                    TextFormField(controller: _nomMere, decoration: const InputDecoration(labelText: 'Nom de la mère')),
                    TextFormField(controller: _nomParrain, decoration: const InputDecoration(labelText: 'Parrain (au moins 1 requis)')),
                    TextFormField(controller: _nomMarraine, decoration: const InputDecoration(labelText: 'Marraine')),
                    TextFormField(controller: _dateBap, decoration: const InputDecoration(labelText: 'Date de baptême (AAAA-MM-JJ) *'), validator: _req),
                    TextFormField(controller: _lieuBap, decoration: const InputDecoration(labelText: 'Lieu de célébration *'), validator: _req),
                    TextFormField(controller: _ministre, decoration: const InputDecoration(labelText: 'Ministre célébrant *'), validator: _req),
                  ],
                ),
                const SizedBox(height: 14),
                ResponsiveGrid(
                  minItemWidth: 160,
                  maxColumns: 3,
                  children: [
                    TextFormField(controller: _tome, decoration: const InputDecoration(labelText: 'Registre Tome *'), validator: _req),
                    TextFormField(controller: _page, decoration: const InputDecoration(labelText: 'Registre Page *'), validator: _req),
                    TextFormField(controller: _numero, decoration: const InputDecoration(labelText: 'Registre N° *'), validator: _req),
                  ],
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Soumettre directement au Curé pour scellement'),
                  value: _soumettre,
                  onChanged: (v) => setState(() => _soumettre = v),
                ),
                const SizedBox(height: 14),
                Wrap(
                  alignment: WrapAlignment.end,
                  spacing: 12,
                  runSpacing: 10,
                  children: [
                    OutlinedButton.icon(
                      onPressed: _saving ? null : _verifierDoublons,
                      icon: const Icon(Icons.find_in_page_outlined),
                      label: const Text('Vérifier doublons'),
                    ),
                    ElevatedButton(
                      onPressed: _saving ? null : _save,
                      child: _saving
                          ? const CircularProgressIndicator(color: Colors.white)
                          : Text(_isEdit ? 'Enregistrer les modifications' : 'Enregistrer l\'acte'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String? _req(String? v) => (v == null || v.trim().isEmpty) ? 'Obligatoire' : null;
}
