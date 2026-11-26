import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/l10n/app_strings.dart';
import '../../../app/theme/app_theme.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/widgets/adaptive_scaffold.dart';
import '../../auth/presentation/auth_controller.dart';
import '../data/referentiel_repository.dart';
import '../domain/referentiel_models.dart';

class ReferentielScreen extends ConsumerStatefulWidget {
  const ReferentielScreen({super.key});

  @override
  ConsumerState<ReferentielScreen> createState() => _ReferentielScreenState();
}

class _ReferentielScreenState extends ConsumerState<ReferentielScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabCtrl;
  List<ProvinceItem> _provinces = [];
  List<DioceseItem> _dioceses = [];
  List<ParoisseItem> _paroisses = [];
  List<UtilisateurItem> _utilisateurs = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final repo = ref.read(referentielRepositoryProvider);
      final results = await Future.wait([
        repo.listerProvinces(),
        repo.listerDioceses(),
        repo.listerParoisses(),
        repo.listerUtilisateurs(),
      ]);
      setState(() {
        _provinces = results[0] as List<ProvinceItem>;
        _dioceses = results[1] as List<DioceseItem>;
        _paroisses = results[2] as List<ParoisseItem>;
        _utilisateurs = results[3] as List<UtilisateurItem>;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = extractApiError(e);
        _loading = false;
      });
    }
  }

  Future<void> _dialogNouveauDiocese() async {
    if (_provinces.isEmpty) return;
    String provId = _provinces.first.id;
    String type = 'DIOCESE';
    final code = TextEditingController();
    final nomFr = TextEditingController();
    final nomEn = TextEditingController();
    final siege = TextEditingController();

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) => AlertDialog(
          title: const Text('Nouveau Diocèse / Archidiocèse'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: provId,
                  decoration: const InputDecoration(labelText: 'Province ecclésiastique'),
                  items: _provinces.map((p) => DropdownMenuItem(value: p.id, child: Text('${p.nomFr} (${p.code})'))).toList(),
                  onChanged: (v) => setS(() => provId = v ?? provId),
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: type,
                  decoration: const InputDecoration(labelText: 'Type'),
                  items: const [
                    DropdownMenuItem(value: 'DIOCESE', child: Text('Diocèse')),
                    DropdownMenuItem(value: 'ARCHIDIOCESE', child: Text('Archidiocèse')),
                  ],
                  onChanged: (v) => setS(() => type = v ?? type),
                ),
                const SizedBox(height: 10),
                TextField(controller: code, decoration: const InputDecoration(labelText: 'Code IUN (ex. YDE, BAF)')),
                const SizedBox(height: 10),
                TextField(controller: nomFr, decoration: const InputDecoration(labelText: 'Nom français')),
                const SizedBox(height: 10),
                TextField(controller: nomEn, decoration: const InputDecoration(labelText: 'Nom anglais')),
                const SizedBox(height: 10),
                TextField(controller: siege, decoration: const InputDecoration(labelText: 'Ville siège')),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Créer')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(referentielRepositoryProvider).creerDiocese({
        'provinceId': provId,
        'code': code.text.trim().toUpperCase(),
        'typeDiocese': type,
        'nomFr': nomFr.text.trim(),
        'nomEn': nomEn.text.trim().isEmpty ? nomFr.text.trim() : nomEn.text.trim(),
        'siege': siege.text.trim(),
      });
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _dialogNouvelleParoisse() async {
    final session = ref.read(authControllerProvider).session;
    final diocesesAutorises = session?.isChancelier == true && session?.dioceseId != null
        ? _dioceses.where((d) => d.id == session!.dioceseId).toList()
        : _dioceses;
    if (diocesesAutorises.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Créez d\'abord un diocèse.')));
      return;
    }
    String dioId = diocesesAutorises.first.id;
    String type = 'PAROISSE';
    final code = TextEditingController();
    final nomFr = TextEditingController();
    final nomEn = TextEditingController();
    final localite = TextEditingController();

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) => AlertDialog(
          title: const Text('Nouvelle Paroisse / Cathédrale'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: dioId,
                  decoration: const InputDecoration(labelText: 'Diocèse'),
                  items: diocesesAutorises.map((d) => DropdownMenuItem(value: d.id, child: Text('${d.nomFr} (${d.code})'))).toList(),
                  onChanged: diocesesAutorises.length > 1 ? (v) => setS(() => dioId = v ?? dioId) : null,
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: type,
                  decoration: const InputDecoration(labelText: 'Type'),
                  items: const [
                    DropdownMenuItem(value: 'PAROISSE', child: Text('Paroisse')),
                    DropdownMenuItem(value: 'CATHEDRALE', child: Text('Cathédrale')),
                    DropdownMenuItem(value: 'QUASI_PAROISSE', child: Text('Quasi-paroisse')),
                    DropdownMenuItem(value: 'MISSION', child: Text('Mission')),
                  ],
                  onChanged: (v) => setS(() => type = v ?? type),
                ),
                const SizedBox(height: 10),
                TextField(controller: code, decoration: const InputDecoration(labelText: 'Code (ex. P001)')),
                const SizedBox(height: 10),
                TextField(controller: nomFr, decoration: const InputDecoration(labelText: 'Nom français')),
                const SizedBox(height: 10),
                TextField(controller: nomEn, decoration: const InputDecoration(labelText: 'Nom anglais')),
                const SizedBox(height: 10),
                TextField(controller: localite, decoration: const InputDecoration(labelText: 'Localité / Quartier')),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Créer')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(referentielRepositoryProvider).creerParoisse({
        'dioceseId': dioId,
        'code': code.text.trim().toUpperCase(),
        'typeParoisse': type,
        'nomFr': nomFr.text.trim(),
        'nomEn': nomEn.text.trim().isEmpty ? nomFr.text.trim() : nomEn.text.trim(),
        'localite': localite.text.trim(),
      });
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  List<DropdownMenuItem<String>> _rolesAutorises(String? callerRole) {
    return switch (callerRole) {
      'ADMIN_NATIONAL' => const [
          DropdownMenuItem(value: 'SECRETAIRE', child: Text('Secrétaire (tous niveaux)')),
          DropdownMenuItem(value: 'CURE', child: Text('Curé / Recteur')),
          DropdownMenuItem(value: 'CHANCELIER', child: Text('Chancelier diocésain')),
          DropdownMenuItem(value: 'RESPONSABLE_PROVINCE', child: Text('Responsable Province')),
          DropdownMenuItem(value: 'ADMIN_NATIONAL', child: Text('Admin National CENC')),
        ],
      'RESPONSABLE_PROVINCE' => const [
          DropdownMenuItem(value: 'SECRETAIRE', child: Text('Secrétaire (Province / Diocèse / Paroisse)')),
          DropdownMenuItem(value: 'CURE', child: Text('Curé / Recteur')),
          DropdownMenuItem(value: 'CHANCELIER', child: Text('Chancelier diocésain')),
        ],
      'CHANCELIER' => const [
          DropdownMenuItem(value: 'SECRETAIRE', child: Text('Secrétaire (Diocèse / Paroisse)')),
          DropdownMenuItem(value: 'CURE', child: Text('Curé / Recteur')),
        ],
      _ => const [
          DropdownMenuItem(value: 'SECRETAIRE', child: Text('Secrétaire paroissial')),
        ],
    };
  }

  List<DropdownMenuItem<String>> _niveauxAutorises(String? callerRole) {
    return switch (callerRole) {
      'ADMIN_NATIONAL' => const [
          DropdownMenuItem(value: 'NATIONAL', child: Text('National (CENC)')),
          DropdownMenuItem(value: 'PROVINCE', child: Text('Province Ecclésiastique')),
          DropdownMenuItem(value: 'DIOCESE', child: Text('Diocèse / Chancellerie')),
          DropdownMenuItem(value: 'PAROISSE', child: Text('Paroisse / Cathédrale')),
        ],
      'RESPONSABLE_PROVINCE' => const [
          DropdownMenuItem(value: 'PROVINCE', child: Text('Province Ecclésiastique')),
          DropdownMenuItem(value: 'DIOCESE', child: Text('Diocèse / Chancellerie')),
          DropdownMenuItem(value: 'PAROISSE', child: Text('Paroisse / Cathédrale')),
        ],
      'CHANCELIER' => const [
          DropdownMenuItem(value: 'DIOCESE', child: Text('Diocèse / Chancellerie')),
          DropdownMenuItem(value: 'PAROISSE', child: Text('Paroisse / Cathédrale')),
        ],
      _ => const [
          DropdownMenuItem(value: 'PAROISSE', child: Text('Paroisse / Cathédrale')),
        ],
    };
  }

  Future<void> _dialogNouvelUtilisateur() async {
    final session = ref.read(authControllerProvider).session;
    final roleItems = _rolesAutorises(session?.role);
    final niveauItems = _niveauxAutorises(session?.role);
    String role = 'SECRETAIRE';
    String niveau = 'PAROISSE';
    String? provId = session?.provinceId ?? _provinces.firstOrNull?.id;
    String? dioId = session?.dioceseId ?? _dioceses.firstOrNull?.id;
    String? parId = session?.paroisseId ?? _paroisses.firstOrNull?.id;
    final identifiant = TextEditingController();
    final mdp = TextEditingController();
    final nom = TextEditingController();

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) => AlertDialog(
          title: const Text('Nouveau Compte Ecclésiastique'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(controller: identifiant, decoration: const InputDecoration(labelText: 'Identifiant (email / tél.)')),
                const SizedBox(height: 10),
                TextField(controller: nom, decoration: const InputDecoration(labelText: 'Nom complet')),
                const SizedBox(height: 10),
                TextField(controller: mdp, obscureText: true, decoration: const InputDecoration(labelText: 'Mot de passe initial (10+ car.)')),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: role,
                  decoration: const InputDecoration(labelText: 'Rôle (Autorité)'),
                  items: roleItems,
                  onChanged: roleItems.length > 1
                      ? (v) => setS(() {
                            role = v ?? role;
                            if (role == 'ADMIN_NATIONAL') niveau = 'NATIONAL';
                            if (role == 'RESPONSABLE_PROVINCE') niveau = 'PROVINCE';
                            if (role == 'CHANCELIER') niveau = 'DIOCESE';
                            if (role == 'CURE') niveau = 'PAROISSE';
                          })
                      : null,
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: niveau,
                  decoration: const InputDecoration(labelText: 'Niveau ecclésial'),
                  items: niveauItems,
                  onChanged: (role == 'SECRETAIRE' && niveauItems.length > 1) ? (v) => setS(() => niveau = v ?? niveau) : null,
                ),
                if (niveau == 'PROVINCE' && _provinces.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: provId,
                    decoration: const InputDecoration(labelText: 'Province'),
                    items: _provinces.map((p) => DropdownMenuItem(value: p.id, child: Text(p.nomFr))).toList(),
                    onChanged: session?.isAdminNational == true ? (v) => setS(() => provId = v) : null,
                  ),
                ],
                if (niveau == 'DIOCESE' && _dioceses.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: dioId,
                    decoration: const InputDecoration(labelText: 'Diocèse'),
                    items: _dioceses.map((d) => DropdownMenuItem(value: d.id, child: Text(d.nomFr))).toList(),
                    onChanged: (session?.isAdminNational == true || session?.isResponsableProvince == true)
                        ? (v) => setS(() => dioId = v)
                        : null,
                  ),
                ],
                if (niveau == 'PAROISSE' && _paroisses.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: parId,
                    decoration: const InputDecoration(labelText: 'Paroisse / Cathédrale'),
                    items: _paroisses.map((p) => DropdownMenuItem(value: p.id, child: Text('${p.nomFr} (${p.code})'))).toList(),
                    onChanged: session?.isCure == true ? null : (v) => setS(() => parId = v),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Créer')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(referentielRepositoryProvider).creerUtilisateur({
        'identifiant': identifiant.text.trim(),
        'motDePasseInitial': mdp.text,
        'nomComplet': nom.text.trim(),
        'role': role,
        'niveau': niveau,
        if (niveau == 'PROVINCE') 'provinceId': provId,
        if (niveau == 'DIOCESE') 'dioceseId': dioId,
        if (niveau == 'PAROISSE') 'paroisseId': parId,
        'languePreferee': 'FR',
      });
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _dialogMuterUtilisateur(UtilisateurItem u) async {
    final session = ref.read(authControllerProvider).session;
    final roleItems = _rolesAutorises(session?.role);
    final niveauItems = _niveauxAutorises(session?.role);
    String role = roleItems.any((i) => i.value == u.role) ? u.role : roleItems.first.value!;
    String niveau = niveauItems.any((i) => i.value == u.niveau) ? u.niveau : niveauItems.first.value!;
    String? provId = session?.provinceId ?? _provinces.firstOrNull?.id;
    String? dioId = session?.dioceseId ?? _dioceses.firstOrNull?.id;
    String? parId = session?.paroisseId ?? _paroisses.firstOrNull?.id;

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setS) => AlertDialog(
          title: Text('Muter ${u.nomComplet}'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: role,
                  decoration: const InputDecoration(labelText: 'Nouveau rôle'),
                  items: roleItems,
                  onChanged: (v) => setS(() {
                    role = v ?? role;
                    if (role == 'ADMIN_NATIONAL') niveau = 'NATIONAL';
                    if (role == 'RESPONSABLE_PROVINCE') niveau = 'PROVINCE';
                    if (role == 'CHANCELIER') niveau = 'DIOCESE';
                    if (role == 'CURE') niveau = 'PAROISSE';
                  }),
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: niveau,
                  decoration: const InputDecoration(labelText: 'Nouveau niveau'),
                  items: niveauItems,
                  onChanged: role == 'SECRETAIRE' ? (v) => setS(() => niveau = v ?? niveau) : null,
                ),
                if (niveau == 'PROVINCE' && _provinces.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: provId,
                    decoration: const InputDecoration(labelText: 'Nouvelle province'),
                    items: _provinces.map((p) => DropdownMenuItem(value: p.id, child: Text(p.nomFr))).toList(),
                    onChanged: session?.isAdminNational == true ? (v) => setS(() => provId = v) : null,
                  ),
                ],
                if (niveau == 'DIOCESE' && _dioceses.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: dioId,
                    decoration: const InputDecoration(labelText: 'Nouveau diocèse'),
                    items: _dioceses.map((d) => DropdownMenuItem(value: d.id, child: Text(d.nomFr))).toList(),
                    onChanged: (session?.isAdminNational == true || session?.isResponsableProvince == true)
                        ? (v) => setS(() => dioId = v)
                        : null,
                  ),
                ],
                if (niveau == 'PAROISSE' && _paroisses.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: parId,
                    decoration: const InputDecoration(labelText: 'Nouvelle paroisse'),
                    items: _paroisses.map((p) => DropdownMenuItem(value: p.id, child: Text('${p.nomFr} (${p.code})'))).toList(),
                    onChanged: (v) => setS(() => parId = v),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Confirmer la mutation')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(referentielRepositoryProvider).muterUtilisateur(u.id, {
        'nouveauRole': role,
        'nouveauNiveau': niveau,
        if (niveau == 'PROVINCE') 'nouvelleProvinceId': provId,
        if (niveau == 'DIOCESE') 'nouveauDioceseId': dioId,
        if (niveau == 'PAROISSE') 'nouvelleParoisseId': parId,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Mutation effectuée pour ${u.nomComplet}.')));
      }
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
    }
  }

  Future<void> _toggleStatutUtilisateur(UtilisateurItem u) async {
    final actif = u.statut == 'ACTIF';
    final nouveauStatut = actif ? 'SUSPENDU' : 'ACTIF';
    try {
      await ref.read(referentielRepositoryProvider).changerStatutUtilisateur(u.id, nouveauStatut);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              actif ? 'Compte ${u.identifiant} suspendu.' : 'Compte ${u.identifiant} réactivé.',
            ),
          ),
        );
      }
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(extractApiError(e))));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = ref.watch(stringsProvider);
    final session = ref.watch(authControllerProvider).session;

    return AdaptiveScaffold(
      currentRoute: '/referentiel',
      title: strings.navReferentiel,
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : Column(
                  children: [
                    TabBar(
                      controller: _tabCtrl,
                      labelColor: AppTheme.navy,
                      indicatorColor: AppTheme.gold,
                      tabs: [
                        Tab(text: 'Diocèses (${_dioceses.length})'),
                        Tab(text: 'Paroisses (${_paroisses.length})'),
                        Tab(text: 'Comptes (${_utilisateurs.length})'),
                      ],
                    ),
                    Expanded(
                      child: TabBarView(
                        controller: _tabCtrl,
                        children: [
                          ListView(
                            padding: const EdgeInsets.all(16),
                            children: [
                              if (session?.isAdminNational == true)
                                Align(
                                  alignment: Alignment.centerRight,
                                  child: ElevatedButton.icon(
                                    onPressed: _dialogNouveauDiocese,
                                    icon: const Icon(Icons.add),
                                    label: const Text('Nouveau Diocèse'),
                                  ),
                                ),
                              const SizedBox(height: 10),
                              ..._dioceses.map((d) => Card(
                                    child: ListTile(
                                      leading: CircleAvatar(backgroundColor: AppTheme.navy, foregroundColor: Colors.white, child: Text(d.code)),
                                      title: Text(d.nomFr),
                                      subtitle: Text('${d.typeDiocese} · Province ${d.provinceCode} · Siège : ${d.siege}'),
                                    ),
                                  )),
                            ],
                          ),
                          ListView(
                            padding: const EdgeInsets.all(16),
                            children: [
                              if (session?.isAdminNational == true || session?.isChancelier == true)
                                Align(
                                  alignment: Alignment.centerRight,
                                  child: ElevatedButton.icon(
                                    onPressed: _dialogNouvelleParoisse,
                                    icon: const Icon(Icons.add),
                                    label: const Text('Nouvelle Paroisse / Cathédrale'),
                                  ),
                                ),
                              const SizedBox(height: 10),
                              ..._paroisses.map((p) => Card(
                                    child: ListTile(
                                      leading: CircleAvatar(backgroundColor: AppTheme.gold, foregroundColor: AppTheme.navy, child: Text(p.code)),
                                      title: Text(p.nomFr),
                                      subtitle: Text('${p.typeParoisse} · Diocèse ${p.dioceseCode} · ${p.localite}'),
                                    ),
                                  )),
                            ],
                          ),
                          ListView(
                            padding: const EdgeInsets.all(16),
                            children: [
                              Align(
                                alignment: Alignment.centerRight,
                                child: ElevatedButton.icon(
                                  onPressed: _dialogNouvelUtilisateur,
                                  icon: const Icon(Icons.person_add),
                                  label: const Text('Nouveau Compte'),
                                ),
                              ),
                              const SizedBox(height: 10),
                              ..._utilisateurs.map((u) {
                                final actif = u.statut == 'ACTIF';
                                return Card(
                                  child: ListTile(
                                    title: Text('${u.nomComplet} (${u.identifiant})'),
                                    subtitle: Text('${u.role} · Niveau ${u.niveau} · 2FA : ${u.totpActive ? "Actif" : "Inactif"}'),
                                    trailing: Wrap(
                                      spacing: 8,
                                      crossAxisAlignment: WrapCrossAlignment.center,
                                      children: [
                                        Chip(
                                          label: Text(u.statut, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
                                          backgroundColor: (actif ? AppTheme.success : AppTheme.danger).withValues(alpha: 0.15),
                                        ),
                                        if (session?.canMutateUsers == true)
                                          IconButton(
                                            tooltip: 'Muter ce compte',
                                            icon: const Icon(Icons.swap_horiz_rounded, color: AppTheme.navy),
                                            onPressed: () => _dialogMuterUtilisateur(u),
                                          ),
                                        if (session?.isAdminNational == true ||
                                            session?.isResponsableProvince == true ||
                                            session?.isChancelier == true ||
                                            (session?.isCure == true && u.role == 'SECRETAIRE'))
                                          IconButton(
                                            tooltip: actif ? 'Suspendre ce compte' : 'Réactiver ce compte',
                                            icon: Icon(
                                              actif ? Icons.block_rounded : Icons.check_circle_outline_rounded,
                                              color: actif ? AppTheme.danger : AppTheme.success,
                                            ),
                                            onPressed: () => _toggleStatutUtilisateur(u),
                                          ),
                                      ],
                                    ),
                                  ),
                                );
                              }),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
    );
  }
}
