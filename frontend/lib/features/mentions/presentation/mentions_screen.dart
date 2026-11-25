import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';

class MentionsScreen extends ConsumerStatefulWidget {
  const MentionsScreen({super.key});

  @override
  ConsumerState<MentionsScreen> createState() => _MentionsScreenState();
}

class _MentionsScreenState extends ConsumerState<MentionsScreen> {
  List<dynamic> _boite = [];
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
      final resp = await ref.read(dioProvider).get('/mentions/boite-reception');
      setState(() {
        _boite = resp.data as List;
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
      currentRoute: '/mentions',
      title: strings.navMentions,
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
                        final m = _boite[i] as Map<String, dynamic>;
                        return Card(
                          child: ListTile(
                            title: Text('${m['typeSacrement']} · ${m['iun']}'),
                            subtitle: Text('Déclarée par ${m['paroisseDeclaranteNomFr']} · Date : ${m['dateEvenement']}'),
                            trailing: Chip(label: Text('${m['statut']}')),
                          ),
                        );
                      },
                    ),
    );
  }
}
