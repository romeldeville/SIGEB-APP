import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:printing/printing.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../../core/widgets/responsive_layout.dart';
import '../../auth/presentation/auth_controller.dart';
import '../data/bapteme_repository.dart';
import '../domain/acte_bapteme.dart';

class BaptemeDetailScreen extends ConsumerStatefulWidget {
  final String acteId;
  const BaptemeDetailScreen({super.key, required this.acteId});

  @override
  ConsumerState<BaptemeDetailScreen> createState() => _BaptemeDetailScreenState();
}

class _BaptemeDetailScreenState extends ConsumerState<BaptemeDetailScreen> {
  ActeBapteme? _acte;
  List<CarteEmise> _cartes = [];
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
      final repo = ref.read(baptemeRepositoryProvider);
      final acte = await repo.consulter(widget.acteId);
      final cartes = acte.isScelle ? await repo.listerCartes(widget.acteId) : <CarteEmise>[];
      setState(() {
        _acte = acte;
        _cartes = cartes;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _loading = false;
      });
    }
  }

  Future<void> _scellerAvecPin() async {
    final pinCtrl = TextEditingController();
    final pin = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Scellement canonique (PIN Curé)'),
        content: TextField(
          controller: pinCtrl,
          obscureText: true,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Code PIN (4 à 6 chiffres)'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          ElevatedButton(onPressed: () => Navigator.pop(ctx, pinCtrl.text.trim()), child: const Text('Sceller')),
        ],
      ),
    );
    if (pin == null || pin.isEmpty) return;
    try {
      await ref.read(baptemeRepositoryProvider).sceller(widget.acteId, pin);
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _emettreCarte() async {
    final estDuplicata = _cartes.any((c) => c.formatDocument == 'CARTE');
    try {
      await ref.read(baptemeRepositoryProvider).emettreCarte(
            acteId: widget.acteId,
            formatDocument: 'CARTE',
            langue: ref.read(localeProvider),
            motifEmission: estDuplicata ? 'MISE_A_JOUR' : 'PREMIERE_EMISSION',
          );
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _imprimerPdf(CarteEmise carte) async {
    try {
      final bytes = await ref.read(baptemeRepositoryProvider).telechargerPdf(carte.id);
      await Printing.layoutPdf(onLayout: (_) async => bytes);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    final session = ref.watch(authControllerProvider).session;

    return AdaptiveScaffold(
      currentRoute: '/baptemes',
      title: _acte?.iun ?? 'Détail Acte',
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : SingleChildScrollView(
                  child: ResponsiveContainer(
                    maxWidth: 960,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                    // Aperçu Prestige de la Carte de Baptême numérique (le "pourboire" visuel !)
                    if (_acte!.isScelle) ...[
                      Container(
                        decoration: BoxDecoration(
                          gradient: AppTheme.navyGradient,
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: AppTheme.gold, width: 1.5),
                          boxShadow: AppTheme.softShadow,
                        ),
                        padding: const EdgeInsets.all(22),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                const Row(
                                  children: [
                                    Icon(Icons.church_rounded, color: AppTheme.goldLight, size: 22),
                                    SizedBox(width: 8),
                                    Text(
                                      'ÉGLISE CATHOLIQUE DU CAMEROUN',
                                      style: TextStyle(
                                        color: AppTheme.goldLight,
                                        fontSize: 11,
                                        fontWeight: FontWeight.w800,
                                        letterSpacing: 1.1,
                                      ),
                                    ),
                                  ],
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: AppTheme.success.withValues(alpha: 0.25),
                                    borderRadius: BorderRadius.circular(999),
                                    border: Border.all(color: const Color(0xFF4ADE80)),
                                  ),
                                  child: const Text('SCELLÉ · OFFICIEL',
                                      style: TextStyle(color: Color(0xFF86EFAC), fontSize: 10, fontWeight: FontWeight.w800)),
                                ),
                              ],
                            ),
                            const SizedBox(height: 16),
                            Text(
                              '${_acte!.nom.toUpperCase()} ${_acte!.prenoms}',
                              style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w900),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              _acte!.iun ?? '',
                              style: const TextStyle(
                                color: AppTheme.goldLight,
                                fontFamily: 'monospace',
                                fontSize: 14,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 0.8,
                              ),
                            ),
                            const Divider(color: Color(0x33FFFFFF), height: 24),
                            Wrap(
                              spacing: 24,
                              runSpacing: 10,
                              children: [
                                _CardField(label: 'PAROISSE', value: _acte!.paroisseNomFr),
                                _CardField(label: 'BAPTISÉ(E) LE', value: _acte!.dateBapteme),
                                _CardField(label: 'REGISTRE PAPIER', value: 'T${_acte!.registreTome} / P${_acte!.registrePage} / N°${_acte!.registreNumero}'),
                                _CardField(label: 'VERSION', value: 'v${_acte!.version}'),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                    ],
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${_acte!.nom} ${_acte!.prenoms}',
                                style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppTheme.navy)),
                            const SizedBox(height: 6),
                            Text('IUN : ${_acte!.iun ?? "En attente de scellement"} · Statut : ${_acte!.statut} (v${_acte!.version})'),
                            const Divider(height: 20),
                            Text('Naissance : ${_acte!.dateNaissance} à ${_acte!.lieuNaissance}'),
                            Text('Baptême : ${_acte!.dateBapteme} à ${_acte!.lieuBapteme} (${_acte!.ministreCelebrant})'),
                            Text('Registre papier : Tome ${_acte!.registreTome} / Page ${_acte!.registrePage} / N° ${_acte!.registreNumero}'),
                            if (_acte!.nomParrain != null) Text('Parrain : ${_acte!.nomParrain}'),
                            if (_acte!.nomMarraine != null) Text('Marraine : ${_acte!.nomMarraine}'),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        if (!_acte!.isScelle && session?.isCure == true)
                          ElevatedButton.icon(
                            onPressed: _scellerAvecPin,
                            icon: const Icon(Icons.verified),
                            label: Text(strings.sealAct),
                          ),
                        if (_acte!.isScelle && session?.isParoisse == true)
                          ElevatedButton.icon(
                            onPressed: _emettreCarte,
                            icon: const Icon(Icons.badge_outlined),
                            label: Text(strings.issueCard),
                          ),
                      ],
                    ),
                    if (_cartes.isNotEmpty) ...[
                      const SizedBox(height: 24),
                      const Text('Cartes & Duplicatas émis (enregistrés en BD)',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.navy)),
                      const SizedBox(height: 8),
                      ..._cartes.map((c) => Card(
                            child: ListTile(
                              title: Text('${c.codeCarte} · ${c.typeEmission} ${c.numeroDuplicata > 0 ? "n°${c.numeroDuplicata}" : ""}'),
                              subtitle: Text('${c.formatDocument} (${c.langue}) · Statut : ${c.statut}'),
                              trailing: IconButton(
                                tooltip: strings.printPdf,
                                icon: const Icon(Icons.print, color: AppTheme.navy),
                                onPressed: () => _imprimerPdf(c),
                              ),
                            ),
                          )),
                    ],
                      ],
                    ),
                  ),
                ),
    );
  }
}

class _CardField extends StatelessWidget {
  final String label;
  final String value;
  const _CardField({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 10, fontWeight: FontWeight.w700)),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
      ],
    );
  }
}
