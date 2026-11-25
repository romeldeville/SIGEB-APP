import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:printing/printing.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
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
              : ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
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
    );
  }
}
