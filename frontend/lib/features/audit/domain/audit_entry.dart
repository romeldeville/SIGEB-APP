class AuditEntry {
  final int id;
  final String horodatage;
  final String roleUtilisateur;
  final String typeAction;
  final String? iunConcerne;
  final String? motif;

  const AuditEntry({
    required this.id,
    required this.horodatage,
    required this.roleUtilisateur,
    required this.typeAction,
    this.iunConcerne,
    this.motif,
  });

  factory AuditEntry.fromJson(Map<String, dynamic> j) => AuditEntry(
        id: (j['id'] as num).toInt(),
        horodatage: j['horodatage']?.toString() ?? '',
        roleUtilisateur: j['roleUtilisateur'] as String? ?? '',
        typeAction: j['typeAction'] as String? ?? '',
        iunConcerne: j['iunConcerne'] as String?,
        motif: j['motif'] as String?,
      );
}

class AuditIntegrity {
  final bool chaineValide;
  final int nombreEntreesVerifiees;
  final int? premiereRuptureId;

  const AuditIntegrity({
    required this.chaineValide,
    required this.nombreEntreesVerifiees,
    this.premiereRuptureId,
  });

  factory AuditIntegrity.fromJson(Map<String, dynamic> j) => AuditIntegrity(
        chaineValide: j['chaineValide'] as bool? ?? false,
        nombreEntreesVerifiees: (j['nombreEntreesVerifiees'] as num?)?.toInt() ?? 0,
        premiereRuptureId: (j['premiereRuptureId'] as num?)?.toInt(),
      );
}
