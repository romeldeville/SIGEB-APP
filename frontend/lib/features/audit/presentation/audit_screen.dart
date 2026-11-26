import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../auth/presentation/auth_controller.dart';
import '../data/audit_repository.dart';
import '../domain/audit_entry.dart';

class AuditScreen extends ConsumerStatefulWidget {
  const AuditScreen({super.key});

  @override
  ConsumerState<AuditScreen> createState() => _AuditScreenState();
}

class _AuditScreenState extends ConsumerState<AuditScreen> {
  List<AuditEntry> _logs = [];
  bool _loading = true;
  String? _error;
  String? _integriteMsg;
  bool _integriteOk = true;
  final _iunFilterCtrl = TextEditingController();
  String _actionFilter = '';

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
      final list = await ref.read(auditRepositoryProvider).consulter(
            iun: _iunFilterCtrl.text,
            typeAction: _actionFilter.isEmpty ? null : _actionFilter,
          );
      setState(() {
        _logs = list;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _loading = false;
      });
    }
  }

  Future<void> _verifierIntegrite() async {
    try {
      final res = await ref.read(auditRepositoryProvider).verifierIntegrite();
      setState(() {
        _integriteOk = res.chaineValide;
        _integriteMsg = res.chaineValide
            ? 'Chaîne SHA-256 intacte (${res.nombreEntreesVerifiees} entrées vérifiées)'
            : 'ALERTE : Rupture détectée à l\'ID ${res.premiereRuptureId}';
      });
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    final isAdmin = ref.watch(authControllerProvider).session?.isAdminNational == true;

    return AdaptiveScaffold(
      currentRoute: '/audit',
      title: strings.navAudit,
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
            child: Wrap(
              spacing: 10,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                SizedBox(
                  width: 240,
                  child: TextField(
                    controller: _iunFilterCtrl,
                    decoration: const InputDecoration(labelText: 'Filtrer par IUN', isDense: true),
                  ),
                ),
                SizedBox(
                  width: 220,
                  child: DropdownButtonFormField<String>(
                    initialValue: _actionFilter,
                    decoration: const InputDecoration(labelText: 'Type d\'action', isDense: true),
                    items: const [
                      DropdownMenuItem(value: '', child: Text('Toutes les actions')),
                      DropdownMenuItem(value: 'CONNEXION', child: Text('CONNEXION')),
                      DropdownMenuItem(value: 'CREATION_ACTE', child: Text('CREATION_ACTE')),
                      DropdownMenuItem(value: 'SCELLEMENT', child: Text('SCELLEMENT')),
                      DropdownMenuItem(value: 'RECTIFICATION', child: Text('RECTIFICATION')),
                      DropdownMenuItem(value: 'EMISSION_CARTE', child: Text('EMISSION_CARTE')),
                      DropdownMenuItem(value: 'REVOCATION_CARTE', child: Text('REVOCATION_CARTE')),
                      DropdownMenuItem(value: 'DECLARATION_MENTION', child: Text('DECLARATION_MENTION')),
                      DropdownMenuItem(value: 'VALIDATION_MENTION', child: Text('VALIDATION_MENTION')),
                      DropdownMenuItem(value: 'RECHERCHE_NATIONALE', child: Text('RECHERCHE_NATIONALE')),
                    ],
                    onChanged: (v) => setState(() => _actionFilter = v ?? ''),
                  ),
                ),
                ElevatedButton.icon(onPressed: _load, icon: const Icon(Icons.filter_list), label: const Text('Filtrer')),
              ],
            ),
          ),
          if (isAdmin)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Wrap(
                spacing: 12,
                runSpacing: 8,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  ElevatedButton.icon(
                    onPressed: _verifierIntegrite,
                    icon: const Icon(Icons.verified_user),
                    label: Text(strings.verifyIntegrity),
                  ),
                  if (_integriteMsg != null)
                    Text(
                      _integriteMsg!,
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: _integriteOk ? AppTheme.success : AppTheme.danger,
                      ),
                    ),
                ],
              ),
            ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(child: Text(_error!))
                    : ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: _logs.length,
                        itemBuilder: (_, i) {
                          final l = _logs[i];
                          return Card(
                            child: ListTile(
                              dense: true,
                              title: Text('${l.typeAction} · ${l.roleUtilisateur}'),
                              subtitle: Text('${l.iunConcerne ?? "-"} · ${l.motif ?? ""} · ${l.horodatage}'),
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
