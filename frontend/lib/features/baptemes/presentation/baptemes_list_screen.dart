import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../data/bapteme_repository.dart';
import '../domain/acte_bapteme.dart';

class BaptemesListScreen extends ConsumerStatefulWidget {
  const BaptemesListScreen({super.key});

  @override
  ConsumerState<BaptemesListScreen> createState() => _BaptemesListScreenState();
}

class _BaptemesListScreenState extends ConsumerState<BaptemesListScreen> {
  String? _statutFilter;
  late Future<List<ActeBapteme>> _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    _future = ref.read(baptemeRepositoryProvider).lister(statut: _statutFilter);
  }

  Color _badgeColor(String statut) => switch (statut) {
        'SCELLE' => AppTheme.success,
        'EN_ATTENTE_VALIDATION' => AppTheme.warning,
        'A_CORRIGER' => AppTheme.danger,
        _ => const Color(0xFF64748B),
      };

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);

    return AdaptiveScaffold(
      currentRoute: '/baptemes',
      title: strings.navActes,
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppTheme.navy,
        foregroundColor: Colors.white,
        onPressed: () => context.go('/baptemes/nouveau'),
        icon: const Icon(Icons.add),
        label: Text(strings.newAct),
      ),
      body: Column(
        children: [
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            child: Row(
              children: [
                for (final entry in <String?, String>{
                  null: 'Tous',
                  'EN_ATTENTE_VALIDATION': 'À valider',
                  'SCELLE': 'Scellés',
                  'BROUILLON': 'Brouillons',
                  'A_CORRIGER': 'À corriger',
                }.entries)
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(entry.value),
                      selected: _statutFilter == entry.key,
                      onSelected: (_) => setState(() {
                        _statutFilter = entry.key;
                        _load();
                      }),
                    ),
                  ),
              ],
            ),
          ),
          Expanded(
            child: FutureBuilder<List<ActeBapteme>>(
              future: _future,
              builder: (context, snap) {
                if (snap.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snap.hasError) {
                  return Center(child: Text(extractApiError(snap.error!)));
                }
                final actes = snap.data ?? [];
                if (actes.isEmpty) {
                  return const Center(child: Text('Aucun acte de baptême dans ce filtre.'));
                }
                return ListView.separated(
                  padding: const EdgeInsets.all(16),
                  itemCount: actes.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 8),
                  itemBuilder: (context, i) {
                    final a = actes[i];
                    return Card(
                      child: ListTile(
                        title: Text('${a.nom} ${a.prenoms}', style: const TextStyle(fontWeight: FontWeight.bold)),
                        subtitle: Text('${a.iun ?? "Non scellé"} · Baptême : ${a.dateBapteme} · Reg: T${a.registreTome}/P${a.registrePage}/N°${a.registreNumero}'),
                        trailing: Chip(
                          backgroundColor: _badgeColor(a.statut).withValues(alpha: 0.15),
                          label: Text(a.statut, style: TextStyle(color: _badgeColor(a.statut), fontSize: 11, fontWeight: FontWeight.bold)),
                        ),
                        onTap: () => context.go('/baptemes/${a.id}'),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
