import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
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
  List<AvenantRectification> _avenants = [];
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
      final avenants = acte.isScelle ? await repo.listerAvenants(widget.acteId) : <AvenantRectification>[];
      setState(() {
        _acte = acte;
        _cartes = cartes;
        _avenants = avenants;
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

  Future<void> _renvoyerPourCorrection() async {
    final motifCtrl = TextEditingController();
    final motif = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Renvoyer au secrétariat pour correction'),
        content: TextField(
          controller: motifCtrl,
          decoration: const InputDecoration(labelText: 'Motif de la correction attendue *'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          ElevatedButton(onPressed: () => Navigator.pop(ctx, motifCtrl.text.trim()), child: const Text('Renvoyer')),
        ],
      ),
    );
    if (motif == null || motif.isEmpty) return;
    try {
      await ref.read(baptemeRepositoryProvider).renvoyerPourCorrection(widget.acteId, motif);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Acte renvoyé pour correction.')));
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _emettreCarte() async {
    String format = 'CARTE';
    String motif = _cartes.any((c) => c.formatDocument == 'CARTE') ? 'MISE_A_JOUR' : 'PREMIERE_EMISSION';

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) => AlertDialog(
          title: const Text('Émettre une Carte ou un Extrait A4'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<String>(
                initialValue: format,
                decoration: const InputDecoration(labelText: 'Format du document'),
                items: const [
                  DropdownMenuItem(value: 'CARTE', child: Text('Carte de Baptême (format PVC)')),
                  DropdownMenuItem(value: 'EXTRAIT_A4', child: Text('Extrait de Baptême (format A4)')),
                ],
                onChanged: (v) {
                  if (v == null) return;
                  final dejaEmis = _cartes.any((c) => c.formatDocument == v);
                  setS(() {
                    format = v;
                    if (!dejaEmis) motif = 'PREMIERE_EMISSION';
                    if (dejaEmis && motif == 'PREMIERE_EMISSION') motif = 'MISE_A_JOUR';
                  });
                },
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: motif,
                decoration: const InputDecoration(labelText: 'Motif d\'émission'),
                items: const [
                  DropdownMenuItem(value: 'PREMIERE_EMISSION', child: Text('Première émission')),
                  DropdownMenuItem(value: 'MISE_A_JOUR', child: Text('Duplicata — Mise à jour')),
                  DropdownMenuItem(value: 'PERTE', child: Text('Duplicata — Perte')),
                  DropdownMenuItem(value: 'VOL', child: Text('Duplicata — Vol')),
                  DropdownMenuItem(value: 'DETERIORATION', child: Text('Duplicata — Détérioration')),
                  DropdownMenuItem(value: 'MARIAGE', child: Text('Préparation Mariage')),
                  DropdownMenuItem(value: 'CONFIRMATION', child: Text('Préparation Confirmation')),
                  DropdownMenuItem(value: 'SEMINAIRE', child: Text('Dossier Séminaire')),
                ],
                onChanged: (v) => setS(() => motif = v ?? motif),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Émettre')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(baptemeRepositoryProvider).emettreCarte(
            acteId: widget.acteId,
            formatDocument: format,
            langue: ref.read(localeProvider),
            motifEmission: motif,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Document officiel émis avec QR signé cryptographiquement.')),
        );
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _revoquerCarte(CarteEmise carte) async {
    final motifCtrl = TextEditingController();
    final motif = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Révoquer ${carte.codeCarte}'),
        content: TextField(
          controller: motifCtrl,
          decoration: const InputDecoration(labelText: 'Motif de révocation (perte, vol, erreur...)'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.danger),
            onPressed: () => Navigator.pop(ctx, motifCtrl.text.trim()),
            child: const Text('Révoquer'),
          ),
        ],
      ),
    );
    if (motif == null || motif.isEmpty) return;
    try {
      await ref.read(baptemeRepositoryProvider).revoquerCarte(carte.id, motif);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Carte ${carte.codeCarte} révoquée.')),
        );
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _demanderAvenant() async {
    String gravite = 'MINEURE';
    String champ = 'lieuNaissance';
    final valeurCtrl = TextEditingController();
    final motifCtrl = TextEditingController();
    final refCtrl = TextEditingController();
    final pinCtrl = TextEditingController();

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) => AlertDialog(
          title: const Text('Avenant de rectification canonique'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: gravite,
                  decoration: const InputDecoration(labelText: 'Gravité'),
                  items: const [
                    DropdownMenuItem(value: 'MINEURE', child: Text('Mineure (validée par PIN Curé)')),
                    DropdownMenuItem(value: 'MAJEURE', child: Text('Majeure (soumise au Chancelier)')),
                  ],
                  onChanged: (v) => setS(() => gravite = v ?? gravite),
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: champ,
                  decoration: const InputDecoration(labelText: 'Champ à rectifier'),
                  items: const [
                    DropdownMenuItem(value: 'nom', child: Text('Nom de famille')),
                    DropdownMenuItem(value: 'prenoms', child: Text('Prénoms')),
                    DropdownMenuItem(value: 'nomBapteme', child: Text('Nom de baptême')),
                    DropdownMenuItem(value: 'dateNaissance', child: Text('Date de naissance (AAAA-MM-JJ)')),
                    DropdownMenuItem(value: 'lieuNaissance', child: Text('Lieu de naissance')),
                    DropdownMenuItem(value: 'nomPere', child: Text('Nom du père')),
                    DropdownMenuItem(value: 'nomMere', child: Text('Nom de la mère')),
                    DropdownMenuItem(value: 'nomParrain', child: Text('Nom du parrain')),
                    DropdownMenuItem(value: 'nomMarraine', child: Text('Nom de la marraine')),
                    DropdownMenuItem(value: 'lieuBapteme', child: Text('Lieu du baptême')),
                    DropdownMenuItem(value: 'ministreCelebrant', child: Text('Ministre célébrant')),
                  ],
                  onChanged: (v) => setS(() => champ = v ?? champ),
                ),
                const SizedBox(height: 10),
                TextField(controller: valeurCtrl, decoration: const InputDecoration(labelText: 'Nouvelle valeur *')),
                const SizedBox(height: 10),
                TextField(controller: motifCtrl, decoration: const InputDecoration(labelText: 'Motif de la rectification *')),
                const SizedBox(height: 10),
                TextField(controller: refCtrl, decoration: const InputDecoration(labelText: 'Référence justificatif (optionnel)')),
                if (gravite == 'MINEURE') ...[
                  const SizedBox(height: 10),
                  TextField(
                    controller: pinCtrl,
                    obscureText: true,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: 'PIN Curé (4 à 6 chiffres) *'),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Soumettre')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(baptemeRepositoryProvider).creerAvenant(widget.acteId, {
        'gravite': gravite,
        'nouvellesValeurs': {champ: valeurCtrl.text.trim()},
        'motif': motifCtrl.text.trim(),
        if (refCtrl.text.trim().isNotEmpty) 'referenceJustificatif': refCtrl.text.trim(),
        if (gravite == 'MINEURE') 'pinSignature': pinCtrl.text.trim(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              gravite == 'MINEURE'
                  ? 'Avenant mineur appliqué et acte incrémenté.'
                  : 'Avenant majeur transmis au Chancelier diocésain pour validation.',
            ),
          ),
        );
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _approuverAvenant(AvenantRectification a) async {
    final pinCtrl = TextEditingController();
    final pin = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Approuver l\'avenant v${a.versionCible} (PIN Chancelier)'),
        content: TextField(
          controller: pinCtrl,
          obscureText: true,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Code PIN Chancelier'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          ElevatedButton(onPressed: () => Navigator.pop(ctx, pinCtrl.text.trim()), child: const Text('Approuver')),
        ],
      ),
    );
    if (pin == null || pin.isEmpty) return;
    try {
      await ref.read(baptemeRepositoryProvider).approuverAvenant(a.id, pin);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Avenant majeur approuvé et appliqué.')));
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _rejeterAvenant(AvenantRectification a) async {
    try {
      await ref.read(baptemeRepositoryProvider).rejeterAvenant(a.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Avenant majeur rejeté.')));
      }
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
                            if (_acte!.motifCorrection != null && _acte!.motifCorrection!.isNotEmpty) ...[
                              const SizedBox(height: 10),
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: AppTheme.danger.withValues(alpha: 0.1),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Text(
                                  'Motif de correction demandé : ${_acte!.motifCorrection}',
                                  style: const TextStyle(color: AppTheme.danger, fontWeight: FontWeight.w600),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        if (!_acte!.isScelle && session?.isParoisse == true)
                          OutlinedButton.icon(
                            onPressed: () => context.go('/baptemes/${_acte!.id}/modifier'),
                            icon: const Icon(Icons.edit_outlined),
                            label: const Text('Modifier l\'acte'),
                          ),
                        if (!_acte!.isScelle && session?.isCure == true) ...[
                          ElevatedButton.icon(
                            onPressed: _scellerAvecPin,
                            icon: const Icon(Icons.verified),
                            label: Text(strings.sealAct),
                          ),
                          OutlinedButton.icon(
                            onPressed: _renvoyerPourCorrection,
                            icon: const Icon(Icons.undo_rounded),
                            label: const Text('Renvoyer pour correction'),
                          ),
                        ],
                        if (_acte!.isScelle && session?.isParoisse == true)
                          ElevatedButton.icon(
                            onPressed: _emettreCarte,
                            icon: const Icon(Icons.badge_outlined),
                            label: Text(_cartes.isEmpty ? strings.issueCard : 'Émettre Duplicata / Extrait A4'),
                          ),
                        if (_acte!.isScelle && session?.isCure == true)
                          OutlinedButton.icon(
                            onPressed: _demanderAvenant,
                            icon: const Icon(Icons.edit_document),
                            label: const Text('Demander un avenant'),
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
                              trailing: Wrap(
                                spacing: 4,
                                children: [
                                  IconButton(
                                    tooltip: strings.printPdf,
                                    icon: const Icon(Icons.print, color: AppTheme.navy),
                                    onPressed: () => _imprimerPdf(c),
                                  ),
                                  if (c.isActive && session?.canRevokeCard == true)
                                    IconButton(
                                      tooltip: 'Révoquer cette carte',
                                      icon: const Icon(Icons.cancel_outlined, color: AppTheme.danger),
                                      onPressed: () => _revoquerCarte(c),
                                    ),
                                ],
                              ),
                            ),
                          )),
                    ],
                    if (_avenants.isNotEmpty) ...[
                      const SizedBox(height: 24),
                      const Text('Historique des Avenants de rectification',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.navy)),
                      const SizedBox(height: 8),
                      ..._avenants.map((a) => Card(
                            child: ListTile(
                              title: Text('v${a.versionCible} · ${a.gravite} · ${a.statut}'),
                              subtitle: Text('Motif : ${a.motif}\nModifications : ${a.nouvellesValeurs}'),
                              isThreeLine: true,
                              trailing: a.isEnAttente && session?.isChancelier == true
                                  ? Wrap(
                                      spacing: 6,
                                      children: [
                                        IconButton(
                                          tooltip: 'Approuver (PIN)',
                                          icon: const Icon(Icons.check_circle, color: AppTheme.success),
                                          onPressed: () => _approuverAvenant(a),
                                        ),
                                        IconButton(
                                          tooltip: 'Rejeter',
                                          icon: const Icon(Icons.cancel, color: AppTheme.danger),
                                          onPressed: () => _rejeterAvenant(a),
                                        ),
                                      ],
                                    )
                                  : null,
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
