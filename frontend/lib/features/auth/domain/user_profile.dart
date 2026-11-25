class UserProfile {
  final String id;
  final String identifiant;
  final String nomComplet;
  final String? telephone;
  final String languePreferee;
  final String role;
  final String niveau;
  final String? provinceNom;
  final String? dioceseNom;
  final String? paroisseNom;
  final String statut;
  final bool totpActive;
  final bool mdpAChanger;
  final bool pinConfigure;

  const UserProfile({
    required this.id,
    required this.identifiant,
    required this.nomComplet,
    this.telephone,
    required this.languePreferee,
    required this.role,
    required this.niveau,
    this.provinceNom,
    this.dioceseNom,
    this.paroisseNom,
    required this.statut,
    required this.totpActive,
    required this.mdpAChanger,
    required this.pinConfigure,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] as String,
      identifiant: json['identifiant'] as String,
      nomComplet: json['nomComplet'] as String,
      telephone: json['telephone'] as String?,
      languePreferee: (json['languePreferee'] as String?) ?? 'FR',
      role: json['role'] as String,
      niveau: json['niveau'] as String,
      provinceNom: json['provinceNom'] as String?,
      dioceseNom: json['dioceseNom'] as String?,
      paroisseNom: json['paroisseNom'] as String?,
      statut: (json['statut'] as String?) ?? 'ACTIF',
      totpActive: json['totpActive'] as bool? ?? false,
      mdpAChanger: json['mdpAChanger'] as bool? ?? false,
      pinConfigure: json['pinConfigure'] as bool? ?? false,
    );
  }

  String get affectationLabel {
    final parts = <String>[
      if (paroisseNom != null && paroisseNom!.isNotEmpty) paroisseNom!,
      if (dioceseNom != null && dioceseNom!.isNotEmpty) dioceseNom!,
      if (provinceNom != null && provinceNom!.isNotEmpty) provinceNom!,
    ];
    if (parts.isEmpty) return 'Conférence Épiscopale Nationale (CENC)';
    return parts.join(' · ');
  }
}

class TotpSetupData {
  final String secret;
  final String otpAuthUri;

  const TotpSetupData({required this.secret, required this.otpAuthUri});

  factory TotpSetupData.fromJson(Map<String, dynamic> json) {
    return TotpSetupData(
      secret: json['secret'] as String,
      otpAuthUri: json['otpAuthUri'] as String,
    );
  }
}
