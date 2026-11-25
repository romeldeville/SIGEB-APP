import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../data/verification_repository.dart';

class RechercheNationaleScreen extends ConsumerStatefulWidget {
  const RechercheNationaleScreen({super.key});

  @override
  ConsumerState<RechercheNationaleScreen> createState() => _RechercheNationaleScreenState();
}

class _RechercheNationaleScreenState extends ConsumerState<RechercheNationaleScreen> {
  final _iunCtrl = TextEditingController();
  final _motifCtrl = TextEditingController();
  List<Map<String, dynamic>> _results = [];
  bool _loading = false;
  String? _error;

  Future<void> _search() async {
    if (_motifCtrl.text.trim().isEmpty) {
      setState(() => _error = 'Le motif canonique est obligatoire pour la traçabilité.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await ref.read(verificationRepositoryProvider).rechercheNationale(
            iun: _iunCtrl.text,
            motif: _motifCtrl.text,
          );
      setState(() {
        _results = res;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    return AdaptiveScaffold(
      currentRoute: '/verification/qr',
      title: strings.nationalSearch,
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          TextField(controller: _iunCtrl, decoration: const InputDecoration(labelText: 'IUN exact (ex. CM-YDE-P001-2026-00001)')),
          const SizedBox(height: 12),
          TextField(controller: _motifCtrl, decoration: const InputDecoration(labelText: 'Motif canonique obligatoire (ex. Préparation mariage)')),
          const SizedBox(height: 14),
          ElevatedButton.icon(onPressed: _loading ? null : _search, icon: const Icon(Icons.search), label: const Text('Rechercher')),
          if (_error != null) Padding(padding: const EdgeInsets.only(top: 12), child: Text(_error!, style: const TextStyle(color: AppTheme.danger))),
          const SizedBox(height: 16),
          ..._results.map((r) => Card(
                child: ListTile(
                  title: Text('${r['nom']} ${r['prenoms']} (${r['iun']})'),
                  subtitle: Text('${r['dioceseNomFr']} · ${r['paroisseNomFr']} · Baptême : ${r['dateBapteme']}'),
                  trailing: Chip(
                    label: Text((r['etatLibreMatrimonial'] == true) ? 'LIBRE' : 'LIÉ'),
                    backgroundColor: (r['etatLibreMatrimonial'] == true ? AppTheme.success : AppTheme.danger).withValues(alpha: 0.15),
                  ),
                ),
              )),
        ],
      ),
    );
  }
}
